package com.ndev.moodyroutine.ui.util

import com.ndev.moodyroutine.data.model.ActionConfig
import com.ndev.moodyroutine.data.model.ActionType
import com.ndev.moodyroutine.data.model.TriggerConfig
import com.ndev.moodyroutine.data.model.TriggerType

object HumanFormatter {

    fun formatTrigger(trigger: TriggerConfig): Pair<String, String> {
        val title = when (trigger.type) {
            TriggerType.LOCATION_ARRIVE, TriggerType.LOCATION_LEAVE -> "Place"
            else -> trigger.type.displayName
        }
        val subtitle = when (trigger.type) {
            TriggerType.TIME_OF_DAY -> {
                val time = trigger.params["time"] ?: "08:00"
                val days = trigger.params["days"]?.let { formatDays(it) } ?: "Every day"
                "$time ($days)"
            }
            TriggerType.TIME_RANGE -> {
                val start = trigger.params["startTime"] ?: "09:00"
                val end = trigger.params["endTime"] ?: "17:00"
                "$start - $end"
            }
            TriggerType.DAY_OF_WEEK -> {
                val days = trigger.params["days"]?.let { formatDays(it) } ?: "Weekdays"
                days
            }
            TriggerType.LOCATION_ARRIVE -> {
                val loc = trigger.params["locationName"] ?: trigger.params["address"] ?: "Selected place"
                "When I arrive at $loc"
            }
            TriggerType.LOCATION_LEAVE -> {
                val loc = trigger.params["locationName"] ?: trigger.params["address"] ?: "Selected place"
                "When I leave $loc"
            }
            TriggerType.BATTERY_LEVEL -> {
                val level = trigger.params["level"] ?: "20"
                val comparison = trigger.params["comparison"] ?: "below"
                if (comparison == "above") "Equal to or above $level%" else "Equal to or below $level%"
            }
            TriggerType.BATTERY_CHARGING -> "When device starts charging"
            TriggerType.BATTERY_DISCHARGING -> "When device is unplugged"
            TriggerType.WIFI_CONNECTED -> "Connected to any Wi-Fi network"
            TriggerType.WIFI_DISCONNECTED -> "Disconnected from Wi-Fi"
            TriggerType.WIFI_SPECIFIC_NETWORK -> {
                val name = trigger.params["wifiName"] ?: "Specific network"
                "Connected to \"$name\""
            }
            TriggerType.BLUETOOTH_CONNECTED -> "Connected to any Bluetooth device"
            TriggerType.BLUETOOTH_DISCONNECTED -> "Disconnected from Bluetooth"
            TriggerType.BLUETOOTH_SPECIFIC_DEVICE -> {
                val device = trigger.params["deviceName"] ?: "Specific device"
                "Connected to \"$device\""
            }
            TriggerType.APP_OPENED -> {
                val app = trigger.params["appName"] ?: trigger.params["packageName"] ?: "Selected app"
                "When $app is opened"
            }
            TriggerType.APP_CLOSED -> {
                val app = trigger.params["appName"] ?: trigger.params["packageName"] ?: "Selected app"
                "When $app is closed"
            }
            TriggerType.HEADPHONE_CONNECTED -> "When headphones are plugged in"
            TriggerType.HEADPHONE_DISCONNECTED -> "When headphones are disconnected"
            TriggerType.SCREEN_ON -> "When screen turns on"
            TriggerType.SCREEN_OFF -> "When screen turns off"
            TriggerType.POWER_CONNECTED -> "When power cable connected"
            TriggerType.POWER_DISCONNECTED -> "When power cable disconnected"
        }
        return Pair(title, subtitle)
    }

    fun formatAction(action: ActionConfig): Pair<String, String> {
        val title = action.type.displayName
        val subtitle = when (action.type) {
            ActionType.SET_VOLUME_RING -> "Set to ${action.params["volume"] ?: "50"}%"
            ActionType.SET_VOLUME_MEDIA -> "Set to ${action.params["volume"] ?: "50"}%"
            ActionType.SET_VOLUME_ALARM -> "Set to ${action.params["volume"] ?: "50"}%"
            ActionType.SET_VOLUME_NOTIFICATION -> "Set to ${action.params["volume"] ?: "50"}%"
            ActionType.SET_DND_ON -> "Turn on"
            ActionType.SET_DND_OFF -> "Turn off"
            ActionType.SET_DND_PRIORITY_ONLY -> "Priority only"
            ActionType.SET_DND_ALARMS_ONLY -> "Alarms only"
            ActionType.SET_BRIGHTNESS -> "Set to ${action.params["brightness"] ?: "50"}%"
            ActionType.OPEN_APP -> {
                val app = action.params["appName"] ?: action.params["packageName"] ?: "Selected app"
                "Open $app"
            }
            ActionType.CLOSE_APP -> {
                val app = action.params["appName"] ?: action.params["packageName"] ?: "Selected app"
                "Close $app"
            }
            ActionType.SET_WALLPAPER -> "Change wallpaper"
            ActionType.TOGGLE_AUTO_ROTATE_ON -> "Turn on"
            ActionType.TOGGLE_AUTO_ROTATE_OFF -> "Turn off"
            ActionType.TOGGLE_FLASHLIGHT_ON -> "Turn on"
            ActionType.TOGGLE_FLASHLIGHT_OFF -> "Turn off"
            ActionType.SET_RINGER_NORMAL -> "Sound on"
            ActionType.SET_RINGER_VIBRATE -> "Vibrate"
            ActionType.SET_RINGER_SILENT -> "Mute"
            ActionType.SEND_NOTIFICATION -> {
                val msg = action.params["title"] ?: "Notification"
                "\"$msg\""
            }
            ActionType.ENABLE_DARK_MODE -> "Turn on"
            ActionType.DISABLE_DARK_MODE -> "Turn off"
        }
        return Pair(title, subtitle)
    }

    private fun formatDays(daysStr: String): String {
        val days = daysStr.split(",").mapNotNull { it.trim().toIntOrNull() }.sorted()
        if (days.size == 7) return "Every day"
        if (days == listOf(2, 3, 4, 5, 6)) return "Weekdays"
        if (days == listOf(1, 7)) return "Weekends"
        val dayNames = mapOf(
            1 to "Sun", 2 to "Mon", 3 to "Tue", 4 to "Wed", 5 to "Thu", 6 to "Fri", 7 to "Sat"
        )
        return days.mapNotNull { dayNames[it] }.joinToString(", ")
    }
}
