package com.ndev.moodyroutine.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ndev.moodyroutine.data.db.MoodyRoutineDatabase
import com.ndev.moodyroutine.data.repository.ModeRepository
import com.ndev.moodyroutine.data.repository.RoutineRepository
import com.ndev.moodyroutine.engine.ActionExecutor
import com.ndev.moodyroutine.engine.AutomationEngine
import com.ndev.moodyroutine.engine.AutomationEvent
import com.ndev.moodyroutine.engine.ConditionEvaluator
import com.ndev.moodyroutine.engine.EventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutomationService : Service() {
    private var engine: AutomationEngine? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isReceiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            scope.launch {
                when (intent.action) {
                    Intent.ACTION_BATTERY_CHANGED -> {
                        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                        EventBus.emit(AutomationEvent.BatteryEvent(level, isCharging))
                    }
                    Intent.ACTION_POWER_CONNECTED -> EventBus.emit(AutomationEvent.PowerEvent(true))
                    Intent.ACTION_POWER_DISCONNECTED -> EventBus.emit(AutomationEvent.PowerEvent(false))
                    Intent.ACTION_HEADSET_PLUG -> {
                        val state = intent.getIntExtra("state", 0)
                        EventBus.emit(AutomationEvent.HeadphoneEvent(state == 1))
                    }
                    Intent.ACTION_SCREEN_ON -> EventBus.emit(AutomationEvent.ScreenEvent(true))
                    Intent.ACTION_SCREEN_OFF -> EventBus.emit(AutomationEvent.ScreenEvent(false))
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("MoodyRoutine", "AutomationService created")
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "moody_routine_service")
            .setContentTitle("MoodyRoutine is active")
            .setContentText("Monitoring for automated triggers")
            .setSmallIcon(android.R.drawable.ic_menu_preferences)
            .build()
        
        startForeground(1, notification)
        
        val db = MoodyRoutineDatabase.getInstance(this)
        // Assuming implementations exist. If not, this might need adjustments based on actual codebase.
        val routineRepo = RoutineRepository(db.routineDao())
        val modeRepo = ModeRepository(db.modeDao())
        
        engine = AutomationEngine(
            routineRepository = routineRepo,
            modeRepository = modeRepo,
            conditionEvaluator = ConditionEvaluator(),
            actionExecutor = ActionExecutor(this)
        )
        engine?.start()
        
        registerReceivers()
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(receiver, filter)
        isReceiverRegistered = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        engine?.stop()
        if (isReceiverRegistered) {
            unregisterReceiver(receiver)
            isReceiverRegistered = false
        }
        Log.i("MoodyRoutine", "AutomationService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "moody_routine_service",
                "Automation Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
