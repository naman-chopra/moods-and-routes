package com.ndev.moodyroutine.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.ndev.moodyroutine.engine.AutomationEvent
import com.ndev.moodyroutine.engine.EventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent ?: return
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) {
            Log.e("MoodyRoutine", "Geofence error: ${geofencingEvent.errorCode}")
            return
        }

        val transition = geofencingEvent.geofenceTransition
        val isEntering = transition == Geofence.GEOFENCE_TRANSITION_ENTER
        val isExiting = transition == Geofence.GEOFENCE_TRANSITION_EXIT

        if (isEntering || isExiting) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences ?: emptyList()
            for (geofence in triggeringGeofences) {
                val requestId = geofence.requestId
                Log.i("MoodyRoutine", "Geofence triggered: $requestId, entering=$isEntering")
                CoroutineScope(Dispatchers.IO).launch {
                    EventBus.emit(
                        AutomationEvent.LocationEvent(
                            locationName = requestId,
                            isEntering = isEntering,
                            latitude = geofencingEvent.triggeringLocation?.latitude,
                            longitude = geofencingEvent.triggeringLocation?.longitude
                        )
                    )
                }
            }
        }
    }
}
