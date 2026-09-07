package com.ndev.moodyroutine.engine

import android.content.Context
import android.util.Log
import com.ndev.moodyroutine.data.model.TriggerMatchType
import com.ndev.moodyroutine.data.model.TriggerType
import com.ndev.moodyroutine.data.repository.ModeRepository
import com.ndev.moodyroutine.data.repository.RoutineRepository
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
            Log.i("MoodyRoutine", "AutomationEngine started")
            EventBus.events.collectLatest { event ->
                Log.d("MoodyRoutine", "Received event: $event")
                try {
                    processEventForRoutines(event)
                    processEventForModes(event)
                } catch (e: Exception) {
                    Log.e("MoodyRoutine", "Error processing event $event", e)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        Log.i("MoodyRoutine", "AutomationEngine stopped")
    }

    private suspend fun processEventForRoutines(event: AutomationEvent) {
        val routines = try {
            routineRepository.getEnabledRoutines().first()
        } catch (e: Exception) {
            emptyList()
        }

        for (routine in routines) {
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
                Log.i("MoodyRoutine", "Executing routine: ${routine.name}")
                actionExecutor.executeAll(routine.actions)
                routineRepository.updateLastTriggered(routine.id, System.currentTimeMillis())
                satisfiedIndices.clear() // reset after execution
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

            var triggerMatched = false
            var exitMatched = false

            for (trigger in mode.autoTriggers) {
                val matches = conditionEvaluator.evaluate(event, trigger)
                if (matches) {
                    if (trigger.type == TriggerType.LOCATION_LEAVE) {
                        exitMatched = true
                    } else {
                        triggerMatched = true
                    }
                } else {
                    // Check inverse exit conditions
                    when (trigger.type) {
                        TriggerType.WIFI_CONNECTED -> {
                            if (event is AutomationEvent.WifiEvent && !event.isConnected) exitMatched = true
                        }
                        TriggerType.WIFI_DISCONNECTED -> {
                            if (event is AutomationEvent.WifiEvent && event.isConnected) exitMatched = true
                        }
                        TriggerType.WIFI_SPECIFIC_NETWORK -> {
                            if (event is AutomationEvent.WifiEvent && !event.isConnected) exitMatched = true
                        }
                        TriggerType.BLUETOOTH_CONNECTED -> {
                            if (event is AutomationEvent.BluetoothEvent && !event.isConnected) exitMatched = true
                        }
                        TriggerType.BLUETOOTH_DISCONNECTED -> {
                            if (event is AutomationEvent.BluetoothEvent && event.isConnected) exitMatched = true
                        }
                        TriggerType.POWER_CONNECTED -> {
                            if (event is AutomationEvent.PowerEvent && !event.isConnected) exitMatched = true
                        }
                        TriggerType.POWER_DISCONNECTED -> {
                            if (event is AutomationEvent.PowerEvent && event.isConnected) exitMatched = true
                        }
                        TriggerType.HEADPHONE_CONNECTED -> {
                            if (event is AutomationEvent.HeadphoneEvent && !event.isConnected) exitMatched = true
                        }
                        TriggerType.HEADPHONE_DISCONNECTED -> {
                            if (event is AutomationEvent.HeadphoneEvent && event.isConnected) exitMatched = true
                        }
                        TriggerType.LOCATION_ARRIVE -> {
                            if (event is AutomationEvent.LocationEvent && !event.isEntering) {
                                val locName = trigger.params["locationName"] ?: ""
                                if (locName.isBlank() || event.locationName.contains(locName, ignoreCase = true) || locName.contains(event.locationName, ignoreCase = true)) {
                                    exitMatched = true
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }

            if (triggerMatched) {
                Log.i("MoodyRoutine", "Trigger matched for mode: ${mode.name}. Activating and executing actions.")
                if (!mode.isActive) {
                    modeRepository.setModeActive(mode.id, true)
                }
                actionExecutor.executeAll(mode.actions)
            } else if (exitMatched && mode.isActive) {
                Log.i("MoodyRoutine", "Exit condition matched for mode: ${mode.name}. Deactivating.")
                modeRepository.setModeActive(mode.id, false)
            }
        }
    }
}
