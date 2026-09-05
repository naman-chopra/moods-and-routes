package com.ndev.moodyroutine.data.model

enum class ActionCategory {
    SOUND, DISPLAY, APPS, DEVICE, NOTIFICATION
}

enum class ActionType(
    val displayName: String,
    val description: String,
    val iconName: String,
    val category: ActionCategory,
    val requiresValue: Boolean
) {
    SET_VOLUME_RING("Set ring volume", "Change the ring volume", "VolumeUp", ActionCategory.SOUND, true),
    SET_VOLUME_MEDIA("Set media volume", "Change the media volume", "MusicNote", ActionCategory.SOUND, true),
    SET_VOLUME_ALARM("Set alarm volume", "Change the alarm volume", "Alarm", ActionCategory.SOUND, true),
    SET_VOLUME_NOTIFICATION("Set notification volume", "Change the notification volume", "Notifications", ActionCategory.SOUND, true),
    SET_DND_ON("Turn on Do Not Disturb", "Enable Do Not Disturb mode", "DoNotDisturbOn", ActionCategory.SOUND, false),
    SET_DND_OFF("Turn off Do Not Disturb", "Disable Do Not Disturb mode", "DoNotDisturbOff", ActionCategory.SOUND, false),
    SET_DND_PRIORITY_ONLY("Priority only DND", "Allow only priority notifications", "Stars", ActionCategory.SOUND, false),
    SET_DND_ALARMS_ONLY("Alarms only DND", "Allow only alarms", "AlarmOn", ActionCategory.SOUND, false),
    SET_BRIGHTNESS("Set brightness", "Change screen brightness", "BrightnessHigh", ActionCategory.DISPLAY, true),
    OPEN_APP("Open app", "Launch a specific app", "Launch", ActionCategory.APPS, true),
    CLOSE_APP("Close app", "Close a specific app", "Close", ActionCategory.APPS, true),
    SET_WALLPAPER("Set wallpaper", "Change device wallpaper", "Wallpaper", ActionCategory.DISPLAY, true),
    TOGGLE_AUTO_ROTATE_ON("Turn on auto-rotate", "Enable screen auto-rotation", "ScreenRotation", ActionCategory.DISPLAY, false),
    TOGGLE_AUTO_ROTATE_OFF("Turn off auto-rotate", "Disable screen auto-rotation", "ScreenLockPortrait", ActionCategory.DISPLAY, false),
    TOGGLE_FLASHLIGHT_ON("Turn on flashlight", "Enable device flashlight", "FlashlightOn", ActionCategory.DEVICE, false),
    TOGGLE_FLASHLIGHT_OFF("Turn off flashlight", "Disable device flashlight", "FlashlightOff", ActionCategory.DEVICE, false),
    SET_RINGER_NORMAL("Normal mode", "Set ringer to normal", "NotificationsActive", ActionCategory.SOUND, false),
    SET_RINGER_VIBRATE("Vibrate mode", "Set ringer to vibrate", "Vibration", ActionCategory.SOUND, false),
    SET_RINGER_SILENT("Silent mode", "Set ringer to silent", "VolumeOff", ActionCategory.SOUND, false),
    SEND_NOTIFICATION("Send notification", "Display a custom notification", "Message", ActionCategory.NOTIFICATION, true),
    ENABLE_DARK_MODE("Enable dark mode", "Turn on system dark mode", "DarkMode", ActionCategory.DISPLAY, false),
    DISABLE_DARK_MODE("Disable dark mode", "Turn off system dark mode", "LightMode", ActionCategory.DISPLAY, false)
}
