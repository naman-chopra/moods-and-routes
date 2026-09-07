package com.ndev.moodyroutine.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.ndev.moodyroutine.data.model.Mode
import com.ndev.moodyroutine.data.model.Routine
import com.ndev.moodyroutine.data.model.TriggerType

class GeofenceManager(private val context: Context) {
    private val client = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    @SuppressLint("MissingPermission")
    fun updateGeofences(routines: List<Routine>, modes: List<Mode>) {
        val geofences = mutableListOf<Geofence>()

        for (routine in routines) {
            if (!routine.isEnabled) continue
            for ((index, trigger) in routine.triggers.withIndex()) {
                if (trigger.type == TriggerType.LOCATION_ARRIVE || trigger.type == TriggerType.LOCATION_LEAVE) {
                    val lat = trigger.params["latitude"]?.toDoubleOrNull()
                    val lng = trigger.params["longitude"]?.toDoubleOrNull()
                    val radius = trigger.params["radius"]?.toFloatOrNull() ?: 150f
                    val name = trigger.params["locationName"] ?: "Routine_${routine.id}"

                    if (lat != null && lng != null) {
                        geofences.add(
                            Geofence.Builder()
                                .setRequestId(name)
                                .setCircularRegion(lat, lng, radius)
                                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                                .build()
                        )
                    }
                }
            }
        }

        for (mode in modes) {
            for ((index, trigger) in mode.autoTriggers.withIndex()) {
                if (trigger.type == TriggerType.LOCATION_ARRIVE || trigger.type == TriggerType.LOCATION_LEAVE) {
                    val lat = trigger.params["latitude"]?.toDoubleOrNull()
                    val lng = trigger.params["longitude"]?.toDoubleOrNull()
                    val radius = trigger.params["radius"]?.toFloatOrNull() ?: 150f
                    val name = trigger.params["locationName"] ?: mode.name

                    if (lat != null && lng != null) {
                        geofences.add(
                            Geofence.Builder()
                                .setRequestId(name)
                                .setCircularRegion(lat, lng, radius)
                                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                                .build()
                        )
                    }
                }
            }
        }

        if (geofences.isNotEmpty()) {
            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofences)
                .build()

            try {
                client.addGeofences(request, geofencePendingIntent).run {
                    addOnSuccessListener {
                        Log.i("MoodyRoutine", "Successfully registered ${geofences.size} Play Services geofences")
                    }
                    addOnFailureListener { e ->
                        Log.w("MoodyRoutine", "Failed to add Play Services geofences (LocationTracker active as fallback): ${e.message}")
                    }
                }
            } catch (e: SecurityException) {
                Log.w("MoodyRoutine", "Missing background location for Play Services geofences (LocationTracker active as fallback): ${e.message}")
            }
        } else {
            try {
                client.removeGeofences(geofencePendingIntent)
            } catch (_: Exception) {}
        }
    }
}
