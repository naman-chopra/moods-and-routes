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
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ndev.moodyroutine.data.model.ActionConfig
import com.ndev.moodyroutine.data.model.ActionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ActionExecutor(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager

    suspend fun executeAll(actions: List<ActionConfig>) {
        withContext(Dispatchers.IO) {
            for (action in actions) {
                try {
                    executeAction(action)
                } catch (e: Exception) {
                    Log.e("MoodyRoutine", "Failed to execute action: $action", e)
                }
            }
        }
    }

    private fun executeAction(action: ActionConfig) {
        Log.i("MoodyRoutine", "Executing action: ${action.type}")
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
                    Log.i("MoodyRoutine", "Set brightness to $brightnessPercent% ($brightness/255)")
                } else {
                    Log.w("MoodyRoutine", "WRITE_SETTINGS permission required to set brightness")
                }
            }
            ActionType.TOGGLE_AUTO_ROTATE_ON -> {
                if (Settings.System.canWrite(context)) {
                    Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)
                    Log.i("MoodyRoutine", "Auto-rotate turned ON")
                } else {
                    Log.w("MoodyRoutine", "WRITE_SETTINGS permission required for auto-rotate")
                }
            }
            ActionType.TOGGLE_AUTO_ROTATE_OFF -> {
                if (Settings.System.canWrite(context)) {
                    Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
                    Log.i("MoodyRoutine", "Auto-rotate turned OFF")
                } else {
                    Log.w("MoodyRoutine", "WRITE_SETTINGS permission required for auto-rotate")
                }
            }

            ActionType.OPEN_APP -> {
                val packageName = action.params["packageName"] ?: return
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    Log.i("MoodyRoutine", "Launched app: $packageName")
                } else {
                    Log.w("MoodyRoutine", "Could not find launch intent for $packageName")
                }
            }
            ActionType.CLOSE_APP -> {
                // Return to Android home screen
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(homeIntent)
                Log.i("MoodyRoutine", "Close app action: Returned to home screen")
            }
            ActionType.SET_WALLPAPER -> {
                try {
                    val wallpaperManager = WallpaperManager.getInstance(context)
                    wallpaperManager.clear()
                    Log.i("MoodyRoutine", "Reset wallpaper")
                } catch (e: Exception) {
                    Log.w("MoodyRoutine", "Could not reset wallpaper", e)
                }
            }

            ActionType.SET_RINGER_NORMAL -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                Log.i("MoodyRoutine", "Ringer mode set to NORMAL")
            }
            ActionType.SET_RINGER_VIBRATE -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                Log.i("MoodyRoutine", "Ringer mode set to VIBRATE")
            }
            ActionType.SET_RINGER_SILENT -> {
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    Log.i("MoodyRoutine", "Ringer mode set to SILENT")
                } else {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    Log.w("MoodyRoutine", "DND access required for SILENT mode, fell back to VIBRATE")
                }
            }

            ActionType.TOGGLE_FLASHLIGHT_ON -> toggleFlashlight(true)
            ActionType.TOGGLE_FLASHLIGHT_OFF -> toggleFlashlight(false)

            ActionType.SEND_NOTIFICATION -> sendNotification(action.params["title"], action.params["message"])

            ActionType.ENABLE_DARK_MODE -> {
                uiModeManager.nightMode = UiModeManager.MODE_NIGHT_YES
                Log.i("MoodyRoutine", "Dark mode ENABLED")
            }
            ActionType.DISABLE_DARK_MODE -> {
                uiModeManager.nightMode = UiModeManager.MODE_NIGHT_NO
                Log.i("MoodyRoutine", "Dark mode DISABLED")
            }
        }
    }

    private fun setVolume(streamType: Int, volumeStr: String?) {
        val volumePercent = volumeStr?.toIntOrNull() ?: return
        val maxVolume = audioManager.getStreamMaxVolume(streamType)
        val targetVolume = ((maxVolume * volumePercent) / 100).coerceIn(0, maxVolume)
        try {
            audioManager.setStreamVolume(streamType, targetVolume, 0)
            Log.i("MoodyRoutine", "Set stream $streamType volume to $volumePercent% ($targetVolume/$maxVolume)")
        } catch (e: SecurityException) {
            Log.w("MoodyRoutine", "Could not set stream $streamType volume (requires DND access)", e)
        }
    }

    private fun setDndMode(filter: Int) {
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(filter)
            Log.i("MoodyRoutine", "DND mode filter set to $filter")
        } else {
            Log.w("MoodyRoutine", "Missing DND permission (Notification Policy Access)")
        }
    }

    private fun toggleFlashlight(state: Boolean) {
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            cameraManager.setTorchMode(cameraId, state)
            Log.i("MoodyRoutine", "Torch mode set to $state")
        } catch (e: Exception) {
            Log.e("MoodyRoutine", "Error toggling flashlight", e)
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
        Log.i("MoodyRoutine", "Posted notification: title='$title', message='$message'")
    }
}
