package com.ndev.moodyroutine.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ndev.moodyroutine.engine.AutomationEvent
import com.ndev.moodyroutine.engine.EventBus
import com.ndev.moodyroutine.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AppLogger.i("AlarmReceiver", "Alarm fired")
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        CoroutineScope(Dispatchers.IO).launch {
            EventBus.emit(AutomationEvent.TimeEvent(hour, minute, dayOfWeek))
        }
    }
}
