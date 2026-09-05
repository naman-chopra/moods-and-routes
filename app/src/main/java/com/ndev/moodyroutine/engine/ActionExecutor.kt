package com.ndev.moodyroutine.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.UiModeManager
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
                    Log.e("MoodyRoutine", "Failed to execute action: \$action", e)
                }
            }
        }
    }

    private fun executeAction(action: ActionConfig) {
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
                val brightnessPercent = action.params["brightness"]?.toIntOrNull() ?: return
                val brightness = (brightnessPercent * 255) / 100
                Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
            }
            ActionType.OPEN_APP -> {
                val packageName = action.params["packageName"] ?: return
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                intent?.let { 
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it) 
                }
            }
            ActionType.SET_RINGER_NORMAL -> audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            ActionType.SET_RINGER_VIBRATE -> audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            ActionType.SET_RINGER_SILENT -> audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            
            ActionType.TOGGLE_AUTO_ROTATE_ON -> {
                Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)
            }
            ActionType.TOGGLE_AUTO_ROTATE_OFF -> {
                Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
            }
            
            ActionType.TOGGLE_FLASHLIGHT_ON -> toggleFlashlight(true)
            ActionType.TOGGLE_FLASHLIGHT_OFF -> toggleFlashlight(false)
            
            ActionType.SEND_NOTIFICATION -> sendNotification(action.params["title"], action.params["message"])
            
            ActionType.ENABLE_DARK_MODE -> uiModeManager.nightMode = UiModeManager.MODE_NIGHT_YES
            ActionType.DISABLE_DARK_MODE -> uiModeManager.nightMode = UiModeManager.MODE_NIGHT_NO
            else -> {
                Log.w("MoodyRoutine", "Unknown action type: \${action.type}")
            }
        }
    }

    private fun setVolume(streamType: Int, volumeStr: String?) {
        val volumePercent = volumeStr?.toIntOrNull() ?: return
        val maxVolume = audioManager.getStreamMaxVolume(streamType)
        val targetVolume = (maxVolume * volumePercent) / 100
        audioManager.setStreamVolume(streamType, targetVolume, 0)
    }
    
    private fun setDndMode(filter: Int) {
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(filter)
        } else {
            Log.w("MoodyRoutine", "Missing DND permission")
        }
    }

    private fun toggleFlashlight(state: Boolean) {
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            cameraManager.setTorchMode(cameraId, state)
        } catch (e: Exception) {
            Log.e("MoodyRoutine", "Error toggling flashlight", e)
        }
    }

    private fun sendNotification(title: String?, message: String?) {
        val channelId = "moody_routine_actions"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Routine Actions", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title ?: "MoodyRoutine")
            .setContentText(message ?: "Action executed")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
