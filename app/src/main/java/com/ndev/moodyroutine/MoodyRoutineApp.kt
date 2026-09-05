package com.ndev.moodyroutine

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.ndev.moodyroutine.data.db.MoodyRoutineDatabase

class MoodyRoutineApp : Application() {

    val database: MoodyRoutineDatabase by lazy {
        MoodyRoutineDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val serviceChannel = NotificationChannel(
            CHANNEL_SERVICE,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }

        val alertChannel = NotificationChannel(
            CHANNEL_ALERTS,
            "Routine Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications from your routines"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(serviceChannel)
        notificationManager.createNotificationChannel(alertChannel)
    }

    companion object {
        const val CHANNEL_SERVICE = "automation_service"
        const val CHANNEL_ALERTS = "routine_alerts"
    }
}
