package com.ndev.moodyroutine.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.UiModeManager
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import android.content.SharedPreferences
import com.ndev.moodyroutine.data.model.ActionConfig
import com.ndev.moodyroutine.data.model.ActionType
import com.ndev.moodyroutine.data.model.Mode
import com.ndev.moodyroutine.data.model.Routine
import com.ndev.moodyroutine.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ActionExecutor(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    private val prefs: SharedPreferences =
        context.getSharedPreferences("moody_mode_snapshots", Context.MODE_PRIVATE)

    suspend fun executeMode(mode: Mode) {
        withContext(Dispatchers.IO) {
            snapshotState("mode_${mode.id}_", mode.actions, mode.name)
            executeAll(mode.actions)
        }
    }

    suspend fun revertMode(mode: Mode) {
        withContext(Dispatchers.IO) {
            restoreState("mode_${mode.id}_", mode.actions, mode.name)
        }
    }

    suspend fun executeRoutine(routine: Routine) {
        withContext(Dispatchers.IO) {
            snapshotState("routine_${routine.id}_", routine.actions, routine.name)
            executeAll(routine.actions)
        }
    }

    suspend fun revertRoutine(routine: Routine) {
        withContext(Dispatchers.IO) {
            restoreState("routine_${routine.id}_", routine.actions, routine.name)
        }
    }

    fun hasRoutineSnapshot(routineId: Long): Boolean {
        return prefs.getBoolean("routine_${routineId}_has_snapshot", false)
    }

    private fun snapshotState(prefix: String, actions: List<ActionConfig>, entityName: String) {
        if (prefs.getBoolean("${prefix}has_snapshot", false)) {
            AppLogger.d("ActionExecutor", "$entityName already has saved snapshot, retaining original state")
            return
        }

        val editor = prefs.edit()
        var savedSomething = false

        for (action in actions) {
            when (action.type) {
                ActionType.SET_RINGER_NORMAL,
                ActionType.SET_RINGER_VIBRATE,
                ActionType.SET_RINGER_SILENT -> {
                    if (!prefs.contains("${prefix}ringer_mode")) {
                        val currentRinger = audioManager.ringerMode
                        editor.putInt("${prefix}ringer_mode", currentRinger)
                        savedSomething = true
                        AppLogger.i("ActionExecutor", "Snapshotted ringer mode: $currentRinger for $entityName")
                    }
                }
                ActionType.SET_VOLUME_RING -> {
                    if (!prefs.contains("${prefix}volume_ring")) {
                        editor.putInt("${prefix}volume_ring", audioManager.getStreamVolume(AudioManager.STREAM_RING))
                        savedSomething = true
                    }
                }
                ActionType.SET_VOLUME_MEDIA -> {
                    if (!prefs.contains("${prefix}volume_media")) {
                        editor.putInt("${prefix}volume_media", audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
                        savedSomething = true
                    }
                }
                ActionType.SET_VOLUME_ALARM -> {
                    if (!prefs.contains("${prefix}volume_alarm")) {
                        editor.putInt("${prefix}volume_alarm", audioManager.getStreamVolume(AudioManager.STREAM_ALARM))
                        savedSomething = true
                    }
                }
                ActionType.SET_VOLUME_NOTIFICATION -> {
                    if (!prefs.contains("${prefix}volume_notif")) {
                        editor.putInt("${prefix}volume_notif", audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION))
                        savedSomething = true
                    }
                }
                ActionType.SET_DND_ON,
                ActionType.SET_DND_OFF,
                ActionType.SET_DND_PRIORITY_ONLY,
                ActionType.SET_DND_ALARMS_ONLY -> {
                    if (!prefs.contains("${prefix}dnd_filter")) {
                        if (notificationManager.isNotificationPolicyAccessGranted) {
                            editor.putInt("${prefix}dnd_filter", notificationManager.currentInterruptionFilter)
                            savedSomething = true
                            AppLogger.i("ActionExecutor", "Snapshotted DND filter: ${notificationManager.currentInterruptionFilter} for $entityName")
                        }
                    }
                }
                ActionType.SET_BRIGHTNESS -> {
                    if (!prefs.contains("${prefix}brightness")) {
                        try {
                            val current = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                            editor.putInt("${prefix}brightness", current)
                            savedSomething = true
                            AppLogger.i("ActionExecutor", "Snapshotted brightness: $current for $entityName")
                        } catch (e: Exception) {
                            AppLogger.w("ActionExecutor", "Could not snapshot brightness", e)
                        }
                    }
                }
                ActionType.TOGGLE_AUTO_ROTATE_ON,
                ActionType.TOGGLE_AUTO_ROTATE_OFF -> {
                    if (!prefs.contains("${prefix}auto_rotate")) {
                        try {
                            val current = Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION)
                            editor.putInt("${prefix}auto_rotate", current)
                            savedSomething = true
                            AppLogger.i("ActionExecutor", "Snapshotted auto-rotate: $current for $entityName")
                        } catch (e: Exception) {
                            AppLogger.w("ActionExecutor", "Could not snapshot auto-rotate", e)
                        }
                    }
                }
                ActionType.TOGGLE_FLASHLIGHT_ON -> {
                    editor.putBoolean("${prefix}flashlight_on", true)
                    savedSomething = true
                }
                ActionType.ENABLE_DARK_MODE,
                ActionType.DISABLE_DARK_MODE -> {
                    if (!prefs.contains("${prefix}dark_mode")) {
                        editor.putInt("${prefix}dark_mode", uiModeManager.nightMode)
                        savedSomething = true
                    }
                }
                else -> {}
            }
        }

        if (savedSomething) {
            editor.putBoolean("${prefix}has_snapshot", true)
            editor.apply()
            AppLogger.i("ActionExecutor", "Saved pre-execution snapshot for: $entityName")
        }
    }

    private fun restoreState(prefix: String, actions: List<ActionConfig>, entityName: String) {
        val hasSnapshot = prefs.getBoolean("${prefix}has_snapshot", false)
        AppLogger.i("ActionExecutor", "Reverting actions for: $entityName (hasSnapshot=$hasSnapshot)")

        if (hasSnapshot) {
            if (prefs.contains("${prefix}ringer_mode")) {
                val ringer = prefs.getInt("${prefix}ringer_mode", AudioManager.RINGER_MODE_NORMAL)
                try {
                    audioManager.ringerMode = ringer
                    AppLogger.i("ActionExecutor", "Reverted ringer mode to $ringer for $entityName")
                } catch (e: Exception) {
                    AppLogger.e("ActionExecutor", "Failed to restore ringer mode", e)
                }
            }

            if (prefs.contains("${prefix}volume_ring")) {
                val vol = prefs.getInt("${prefix}volume_ring", -1)
                if (vol >= 0) safeSetStreamVolume(AudioManager.STREAM_RING, vol)
            }
            if (prefs.contains("${prefix}volume_media")) {
                val vol = prefs.getInt("${prefix}volume_media", -1)
                if (vol >= 0) safeSetStreamVolume(AudioManager.STREAM_MUSIC, vol)
            }
            if (prefs.contains("${prefix}volume_alarm")) {
                val vol = prefs.getInt("${prefix}volume_alarm", -1)
                if (vol >= 0) safeSetStreamVolume(AudioManager.STREAM_ALARM, vol)
            }
            if (prefs.contains("${prefix}volume_notif")) {
                val vol = prefs.getInt("${prefix}volume_notif", -1)
                if (vol >= 0) safeSetStreamVolume(AudioManager.STREAM_NOTIFICATION, vol)
            }

            if (prefs.contains("${prefix}dnd_filter")) {
                val filter = prefs.getInt("${prefix}dnd_filter", NotificationManager.INTERRUPTION_FILTER_ALL)
                setDndMode(filter)
                AppLogger.i("ActionExecutor", "Reverted DND filter to $filter for $entityName")
            }

            if (prefs.contains("${prefix}brightness")) {
                val brightness = prefs.getInt("${prefix}brightness", -1)
                if (brightness >= 0 && Settings.System.canWrite(context)) {
                    try {
                        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
                        AppLogger.i("ActionExecutor", "Reverted brightness to $brightness for $entityName")
                    } catch (e: Exception) {
                        AppLogger.e("ActionExecutor", "Could not restore brightness", e)
                    }
                }
            }

            if (prefs.contains("${prefix}auto_rotate")) {
                val rotate = prefs.getInt("${prefix}auto_rotate", -1)
                if (rotate >= 0 && Settings.System.canWrite(context)) {
                    try {
                        Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, rotate)
                        AppLogger.i("ActionExecutor", "Reverted auto-rotate to $rotate for $entityName")
                    } catch (e: Exception) {
                        AppLogger.e("ActionExecutor", "Could not restore auto-rotate", e)
                    }
                }
            }

            if (prefs.getBoolean("${prefix}flashlight_on", false)) {
                toggleFlashlight(false)
                AppLogger.i("ActionExecutor", "Reverted flashlight to OFF for $entityName")
            }

            if (prefs.contains("${prefix}dark_mode")) {
                val nightMode = prefs.getInt("${prefix}dark_mode", UiModeManager.MODE_NIGHT_AUTO)
                try {
                    uiModeManager.nightMode = nightMode
                    AppLogger.i("ActionExecutor", "Reverted dark mode to $nightMode for $entityName")
                } catch (e: Exception) {
                    AppLogger.e("ActionExecutor", "Could not restore dark mode", e)
                }
            }

            // Clear snapshot
            prefs.edit()
                .remove("${prefix}has_snapshot")
                .remove("${prefix}ringer_mode")
                .remove("${prefix}volume_ring")
                .remove("${prefix}volume_media")
                .remove("${prefix}volume_alarm")
                .remove("${prefix}volume_notif")
                .remove("${prefix}dnd_filter")
                .remove("${prefix}brightness")
                .remove("${prefix}auto_rotate")
                .remove("${prefix}flashlight_on")
                .remove("${prefix}dark_mode")
                .apply()
        } else {
            // Intelligent fallback if no snapshot was recorded
            for (action in actions) {
                when (action.type) {
                    ActionType.SET_RINGER_SILENT,
                    ActionType.SET_RINGER_VIBRATE -> {
                        try {
                            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                            AppLogger.i("ActionExecutor", "Fallback: restored ringer to NORMAL for $entityName")
                        } catch (e: Exception) {
                            AppLogger.e("ActionExecutor", "Fallback: failed to restore ringer mode", e)
                        }
                    }
                    ActionType.SET_DND_ON,
                    ActionType.SET_DND_PRIORITY_ONLY,
                    ActionType.SET_DND_ALARMS_ONLY -> {
                        setDndMode(NotificationManager.INTERRUPTION_FILTER_ALL)
                        AppLogger.i("ActionExecutor", "Fallback: restored DND to OFF for $entityName")
                    }
                    ActionType.TOGGLE_FLASHLIGHT_ON -> {
                        toggleFlashlight(false)
                    }
                    else -> {}
                }
            }
        }
    }

    suspend fun executeAll(actions: List<ActionConfig>) {
        withContext(Dispatchers.IO) {
            for (action in actions) {
                try {
                    if (action.type == ActionType.WAIT_DELAY) {
                        val sec = action.params["seconds"]?.toLongOrNull() ?: 5L
                        AppLogger.i("ActionExecutor", "Waiting $sec seconds before next action...")
                        kotlinx.coroutines.delay(sec * 1000L)
                        continue
                    }
                    executeAction(action)
                } catch (e: Exception) {
                    AppLogger.e("ActionExecutor", "Failed to execute action: $action", e)
                }
            }
        }
    }

    private fun executeAction(action: ActionConfig) {
        AppLogger.i("ActionExecutor", "Executing action: ${action.type}")
        when (action.type) {
            ActionType.SET_VOLUME_RING -> setVolume(AudioManager.STREAM_RING, action.params["volume"])
            ActionType.SET_VOLUME_MEDIA -> setVolume(AudioManager.STREAM_MUSIC, action.params["volume"])
            ActionType.SET_VOLUME_ALARM -> setVolume(AudioManager.STREAM_ALARM, action.params["volume"])
            ActionType.SET_VOLUME_NOTIFICATION -> setVolume(AudioManager.STREAM_NOTIFICATION, action.params["volume"])

            ActionType.SET_DND_ON -> setDndMode(NotificationManager.INTERRUPTION_FILTER_NONE)
            ActionType.SET_DND_OFF -> setDndMode(NotificationManager.INTERRUPTION_FILTER_ALL)
            ActionType.SET_DND_PRIORITY_ONLY -> setDndMode(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            ActionType.SET_DND_ALARMS_ONLY -> setDndMode(NotificationManager.INTERRUPTION_FILTER_ALARMS)

            ActionType.SET_BRIGHTNESS -> {
                if (Settings.System.canWrite(context)) {
                    val brightnessPercent = action.params["brightness"]?.toIntOrNull() ?: return
                    val brightness = ((brightnessPercent * 255) / 100).coerceIn(0, 255)
                    Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
                    AppLogger.i("ActionExecutor", "Set brightness to $brightnessPercent% ($brightness/255)")
                } else {
                    AppLogger.w("ActionExecutor", "WRITE_SETTINGS permission required to set brightness")
                }
            }
            ActionType.TOGGLE_AUTO_ROTATE_ON -> {
                if (Settings.System.canWrite(context)) {
                    Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)
                    AppLogger.i("ActionExecutor", "Auto-rotate turned ON")
                } else {
                    AppLogger.w("ActionExecutor", "WRITE_SETTINGS permission required for auto-rotate")
                }
            }
            ActionType.TOGGLE_AUTO_ROTATE_OFF -> {
                if (Settings.System.canWrite(context)) {
                    Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
                    AppLogger.i("ActionExecutor", "Auto-rotate turned OFF")
                } else {
                    AppLogger.w("ActionExecutor", "WRITE_SETTINGS permission required for auto-rotate")
                }
            }

            ActionType.OPEN_APP -> {
                val shortcutUri = action.params["shortcutUri"]
                if (!shortcutUri.isNullOrBlank()) {
                    try {
                        val intent = Intent.parseUri(shortcutUri, Intent.URI_INTENT_SCHEME)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        AppLogger.i("ActionExecutor", "Launched shortcut: ${action.params["shortcutName"]} ($shortcutUri)")
                        return
                    } catch (e: Exception) {
                        AppLogger.e("ActionExecutor", "Failed to launch shortcut URI", e)
                    }
                }
                val packageName = action.params["packageName"] ?: return
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    AppLogger.i("ActionExecutor", "Launched app: $packageName")
                } else {
                    AppLogger.w("ActionExecutor", "Could not find launch intent for $packageName")
                }
            }
            ActionType.CLOSE_APP -> {
                // Return to Android home screen
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(homeIntent)
                AppLogger.i("ActionExecutor", "Close app action: Returned to home screen")
            }
            ActionType.RESTRICT_APPS -> {
                AppLogger.i("ActionExecutor", "Restricted apps action registered: ${action.params["appCount"]} apps")
            }
            ActionType.SET_WALLPAPER,
            ActionType.SET_HOME_WALLPAPER,
            ActionType.SET_LOCK_WALLPAPER -> {
                val wallpaperUriStr = action.params["wallpaperUri"]
                val wallpaperManager = WallpaperManager.getInstance(context)
                if (!wallpaperUriStr.isNullOrBlank()) {
                    try {
                        val uri = android.net.Uri.parse(wallpaperUriStr)
                        val inputStream = context.contentResolver.openInputStream(uri)
                        if (inputStream != null) {
                            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                            inputStream.close()
                            if (bitmap != null) {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                    val which = when (action.type) {
                                        ActionType.SET_HOME_WALLPAPER -> WallpaperManager.FLAG_SYSTEM
                                        ActionType.SET_LOCK_WALLPAPER -> WallpaperManager.FLAG_LOCK
                                        else -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                                    }
                                    wallpaperManager.setBitmap(bitmap, null, true, which)
                                } else {
                                    wallpaperManager.setBitmap(bitmap)
                                }
                                AppLogger.i("ActionExecutor", "Applied wallpaper for ${action.type}")
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.e("ActionExecutor", "Failed to set wallpaper", e)
                    }
                } else {
                    try {
                        wallpaperManager.clear()
                        AppLogger.i("ActionExecutor", "Reset wallpaper")
                    } catch (e: Exception) {
                        AppLogger.w("ActionExecutor", "Could not reset wallpaper", e)
                    }
                }
            }
            ActionType.WAIT_DELAY -> {
                // Handled in executeAll suspend loop
            }

            ActionType.SET_RINGER_NORMAL -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                AppLogger.i("ActionExecutor", "Ringer mode set to NORMAL")
            }
            ActionType.SET_RINGER_VIBRATE -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                AppLogger.i("ActionExecutor", "Ringer mode set to VIBRATE")
            }
            ActionType.SET_RINGER_SILENT -> {
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    AppLogger.i("ActionExecutor", "Ringer mode set to SILENT")
                } else {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    AppLogger.w("ActionExecutor", "DND access required for SILENT mode, fell back to VIBRATE")
                }
            }

            ActionType.TOGGLE_FLASHLIGHT_ON -> toggleFlashlight(true)
            ActionType.TOGGLE_FLASHLIGHT_OFF -> toggleFlashlight(false)

            ActionType.SEND_NOTIFICATION -> sendNotification(action.params["title"], action.params["message"])

            ActionType.ENABLE_DARK_MODE -> {
                uiModeManager.nightMode = UiModeManager.MODE_NIGHT_YES
                AppLogger.i("ActionExecutor", "Dark mode ENABLED")
            }
            ActionType.DISABLE_DARK_MODE -> {
                uiModeManager.nightMode = UiModeManager.MODE_NIGHT_NO
                AppLogger.i("ActionExecutor", "Dark mode DISABLED")
            }
        }
    }

    private fun safeSetStreamVolume(streamType: Int, targetVolume: Int) {
        try {
            audioManager.setStreamVolume(streamType, targetVolume, 0)
            AppLogger.i("ActionExecutor", "Set stream $streamType volume to $targetVolume")
        } catch (e: SecurityException) {
            AppLogger.w("ActionExecutor", "Could not set stream $streamType volume (requires DND/Notification Policy access): ${e.message}")
        } catch (e: Exception) {
            AppLogger.e("ActionExecutor", "Failed to set stream $streamType volume", e)
        }
    }

    private fun setVolume(streamType: Int, volumeStr: String?) {
        val volumePercent = volumeStr?.toIntOrNull() ?: return
        val maxVolume = audioManager.getStreamMaxVolume(streamType)
        val targetVolume = ((maxVolume * volumePercent) / 100).coerceIn(0, maxVolume)
        safeSetStreamVolume(streamType, targetVolume)
    }

    private fun setDndMode(filter: Int) {
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(filter)
            AppLogger.i("ActionExecutor", "DND mode filter set to $filter")
        } else {
            AppLogger.w("ActionExecutor", "Missing DND permission (Notification Policy Access)")
        }
    }

    private fun toggleFlashlight(state: Boolean) {
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            cameraManager.setTorchMode(cameraId, state)
            AppLogger.i("ActionExecutor", "Torch mode set to $state")
        } catch (e: Exception) {
            AppLogger.e("ActionExecutor", "Error toggling flashlight", e)
        }
    }

    private fun sendNotification(title: String?, message: String?) {
        val channelId = "moody_routine_actions"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Routine Actions", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title?.ifBlank { "MoodyRoutine" } ?: "MoodyRoutine")
            .setContentText(message?.ifBlank { "Action executed" } ?: "Action executed")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        val notifId = (System.currentTimeMillis() % 100000).toInt()
        notificationManager.notify(notifId, builder.build())
        AppLogger.i("ActionExecutor", "Posted notification: title='$title', message='$message'")
    }
}
