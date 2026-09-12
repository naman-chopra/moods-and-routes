package com.ndev.moodyroutine.engine

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import com.ndev.moodyroutine.data.model.Routine
import com.ndev.moodyroutine.data.model.TriggerConfig
import com.ndev.moodyroutine.data.model.TriggerMatchType
import com.ndev.moodyroutine.data.model.TriggerType
import com.ndev.moodyroutine.data.repository.ModeRepository
import com.ndev.moodyroutine.data.repository.RoutineRepository
import com.ndev.moodyroutine.service.LocationTracker
import com.ndev.moodyroutine.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class AutomationEngine(
    private val context: Context,
    private val routineRepository: RoutineRepository,
    private val modeRepository: ModeRepository,
    private val conditionEvaluator: ConditionEvaluator,
    private val actionExecutor: ActionExecutor,
    private val locationTrackerProvider: (() -> LocationTracker?)? = null
) {
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private val activeTriggers = ConcurrentHashMap<Long, MutableSet<Int>>() // routineId -> satisfied trigger indices

    // Execution guard: in-memory state tracking to eliminate race conditions with Room DB
    private val activeRoutineIds = ConcurrentHashMap.newKeySet<Long>()
    private val routineLastExecutionTime = ConcurrentHashMap<Long, Long>()
    private val recentLocationEvents = ConcurrentHashMap<String, Long>()

    companion object {
        private const val ROUTINE_COOLDOWN_MS = 15_000L // 15s debounce cooldown per routine
        private const val LOCATION_DEDUP_WINDOW_MS = 10_000L // 10s deduplication between parallel providers
    }

    fun start() {
        if (job != null) return
        job = scope.launch {
            AppLogger.i("AutomationEngine", "AutomationEngine started")
            // Use sequential collect so in-flight action execution and snapshot saves are never cancelled
            EventBus.events.collect { event ->
                AppLogger.d("AutomationEngine", "Received event: $event")
                try {
                    if (event is AutomationEvent.LocationEvent && !event.isInitial) {
                        val key = "${event.locationName.lowercase()}_${event.isEntering}"
                        val now = System.currentTimeMillis()
                        val lastSeen = recentLocationEvents[key] ?: 0L
                        if (now - lastSeen < LOCATION_DEDUP_WINDOW_MS) {
                            AppLogger.d("AutomationEngine", "Suppressed duplicate location event for $key (${now - lastSeen}ms ago)")
                            return@collect
                        }
                        recentLocationEvents[key] = now
                    }

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

            val isCurrentlyActive = routine.isActive || activeRoutineIds.contains(routine.id)

            if (exitMatched && isCurrentlyActive) {
                AppLogger.i("AutomationEngine", "Exit condition matched for routine: ${routine.name}. Deactivating and reverting actions.")
                activeRoutineIds.remove(routine.id)
                routineRepository.setRoutineActive(routine.id, false)
                if (routine.revertActionsOnExit) {
                    actionExecutor.revertRoutine(routine)
                }
                activeTriggers.remove(routine.id)
                continue
            }

            // 2. Evaluate triggers
            if (!exitMatched) {
                if (event is AutomationEvent.LocationEvent && event.isInitial) {
                    continue
                }
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
                    // Edge-triggered execution: only execute on rising transition into active state
                    if (!isCurrentlyActive) {
                        val now = System.currentTimeMillis()
                        val lastExec = routineLastExecutionTime[routine.id] ?: 0L
                        if (now - lastExec >= ROUTINE_COOLDOWN_MS) {
                            routineLastExecutionTime[routine.id] = now
                            activeRoutineIds.add(routine.id)
                            AppLogger.i("AutomationEngine", "Executing routine: ${routine.name}")
                            routineRepository.setRoutineActive(routine.id, true)
                            actionExecutor.executeRoutine(routine)
                            routineRepository.updateLastTriggered(routine.id, now)
                        } else {
                            AppLogger.d("AutomationEngine", "Skipping routine ${routine.name}: cooldown active (${(now - lastExec) / 1000}s / ${ROUTINE_COOLDOWN_MS / 1000}s)")
                        }
                    }
                    // Reset satisfied indices after evaluation
                    satisfiedIndices.clear()
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
                    val targetSsid = trigger.params["wifiName"] ?: trigger.params["ssid"]
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
            TriggerType.BATTERY_LEVEL -> {
                if (event is AutomationEvent.BatteryEvent) {
                    val targetLevel = trigger.params["level"]?.toIntOrNull() ?: return false
                    val comparison = trigger.params["comparison"] ?: "below"
                    when (comparison) {
                        "above" -> event.level < targetLevel
                        "below" -> event.level > targetLevel || event.isCharging
                        "equal" -> event.level != targetLevel
                        else -> event.level != targetLevel
                    }
                } else false
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
            TriggerType.TIME_OF_DAY -> {
                if (event is AutomationEvent.TimeEvent) {
                    val timeStr = trigger.params["time"]
                    val (hour, minute) = if (timeStr != null && timeStr.contains(":")) {
                        val parts = timeStr.split(":")
                        Pair(parts[0].toIntOrNull(), parts[1].toIntOrNull())
                    } else {
                        Pair(trigger.params["hour"]?.toIntOrNull(), trigger.params["minute"]?.toIntOrNull())
                    }
                    if (hour == null || minute == null) return false
                    event.hour != hour || event.minute != minute
                } else false
            }
            TriggerType.APP_OPENED -> {
                if (event is AutomationEvent.AppEvent && !event.isOpened) {
                    val pkg = trigger.params["packageName"]
                    pkg.isNullOrBlank() || event.packageName == pkg
                } else false
            }
            TriggerType.APP_CLOSED -> {
                if (event is AutomationEvent.AppEvent && event.isOpened) {
                    val pkg = trigger.params["packageName"]
                    pkg.isNullOrBlank() || event.packageName == pkg
                } else false
            }
            TriggerType.SCREEN_ON -> {
                event is AutomationEvent.ScreenEvent && !event.isOn
            }
            TriggerType.SCREEN_OFF -> {
                event is AutomationEvent.ScreenEvent && event.isOn
            }
        }
    }

    private fun isLocationMatch(event: AutomationEvent.LocationEvent, trigger: TriggerConfig, modeName: String): Boolean {
        val trigLat = trigger.params["latitude"]?.toDoubleOrNull()
        val trigLng = trigger.params["longitude"]?.toDoubleOrNull()
        val trigRad = trigger.params["radius"]?.toFloatOrNull() ?: 150f

        if (trigLat != null && trigLng != null && event.latitude != null && event.longitude != null) {
            val results = FloatArray(1)
            android.location.Location.distanceBetween(event.latitude, event.longitude, trigLat, trigLng, results)
            if (results[0] > trigRad + 1000f) {
                return false
            }
        }

        val configuredName = trigger.params["locationName"] ?: ""
        if (configuredName.isNotBlank()) {
            return event.locationName.contains(configuredName, ignoreCase = true) ||
                    configuredName.contains(event.locationName, ignoreCase = true)
        }
        if (modeName.isNotBlank()) {
            return event.locationName.contains(modeName, ignoreCase = true) ||
                    modeName.contains(event.locationName, ignoreCase = true)
        }
        return false
    }

    private fun isTriggerCurrentlySatisfied(trigger: TriggerConfig): Boolean {
        return when (trigger.type) {
            TriggerType.LOCATION_ARRIVE -> {
                val lat = trigger.params["latitude"]?.toDoubleOrNull() ?: return false
                val lng = trigger.params["longitude"]?.toDoubleOrNull() ?: return false
                val rad = trigger.params["radius"]?.toFloatOrNull() ?: 150f
                locationTrackerProvider?.invoke()?.isInside(lat, lng, rad) ?: false
            }
            TriggerType.LOCATION_LEAVE -> {
                val lat = trigger.params["latitude"]?.toDoubleOrNull() ?: return false
                val lng = trigger.params["longitude"]?.toDoubleOrNull() ?: return false
                val rad = trigger.params["radius"]?.toFloatOrNull() ?: 150f
                val isInside = locationTrackerProvider?.invoke()?.isInside(lat, lng, rad) ?: true
                !isInside
            }
            TriggerType.WIFI_CONNECTED -> {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            }
            TriggerType.WIFI_DISCONNECTED -> {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) != true
            }
            TriggerType.WIFI_SPECIFIC_NETWORK -> {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
                if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                    val targetWifi = trigger.params["wifiName"] ?: trigger.params["ssid"]
                    if (targetWifi.isNullOrBlank()) return true
                    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
                    @Suppress("DEPRECATION")
                    val currentSsid = wm?.connectionInfo?.ssid?.replace("\"", "")
                    currentSsid?.equals(targetWifi.replace("\"", ""), ignoreCase = true) == true
                } else false
            }
            TriggerType.POWER_CONNECTED, TriggerType.BATTERY_CHARGING -> {
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            }
            TriggerType.POWER_DISCONNECTED, TriggerType.BATTERY_DISCHARGING -> {
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                status != BatteryManager.BATTERY_STATUS_CHARGING && status != BatteryManager.BATTERY_STATUS_FULL
            }
            TriggerType.BATTERY_LEVEL -> {
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val currentLevel = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val targetLevel = trigger.params["level"]?.toIntOrNull() ?: return false
                val comparison = trigger.params["comparison"] ?: "below"
                if (currentLevel < 0) return false
                when (comparison) {
                    "above" -> currentLevel >= targetLevel
                    "below" -> currentLevel <= targetLevel
                    "equal" -> currentLevel == targetLevel
                    else -> currentLevel == targetLevel
                }
            }
            TriggerType.BLUETOOTH_CONNECTED -> {
                val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
                val adapter = bm?.adapter
                adapter?.isEnabled == true && (
                    adapter.getProfileConnectionState(android.bluetooth.BluetoothProfile.HEADSET) == android.bluetooth.BluetoothProfile.STATE_CONNECTED ||
                    adapter.getProfileConnectionState(android.bluetooth.BluetoothProfile.A2DP) == android.bluetooth.BluetoothProfile.STATE_CONNECTED
                )
            }
            TriggerType.BLUETOOTH_DISCONNECTED -> {
                val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
                val adapter = bm?.adapter
                adapter?.isEnabled != true || (
                    adapter.getProfileConnectionState(android.bluetooth.BluetoothProfile.HEADSET) != android.bluetooth.BluetoothProfile.STATE_CONNECTED &&
                    adapter.getProfileConnectionState(android.bluetooth.BluetoothProfile.A2DP) != android.bluetooth.BluetoothProfile.STATE_CONNECTED
                )
            }
            TriggerType.BLUETOOTH_SPECIFIC_DEVICE -> {
                val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
                val adapter = bm?.adapter
                val targetName = trigger.params["deviceName"]
                val targetAddress = trigger.params["deviceAddress"]
                if (adapter?.isEnabled == true) {
                    try {
                        @Suppress("DEPRECATION")
                        val bonded = adapter.bondedDevices ?: emptySet()
                        bonded.any { dev ->
                            val nameMatch = targetName != null && dev.name?.contains(targetName, ignoreCase = true) == true
                            val addrMatch = targetAddress != null && dev.address.equals(targetAddress, ignoreCase = true)
                            (nameMatch || addrMatch)
                        }
                    } catch (_: SecurityException) { false }
                } else false
            }
            TriggerType.HEADPHONE_CONNECTED -> {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                @Suppress("DEPRECATION")
                (am?.isWiredHeadsetOn == true || am?.isBluetoothA2dpOn == true)
            }
            TriggerType.HEADPHONE_DISCONNECTED -> {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                @Suppress("DEPRECATION")
                !(am?.isWiredHeadsetOn == true || am?.isBluetoothA2dpOn == true)
            }
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
            TriggerType.TIME_OF_DAY -> {
                val timeStr = trigger.params["time"]
                val (hour, minute) = if (timeStr != null && timeStr.contains(":")) {
                    val parts = timeStr.split(":")
                    Pair(parts[0].toIntOrNull(), parts[1].toIntOrNull())
                } else {
                    Pair(trigger.params["hour"]?.toIntOrNull(), trigger.params["minute"]?.toIntOrNull())
                }
                if (hour == null || minute == null) return false
                val cal = java.util.Calendar.getInstance()
                val currentHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                val currentMinute = cal.get(java.util.Calendar.MINUTE)
                if (currentHour != hour || currentMinute != minute) return false

                val daysStr = trigger.params["days"]
                if (!daysStr.isNullOrBlank()) {
                    val days = daysStr.split(",").mapNotNull { it.trim().toIntOrNull() }
                    if (days.isNotEmpty() && !days.contains(cal.get(java.util.Calendar.DAY_OF_WEEK))) {
                        return false
                    }
                }
                true
            }
            TriggerType.SCREEN_ON -> {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                pm?.isInteractive == true
            }
            TriggerType.SCREEN_OFF -> {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                pm?.isInteractive == false
            }
            else -> false
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
