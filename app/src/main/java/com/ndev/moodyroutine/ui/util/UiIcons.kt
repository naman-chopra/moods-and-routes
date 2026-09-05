package com.ndev.moodyroutine.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.Launch
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ndev.moodyroutine.data.model.ActionCategory
import com.ndev.moodyroutine.data.model.ActionType
import com.ndev.moodyroutine.data.model.TriggerCategory
import com.ndev.moodyroutine.data.model.TriggerType
import com.ndev.moodyroutine.ui.theme.*

object UiIcons {

    fun getTriggerIcon(type: TriggerType): ImageVector {
        return when (type) {
            TriggerType.TIME_OF_DAY -> Icons.Rounded.Schedule
            TriggerType.TIME_RANGE -> Icons.Rounded.DateRange
            TriggerType.DAY_OF_WEEK -> Icons.Rounded.CalendarMonth
            TriggerType.LOCATION_ARRIVE -> Icons.Rounded.LocationOn
            TriggerType.LOCATION_LEAVE -> Icons.Rounded.LocationOff
            TriggerType.BATTERY_LEVEL -> Icons.Rounded.BatteryFull
            TriggerType.BATTERY_CHARGING -> Icons.Rounded.BatteryChargingFull
            TriggerType.BATTERY_DISCHARGING -> Icons.Rounded.BatteryAlert
            TriggerType.WIFI_CONNECTED -> Icons.Rounded.Wifi
            TriggerType.WIFI_DISCONNECTED -> Icons.Rounded.WifiOff
            TriggerType.WIFI_SPECIFIC_NETWORK -> Icons.Rounded.Wifi
            TriggerType.BLUETOOTH_CONNECTED -> Icons.Rounded.Bluetooth
            TriggerType.BLUETOOTH_DISCONNECTED -> Icons.Rounded.BluetoothDisabled
            TriggerType.BLUETOOTH_SPECIFIC_DEVICE -> Icons.Rounded.BluetoothConnected
            TriggerType.APP_OPENED -> Icons.Rounded.Apps
            TriggerType.APP_CLOSED -> Icons.AutoMirrored.Rounded.ExitToApp
            TriggerType.HEADPHONE_CONNECTED -> Icons.Rounded.Headphones
            TriggerType.HEADPHONE_DISCONNECTED -> Icons.Rounded.HeadsetOff
            TriggerType.SCREEN_ON -> Icons.Rounded.Smartphone
            TriggerType.SCREEN_OFF -> Icons.Rounded.PhoneAndroid
            TriggerType.POWER_CONNECTED -> Icons.Rounded.Power
            TriggerType.POWER_DISCONNECTED -> Icons.Rounded.PowerOff
        }
    }

    fun getTriggerColor(category: TriggerCategory): Color {
        return when (category) {
            TriggerCategory.TIME -> AccentPurple
            TriggerCategory.LOCATION -> Color(0xFFE11D48)
            TriggerCategory.BATTERY -> AccentGreen
            TriggerCategory.CONNECTIVITY -> SamsungBlue
            TriggerCategory.APP -> AccentPink
            TriggerCategory.DEVICE -> AccentOrange
        }
    }

    fun getActionIcon(type: ActionType): ImageVector {
        return when (type) {
            ActionType.SET_VOLUME_RING -> Icons.AutoMirrored.Rounded.VolumeUp
            ActionType.SET_VOLUME_MEDIA -> Icons.Rounded.MusicNote
            ActionType.SET_VOLUME_ALARM -> Icons.Rounded.Alarm
            ActionType.SET_VOLUME_NOTIFICATION -> Icons.Rounded.Notifications
            ActionType.SET_DND_ON -> Icons.Rounded.DoNotDisturbOn
            ActionType.SET_DND_OFF -> Icons.Rounded.DoNotDisturbOff
            ActionType.SET_DND_PRIORITY_ONLY -> Icons.Rounded.Stars
            ActionType.SET_DND_ALARMS_ONLY -> Icons.Rounded.AlarmOn
            ActionType.SET_BRIGHTNESS -> Icons.Rounded.BrightnessMedium
            ActionType.OPEN_APP -> Icons.AutoMirrored.Rounded.Launch
            ActionType.CLOSE_APP -> Icons.Rounded.Close
            ActionType.SET_WALLPAPER -> Icons.Rounded.Wallpaper
            ActionType.TOGGLE_AUTO_ROTATE_ON -> Icons.Rounded.ScreenRotation
            ActionType.TOGGLE_AUTO_ROTATE_OFF -> Icons.Rounded.ScreenLockPortrait
            ActionType.TOGGLE_FLASHLIGHT_ON -> Icons.Rounded.FlashlightOn
            ActionType.TOGGLE_FLASHLIGHT_OFF -> Icons.Rounded.FlashlightOff
            ActionType.SET_RINGER_NORMAL -> Icons.Rounded.NotificationsActive
            ActionType.SET_RINGER_VIBRATE -> Icons.Rounded.Vibration
            ActionType.SET_RINGER_SILENT -> Icons.Rounded.VolumeOff
            ActionType.SEND_NOTIFICATION -> Icons.AutoMirrored.Rounded.Message
            ActionType.ENABLE_DARK_MODE -> Icons.Rounded.DarkMode
            ActionType.DISABLE_DARK_MODE -> Icons.Rounded.LightMode
        }
    }

    fun getActionColor(category: ActionCategory): Color {
        return when (category) {
            ActionCategory.SOUND -> AccentTeal
            ActionCategory.DISPLAY -> AccentOrange
            ActionCategory.APPS -> AccentPurple
            ActionCategory.DEVICE -> AccentRed
            ActionCategory.NOTIFICATION -> SamsungBlue
        }
    }

    fun getModeIcon(iconName: String): ImageVector {
        return when (iconName.lowercase()) {
            "bedtime", "sleep", "night" -> Icons.Rounded.Bedtime
            "car", "driving" -> Icons.Rounded.DirectionsCar
            "fitness", "exercise", "workout" -> Icons.Rounded.FitnessCenter
            "spa", "relax" -> Icons.Rounded.Spa
            "work", "business" -> Icons.Rounded.Work
            "game", "gaming" -> Icons.Rounded.SportsEsports
            "theater", "movie" -> Icons.Rounded.Theaters
            "music" -> Icons.Rounded.MusicNote
            "book", "reading" -> Icons.Rounded.MenuBook
            else -> Icons.Rounded.Palette
        }
    }
}
