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
            ActionType.RESTRICT_APPS -> Icons.Rounded.Block
            ActionType.SET_WALLPAPER -> Icons.Rounded.Wallpaper
            ActionType.SET_HOME_WALLPAPER -> Icons.Rounded.Wallpaper
            ActionType.SET_LOCK_WALLPAPER -> Icons.Rounded.Lock
            ActionType.WAIT_DELAY -> Icons.Rounded.HourglassEmpty
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

    val MODE_ICON_KEYS = listOf(
        "sleep",
        "driving",
        "exercise",
        "work",
        "relax",
        "game",
        "movie",
        "music",
        "book",
        "school",
        "code",
        "home",
        "coffee",
        "restaurant",
        "flight",
        "park",
        "shopping",
        "call",
        "groups",
        "focus",
        "run",
        "bike",
        "cleaning",
        "health",
        "headphones",
        "sunny",
        "shield",
        "custom"
    )

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
            "school", "study" -> Icons.Rounded.School
            "code", "coding", "dev" -> Icons.Rounded.Code
            "home", "house" -> Icons.Rounded.Home
            "coffee", "cafe" -> Icons.Rounded.Coffee
            "restaurant", "food", "dining" -> Icons.Rounded.Restaurant
            "flight", "travel", "plane" -> Icons.Rounded.Flight
            "park", "nature", "outdoor" -> Icons.Rounded.Park
            "shopping", "cart", "store" -> Icons.Rounded.ShoppingCart
            "call", "meeting", "phone" -> Icons.Rounded.PhoneInTalk
            "groups", "social", "team" -> Icons.Rounded.Groups
            "focus", "target" -> Icons.Rounded.CenterFocusStrong
            "run", "running", "walk" -> Icons.Rounded.DirectionsRun
            "bike", "cycling" -> Icons.Rounded.DirectionsBike
            "cleaning", "clean" -> Icons.Rounded.CleaningServices
            "health", "hospital", "medical" -> Icons.Rounded.LocalHospital
            "meditation" -> Icons.Rounded.SelfImprovement
            "headphones", "audio" -> Icons.Rounded.Headphones
            "sunny", "morning", "day" -> Icons.Rounded.WbSunny
            "shield", "security" -> Icons.Rounded.Shield
            "palette", "art" -> Icons.Rounded.Palette
            else -> Icons.Rounded.AutoAwesome
        }
    }

    fun getModeDrawableRes(iconName: String): Int {
        return when (iconName.lowercase()) {
            "bedtime", "sleep", "night" -> com.ndev.moodyroutine.R.drawable.ic_mode_sleep
            "car", "driving" -> com.ndev.moodyroutine.R.drawable.ic_mode_driving
            "fitness", "exercise", "workout", "gym" -> com.ndev.moodyroutine.R.drawable.ic_mode_exercise
            "spa", "relax" -> com.ndev.moodyroutine.R.drawable.ic_mode_relax
            "work", "business" -> com.ndev.moodyroutine.R.drawable.ic_mode_work
            "game", "gaming" -> com.ndev.moodyroutine.R.drawable.ic_mode_game
            "theater", "movie" -> com.ndev.moodyroutine.R.drawable.ic_mode_movie
            "music" -> com.ndev.moodyroutine.R.drawable.ic_mode_music
            "book", "reading" -> com.ndev.moodyroutine.R.drawable.ic_mode_book
            "school", "study" -> com.ndev.moodyroutine.R.drawable.ic_mode_school
            "code", "coding", "dev" -> com.ndev.moodyroutine.R.drawable.ic_mode_code
            "home", "house" -> com.ndev.moodyroutine.R.drawable.ic_mode_home
            "coffee", "cafe" -> com.ndev.moodyroutine.R.drawable.ic_mode_coffee
            "restaurant", "food", "dining" -> com.ndev.moodyroutine.R.drawable.ic_mode_restaurant
            "flight", "travel", "plane" -> com.ndev.moodyroutine.R.drawable.ic_mode_flight
            "park", "nature", "outdoor" -> com.ndev.moodyroutine.R.drawable.ic_mode_park
            "shopping", "cart", "store" -> com.ndev.moodyroutine.R.drawable.ic_mode_shopping
            "call", "meeting", "phone" -> com.ndev.moodyroutine.R.drawable.ic_mode_call
            "groups", "social", "team" -> com.ndev.moodyroutine.R.drawable.ic_mode_groups
            "focus", "target" -> com.ndev.moodyroutine.R.drawable.ic_mode_focus
            "run", "running", "walk" -> com.ndev.moodyroutine.R.drawable.ic_mode_run
            "bike", "cycling" -> com.ndev.moodyroutine.R.drawable.ic_mode_bike
            "cleaning", "clean" -> com.ndev.moodyroutine.R.drawable.ic_mode_cleaning
            "health", "hospital", "medical" -> com.ndev.moodyroutine.R.drawable.ic_mode_health
            "meditation" -> com.ndev.moodyroutine.R.drawable.ic_mode_meditation
            "headphones", "audio" -> com.ndev.moodyroutine.R.drawable.ic_mode_headphones
            "sunny", "morning", "day" -> com.ndev.moodyroutine.R.drawable.ic_mode_sunny
            "shield", "security" -> com.ndev.moodyroutine.R.drawable.ic_mode_shield
            "palette", "art" -> com.ndev.moodyroutine.R.drawable.ic_mode_palette
            else -> com.ndev.moodyroutine.R.drawable.ic_mode_custom
        }
    }

    fun getModeLargeIconBitmap(context: android.content.Context, iconName: String, colorHex: String): android.graphics.Bitmap? {
        return try {
            val drawableId = getModeDrawableRes(iconName)
            val drawable = androidx.core.content.ContextCompat.getDrawable(context, drawableId) ?: return null
            val density = context.resources.displayMetrics.density
            val sizePx = (56 * density).toInt().coerceAtLeast(112)
            val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)

            val color = try {
                android.graphics.Color.parseColor(colorHex)
            } catch (_: Exception) {
                0xFF3B82F6.toInt()
            }

            val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = android.graphics.Paint.Style.FILL
            }
            canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, bgPaint)

            drawable.setTint(android.graphics.Color.WHITE)
            val padding = (sizePx * 0.22f).toInt()
            drawable.setBounds(padding, padding, sizePx - padding, sizePx - padding)
            drawable.draw(canvas)

            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
