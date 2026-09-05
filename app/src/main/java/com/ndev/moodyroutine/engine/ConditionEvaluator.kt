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
            TriggerType.LOCATION_ARRIVE -> {
                if (event is AutomationEvent.LocationEvent) {
                    val locationName = trigger.params["locationName"] ?: ""
                    event.isEntering && (locationName.isBlank() || event.locationName.contains(locationName, ignoreCase = true))
                } else false
            }
            TriggerType.LOCATION_LEAVE -> {
                if (event is AutomationEvent.LocationEvent) {
                    val locationName = trigger.params["locationName"] ?: ""
                    !event.isEntering && (locationName.isBlank() || event.locationName.contains(locationName, ignoreCase = true))
                } else false
            }
            TriggerType.BATTERY_LEVEL -> {
                if (event is AutomationEvent.BatteryEvent) {
                    val level = trigger.params["level"]?.toIntOrNull() ?: return false
                    val comparison = trigger.params["comparison"] ?: "below"
                    when (comparison) {
                        "above" -> event.level >= level
                        "below" -> event.level <= level
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
                    event.isConnected && event.ssid?.replace("\"", "") == wifiName?.replace("\"", "")
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
                        (deviceAddress != null && event.deviceAddress == deviceAddress)
                    )
                } else false
            }
            TriggerType.APP_OPENED -> {
                if (event is AutomationEvent.AppEvent) {
                    val packageName = trigger.params["packageName"]
                    event.isOpened && event.packageName == packageName
                } else false
            }
            TriggerType.APP_CLOSED -> {
                if (event is AutomationEvent.AppEvent) {
                    val packageName = trigger.params["packageName"]
                    !event.isOpened && event.packageName == packageName
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
            else -> false
        }
    }
}
