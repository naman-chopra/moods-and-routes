package com.ndev.moodyroutine.engine

import com.ndev.moodyroutine.data.model.TriggerConfig
import com.ndev.moodyroutine.data.model.TriggerType

class ConditionEvaluator {
    fun evaluate(event: AutomationEvent, trigger: TriggerConfig): Boolean {
        return when (trigger.type) {
            TriggerType.TIME_OF_DAY -> {
                if (event is AutomationEvent.TimeEvent) {
                    val hour = trigger.params["hour"]?.toIntOrNull() ?: return false
                    val minute = trigger.params["minute"]?.toIntOrNull() ?: return false
                    event.hour == hour && event.minute == minute
                } else false
            }
            TriggerType.BATTERY_LEVEL -> {
                if (event is AutomationEvent.BatteryEvent) {
                    val level = trigger.params["level"]?.toIntOrNull() ?: return false
                    val comparison = trigger.params["comparison"] ?: return false
                    when (comparison) {
                        "above" -> event.level > level
                        "below" -> event.level < level
                        else -> event.level == level
                    }
                } else false
            }
            TriggerType.BATTERY_CHARGING -> {
                if (event is AutomationEvent.BatteryEvent) {
                    event.isCharging
                } else false
            }
            TriggerType.WIFI_CONNECTED -> {
                if (event is AutomationEvent.WifiEvent) {
                    event.isConnected
                } else false
            }
            TriggerType.WIFI_SPECIFIC_NETWORK -> {
                if (event is AutomationEvent.WifiEvent) {
                    val wifiName = trigger.params["wifiName"]
                    event.isConnected && event.ssid == wifiName
                } else false
            }
            TriggerType.BLUETOOTH_CONNECTED -> {
                if (event is AutomationEvent.BluetoothEvent) {
                    event.isConnected
                } else false
            }
            TriggerType.BLUETOOTH_SPECIFIC_DEVICE -> {
                if (event is AutomationEvent.BluetoothEvent) {
                    val deviceName = trigger.params["deviceName"]
                    val deviceAddress = trigger.params["deviceAddress"]
                    event.isConnected && (event.deviceName == deviceName || event.deviceAddress == deviceAddress)
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
