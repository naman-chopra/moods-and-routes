package com.ndev.moodyroutine.engine

import android.content.Context
import com.ndev.moodyroutine.data.model.TriggerConfig
import com.ndev.moodyroutine.data.model.TriggerMatchType
import com.ndev.moodyroutine.data.model.TriggerType
import com.ndev.moodyroutine.data.repository.ModeRepository
import com.ndev.moodyroutine.data.repository.RoutineRepository
import com.ndev.moodyroutine.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AutomationEngine(
    private val context: Context,
    private val routineRepository: RoutineRepository,
    private val modeRepository: ModeRepository,
    private val conditionEvaluator: ConditionEvaluator,
    private val actionExecutor: ActionExecutor
) {
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private val activeTriggers = mutableMapOf<Long, MutableSet<Int>>() // routineId -> satisfied trigger indices

    fun start() {
        if (job != null) return
        job = scope.launch {
            AppLogger.i("AutomationEngine", "AutomationEngine started")
            EventBus.events.collectLatest { event ->
                AppLogger.d("AutomationEngine", "Received event: $event")
                try {
                    processEventForRoutines(event)
                    processEventForModes(event)
                } catch (e: Exception) {
                    AppLogger.e("AutomationEngine", "Error processing event $event", e)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        AppLogger.i("AutomationEngine", "AutomationEngine stopped")
    }

    private suspend fun processEventForRoutines(event: AutomationEvent) {
        val routines = try {
            routineRepository.getEnabledRoutines().first()
        } catch (e: Exception) {
            emptyList()
        }

        for (routine in routines) {
            // 1. Check if the current event causes an exit condition / condition mismatch
            var exitMatched = false
            for (trigger in routine.triggers) {
                if (isExitConditionForTrigger(event, trigger, routine.name)) {
                    exitMatched = true
                    break
                }
            }

            if (exitMatched && routine.isActive) {
                AppLogger.i("AutomationEngine", "Exit condition matched for routine: ${routine.name}. Deactivating and reverting actions.")
                routineRepository.setRoutineActive(routine.id, false)
                if (routine.revertActionsOnExit) {
                    actionExecutor.revertRoutine(routine)
                }
                activeTriggers.remove(routine.id)
                continue
            }

            // 2. Evaluate triggers
            if (!exitMatched) {
                var shouldExecute = false
                val satisfiedIndices = activeTriggers.getOrPut(routine.id) { mutableSetOf() }

                for ((index, trigger) in routine.triggers.withIndex()) {
                    if (conditionEvaluator.evaluate(event, trigger)) {
                        satisfiedIndices.add(index)
                    }
                }

                if (routine.triggerMatchType == TriggerMatchType.ANY) {
                    if (satisfiedIndices.isNotEmpty()) {
                        shouldExecute = true
                    }
                } else if (routine.triggerMatchType == TriggerMatchType.ALL) {
                    if (routine.triggers.isNotEmpty() && satisfiedIndices.size >= routine.triggers.size) {
                        shouldExecute = true
                    }
                }

                if (shouldExecute) {
                    AppLogger.i("AutomationEngine", "Executing routine: ${routine.name}")
                    if (!routine.isActive) {
                        routineRepository.setRoutineActive(routine.id, true)
                        actionExecutor.executeRoutine(routine)
                    } else {
                        actionExecutor.executeAll(routine.actions)
                    }
                    routineRepository.updateLastTriggered(routine.id, System.currentTimeMillis())
                    satisfiedIndices.clear() // reset after execution
                }
            }
        }
    }

    private suspend fun processEventForModes(event: AutomationEvent) {
        val allModes = try {
            modeRepository.getAllModes().first()
        } catch (e: Exception) {
            emptyList()
        }

        for (mode in allModes) {
            if (!mode.isEnabled || mode.autoTriggers.isEmpty()) continue

            // 1. Check if the current event causes an exit condition / condition mismatch
            var exitMatched = false
            for (trigger in mode.autoTriggers) {
                if (isExitConditionForTrigger(event, trigger, mode.name)) {
                    exitMatched = true
                    break
                }
            }

            if (exitMatched && mode.isActive) {
                AppLogger.i("AutomationEngine", "Exit condition / condition mismatch matched for mode: ${mode.name}. Deactivating and reverting actions.")
                modeRepository.setModeActive(mode.id, false)
                if (mode.revertActionsOnExit) {
                    actionExecutor.revertMode(mode)
                }
                continue
            }

            // 2. Check if all triggers are satisfied (AND condition)
            if (!exitMatched) {
                val eventMatchesAny = mode.autoTriggers.any { conditionEvaluator.evaluate(event, it) }
                if (eventMatchesAny) {
                    val allSatisfied = mode.autoTriggers.all { trigger ->
                        conditionEvaluator.evaluate(event, trigger) || isTriggerCurrentlySatisfied(trigger)
                    }

                    if (allSatisfied) {
                        AppLogger.i("AutomationEngine", "Conditions matched for mode: ${mode.name}. Activating and executing actions.")
                        if (!mode.isActive) {
                            modeRepository.setModeActive(mode.id, true)
                            actionExecutor.executeMode(mode)
                        }
                    }
                }
            }
        }
    }

    private fun isExitConditionForTrigger(event: AutomationEvent, trigger: TriggerConfig, modeName: String): Boolean {
        return when (trigger.type) {
            TriggerType.LOCATION_ARRIVE -> {
                if (event is AutomationEvent.LocationEvent && !event.isEntering) {
                    isLocationMatch(event, trigger, modeName)
                } else false
            }
            TriggerType.LOCATION_LEAVE -> {
                if (event is AutomationEvent.LocationEvent && event.isEntering) {
                    isLocationMatch(event, trigger, modeName)
                } else false
            }
            TriggerType.WIFI_CONNECTED -> {
                event is AutomationEvent.WifiEvent && !event.isConnected
            }
            TriggerType.WIFI_DISCONNECTED -> {
                event is AutomationEvent.WifiEvent && event.isConnected
            }
            TriggerType.WIFI_SPECIFIC_NETWORK -> {
                if (event is AutomationEvent.WifiEvent) {
                    val targetSsid = trigger.params["ssid"]
                    !event.isConnected || (targetSsid != null && !event.ssid.equals(targetSsid, ignoreCase = true))
                } else false
            }
            TriggerType.BLUETOOTH_CONNECTED -> {
                event is AutomationEvent.BluetoothEvent && !event.isConnected
            }
            TriggerType.BLUETOOTH_DISCONNECTED -> {
                event is AutomationEvent.BluetoothEvent && event.isConnected
            }
            TriggerType.BLUETOOTH_SPECIFIC_DEVICE -> {
                if (event is AutomationEvent.BluetoothEvent && !event.isConnected) {
                    val devName = trigger.params["deviceName"]
                    val devAddr = trigger.params["deviceAddress"]
                    (devName != null && event.deviceName?.contains(devName, ignoreCase = true) == true) ||
                    (devAddr != null && event.deviceAddress.equals(devAddr, ignoreCase = true))
                } else false
            }
            TriggerType.POWER_CONNECTED -> {
                event is AutomationEvent.PowerEvent && !event.isConnected
            }
            TriggerType.POWER_DISCONNECTED -> {
                event is AutomationEvent.PowerEvent && event.isConnected
            }
            TriggerType.BATTERY_CHARGING -> {
                event is AutomationEvent.BatteryEvent && !event.isCharging
            }
            TriggerType.BATTERY_DISCHARGING -> {
                event is AutomationEvent.BatteryEvent && event.isCharging
            }
            TriggerType.HEADPHONE_CONNECTED -> {
                event is AutomationEvent.HeadphoneEvent && !event.isConnected
            }
            TriggerType.HEADPHONE_DISCONNECTED -> {
                event is AutomationEvent.HeadphoneEvent && event.isConnected
            }
            TriggerType.TIME_RANGE -> {
                if (event is AutomationEvent.TimeEvent) {
                    val startTime = trigger.params["startTime"]
                    val endTime = trigger.params["endTime"]
                    if (startTime != null && endTime != null) {
                        val currentMins = event.hour * 60 + event.minute
                        val startMins = parseMinutes(startTime)
                        val endMins = parseMinutes(endTime)
                        if (startMins != null && endMins != null) {
                            val inRange = if (startMins <= endMins) {
                                currentMins in startMins..endMins
                            } else {
                                currentMins >= startMins || currentMins <= endMins
                            }
                            !inRange
                        } else false
                    } else false
                } else false
            }
            TriggerType.DAY_OF_WEEK -> {
                if (event is AutomationEvent.TimeEvent) {
                    val daysStr = trigger.params["days"]
                    if (!daysStr.isNullOrBlank()) {
                        val days = daysStr.split(",").mapNotNull { it.trim().toIntOrNull() }
                        days.isNotEmpty() && !days.contains(event.dayOfWeek)
                    } else false
                } else false
            }
            TriggerType.SCREEN_ON -> {
                event is AutomationEvent.ScreenEvent && !event.isOn
            }
            TriggerType.SCREEN_OFF -> {
                event is AutomationEvent.ScreenEvent && event.isOn
            }
            else -> false
        }
    }

    private fun isLocationMatch(event: AutomationEvent.LocationEvent, trigger: TriggerConfig, modeName: String): Boolean {
        val configuredName = trigger.params["locationName"] ?: ""
        if (configuredName.isNotBlank()) {
            if (event.locationName.contains(configuredName, ignoreCase = true) ||
                configuredName.contains(event.locationName, ignoreCase = true)) {
                return true
            }
        }
        if (event.locationName.contains(modeName, ignoreCase = true) ||
            modeName.contains(event.locationName, ignoreCase = true)) {
            return true
        }
        if (configuredName.isBlank()) return true
        return false
    }

    private fun isTriggerCurrentlySatisfied(trigger: TriggerConfig): Boolean {
        return when (trigger.type) {
            TriggerType.TIME_RANGE -> {
                val startTime = trigger.params["startTime"] ?: return false
                val endTime = trigger.params["endTime"] ?: return false
                val cal = java.util.Calendar.getInstance()
                val currentMins = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
                val startMins = parseMinutes(startTime) ?: return false
                val endMins = parseMinutes(endTime) ?: return false
                if (startMins <= endMins) {
                    currentMins in startMins..endMins
                } else {
                    currentMins >= startMins || currentMins <= endMins
                }
            }
            TriggerType.DAY_OF_WEEK -> {
                val daysStr = trigger.params["days"] ?: return false
                val days = daysStr.split(",").mapNotNull { it.trim().toIntOrNull() }
                val cal = java.util.Calendar.getInstance()
                val currentDay = cal.get(java.util.Calendar.DAY_OF_WEEK)
                days.contains(currentDay)
            }
            else -> true
        }
    }

    private fun parseMinutes(timeStr: String): Int? {
        val parts = timeStr.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return h * 60 + m
    }
}
