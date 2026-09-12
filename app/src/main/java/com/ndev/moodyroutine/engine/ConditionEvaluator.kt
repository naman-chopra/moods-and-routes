package com.ndev.moodyroutine.engine

import com.ndev.moodyroutine.data.model.TriggerConfig
import com.ndev.moodyroutine.data.model.TriggerType

class ConditionEvaluator {
    fun evaluate(event: AutomationEvent, trigger: TriggerConfig): Boolean {
        return when (trigger.type) {
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
                    if (event.hour != hour || event.minute != minute) return false

                    val daysStr = trigger.params["days"]
                    if (!daysStr.isNullOrBlank()) {
                        val days = daysStr.split(",").mapNotNull { it.trim().toIntOrNull() }
                        if (days.isNotEmpty() && !days.contains(event.dayOfWeek)) {
                            return false
                        }
                    }
                    true
                } else false
            }
            TriggerType.TIME_RANGE -> {
                if (event is AutomationEvent.TimeEvent) {
                    val startTime = trigger.params["startTime"] ?: return false
                    val endTime = trigger.params["endTime"] ?: return false
                    val currentMins = event.hour * 60 + event.minute
                    val startMins = parseMinutes(startTime) ?: return false
                    val endMins = parseMinutes(endTime) ?: return false
                    if (startMins <= endMins) {
                        currentMins in startMins..endMins
                    } else {
                        currentMins >= startMins || currentMins <= endMins
                    }
                } else false
            }
            TriggerType.DAY_OF_WEEK -> {
                if (event is AutomationEvent.TimeEvent) {
                    val daysStr = trigger.params["days"] ?: return false
                    val days = daysStr.split(",").mapNotNull { it.trim().toIntOrNull() }
                    days.contains(event.dayOfWeek)
                } else false
            }
            TriggerType.LOCATION_ARRIVE -> {
                if (event is AutomationEvent.LocationEvent) {
                    if (!event.isEntering) return false
                    val trigLat = trigger.params["latitude"]?.toDoubleOrNull()
                    val trigLng = trigger.params["longitude"]?.toDoubleOrNull()
                    val trigRad = trigger.params["radius"]?.toFloatOrNull() ?: 150f

                    if (trigLat != null && trigLng != null && event.latitude != null && event.longitude != null) {
                        val results = FloatArray(1)
                        android.location.Location.distanceBetween(event.latitude, event.longitude, trigLat, trigLng, results)
                        results[0] <= trigRad
                    } else {
                        val locationName = trigger.params["locationName"] ?: ""
                        if (locationName.isNotBlank()) {
                            event.locationName.contains(locationName, ignoreCase = true) ||
                            locationName.contains(event.locationName, ignoreCase = true)
                        } else false
                    }
                } else false
            }
            TriggerType.LOCATION_LEAVE -> {
                if (event is AutomationEvent.LocationEvent) {
                    if (event.isEntering) return false
                    val trigLat = trigger.params["latitude"]?.toDoubleOrNull()
                    val trigLng = trigger.params["longitude"]?.toDoubleOrNull()
                    val trigRad = trigger.params["radius"]?.toFloatOrNull() ?: 150f

                    if (trigLat != null && trigLng != null && event.latitude != null && event.longitude != null) {
                        val results = FloatArray(1)
                        android.location.Location.distanceBetween(event.latitude, event.longitude, trigLat, trigLng, results)
                        // Must be in the vicinity of the exit location (within radius + 1km buffer), not across the country
                        if (results[0] > trigRad + 1000f) {
                            false
                        } else {
                            val locationName = trigger.params["locationName"] ?: ""
                            if (locationName.isNotBlank()) {
                                event.locationName.contains(locationName, ignoreCase = true) ||
                                locationName.contains(event.locationName, ignoreCase = true)
                            } else true
                        }
                    } else {
                        val locationName = trigger.params["locationName"] ?: ""
                        if (locationName.isNotBlank()) {
                            event.locationName.contains(locationName, ignoreCase = true) ||
                            locationName.contains(event.locationName, ignoreCase = true)
                        } else false
                    }
                } else false
            }
            TriggerType.BATTERY_LEVEL -> {
                if (event is AutomationEvent.BatteryEvent) {
                    val level = trigger.params["level"]?.toIntOrNull() ?: return false
                    val comparison = trigger.params["comparison"] ?: "below"
                    when (comparison) {
                        "above" -> event.level >= level
                        "below" -> event.level <= level
                        "equal" -> event.level == level
                        else -> event.level == level
                    }
                } else false
            }
            TriggerType.BATTERY_CHARGING -> {
                if (event is AutomationEvent.BatteryEvent) {
                    event.isCharging
                } else false
            }
            TriggerType.BATTERY_DISCHARGING -> {
                if (event is AutomationEvent.BatteryEvent) {
                    !event.isCharging
                } else false
            }
            TriggerType.WIFI_CONNECTED -> {
                if (event is AutomationEvent.WifiEvent) {
                    event.isConnected
                } else false
            }
            TriggerType.WIFI_DISCONNECTED -> {
                if (event is AutomationEvent.WifiEvent) {
                    !event.isConnected
                } else false
            }
            TriggerType.WIFI_SPECIFIC_NETWORK -> {
                if (event is AutomationEvent.WifiEvent) {
                    val wifiName = trigger.params["wifiName"]
                    event.isConnected && (
                        wifiName.isNullOrBlank() ||
                        event.ssid?.replace("\"", "")?.equals(wifiName.replace("\"", ""), ignoreCase = true) == true
                    )
                } else false
            }
            TriggerType.BLUETOOTH_CONNECTED -> {
                if (event is AutomationEvent.BluetoothEvent) {
                    event.isConnected
                } else false
            }
            TriggerType.BLUETOOTH_DISCONNECTED -> {
                if (event is AutomationEvent.BluetoothEvent) {
                    !event.isConnected
                } else false
            }
            TriggerType.BLUETOOTH_SPECIFIC_DEVICE -> {
                if (event is AutomationEvent.BluetoothEvent) {
                    val deviceName = trigger.params["deviceName"]
                    val deviceAddress = trigger.params["deviceAddress"]
                    event.isConnected && (
                        (deviceName != null && event.deviceName?.contains(deviceName, ignoreCase = true) == true) ||
                        (deviceAddress != null && event.deviceAddress.equals(deviceAddress, ignoreCase = true))
                    )
                } else false
            }
            TriggerType.APP_OPENED -> {
                if (event is AutomationEvent.AppEvent) {
                    val packageName = trigger.params["packageName"]
                    event.isOpened && (packageName.isNullOrBlank() || event.packageName == packageName)
                } else false
            }
            TriggerType.APP_CLOSED -> {
                if (event is AutomationEvent.AppEvent) {
                    val packageName = trigger.params["packageName"]
                    !event.isOpened && (packageName.isNullOrBlank() || event.packageName == packageName)
                } else false
            }
            TriggerType.HEADPHONE_CONNECTED -> {
                if (event is AutomationEvent.HeadphoneEvent) {
                    event.isConnected
                } else false
            }
            TriggerType.HEADPHONE_DISCONNECTED -> {
                if (event is AutomationEvent.HeadphoneEvent) {
                    !event.isConnected
                } else false
            }
            TriggerType.SCREEN_ON -> {
                if (event is AutomationEvent.ScreenEvent) {
                    event.isOn
                } else false
            }
            TriggerType.SCREEN_OFF -> {
                if (event is AutomationEvent.ScreenEvent) {
                    !event.isOn
                } else false
            }
            TriggerType.POWER_CONNECTED -> {
                if (event is AutomationEvent.PowerEvent) {
                    event.isConnected
                } else false
            }
            TriggerType.POWER_DISCONNECTED -> {
                if (event is AutomationEvent.PowerEvent) {
                    !event.isConnected
                } else false
            }
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
