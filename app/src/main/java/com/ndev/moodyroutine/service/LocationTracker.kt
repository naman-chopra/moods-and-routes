package com.ndev.moodyroutine.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import com.ndev.moodyroutine.util.AppLogger
import com.ndev.moodyroutine.data.model.Mode
import com.ndev.moodyroutine.data.model.Routine
import com.ndev.moodyroutine.data.model.TriggerType
import com.ndev.moodyroutine.engine.AutomationEvent
import com.ndev.moodyroutine.engine.EventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class LocationTarget(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val isArrive: Boolean
)

class LocationTracker(private val context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isTracking = false

    private val insideTargets = mutableSetOf<String>()
    private var targets = listOf<LocationTarget>()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            checkLocation(location)
        }
    }

    fun updateTargets(routines: List<Routine>, modes: List<Mode>) {
        val newTargets = mutableListOf<LocationTarget>()

        for (routine in routines) {
            if (!routine.isEnabled) continue
            for ((index, trigger) in routine.triggers.withIndex()) {
                if (trigger.type == TriggerType.LOCATION_ARRIVE || trigger.type == TriggerType.LOCATION_LEAVE) {
                    val lat = trigger.params["latitude"]?.toDoubleOrNull()
                    val lng = trigger.params["longitude"]?.toDoubleOrNull()
                    val rad = trigger.params["radius"]?.toFloatOrNull() ?: 150f
                    val name = trigger.params["locationName"] ?: "Routine_${routine.id}"
                    if (lat != null && lng != null) {
                        newTargets.add(
                            LocationTarget(
                                id = "routine_${routine.id}_$index",
                                name = name,
                                latitude = lat,
                                longitude = lng,
                                radiusMeters = rad,
                                isArrive = trigger.type == TriggerType.LOCATION_ARRIVE
                            )
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
                    val rad = trigger.params["radius"]?.toFloatOrNull() ?: 150f
                    val name = trigger.params["locationName"] ?: mode.name
                    if (lat != null && lng != null) {
                        newTargets.add(
                            LocationTarget(
                                id = "mode_${mode.id}_$index",
                                name = name,
                                latitude = lat,
                                longitude = lng,
                                radiusMeters = rad,
                                isArrive = trigger.type == TriggerType.LOCATION_ARRIVE
                            )
                        )
                    }
                }
            }
        }

        targets = newTargets
        AppLogger.i("LocationTracker", "LocationTracker targets updated: ${targets.size} active targets")

        if (targets.isNotEmpty() && !isTracking) {
            startTracking()
        } else if (targets.isEmpty() && isTracking) {
            stopTracking()
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (isTracking) return
        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                30_000L
            ).apply {
                setMinUpdateIntervalMillis(15_000L)
                setMaxUpdateDelayMillis(45_000L)
            }.build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            isTracking = true
            AppLogger.i("LocationTracker", "LocationTracker started updates (interval: 30s)")

            // Immediate check with last location
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) checkLocation(loc)
            }
        } catch (e: SecurityException) {
            AppLogger.e("LocationTracker", "Location permission missing in LocationTracker", e)
        }
    }

    fun stopTracking() {
        if (!isTracking) return
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isTracking = false
        AppLogger.i("LocationTracker", "LocationTracker stopped")
    }

    private fun checkLocation(currentLocation: Location) {
        val results = FloatArray(1)
        for (target in targets) {
            Location.distanceBetween(
                currentLocation.latitude,
                currentLocation.longitude,
                target.latitude,
                target.longitude,
                results
            )
            val distance = results[0]
            val isInside = distance <= target.radiusMeters
            val wasInside = insideTargets.contains(target.id)

            if (isInside && !wasInside) {
                insideTargets.add(target.id)
                AppLogger.i("LocationTracker", "LocationTracker: ENTERED ${target.name} (dist=${distance.toInt()}m <= ${target.radiusMeters.toInt()}m)")
                scope.launch {
                    EventBus.emit(
                        AutomationEvent.LocationEvent(
                            locationName = target.name,
                            isEntering = true,
                            latitude = currentLocation.latitude,
                            longitude = currentLocation.longitude
                        )
                    )
                }
            } else if (!isInside && wasInside) {
                insideTargets.remove(target.id)
                AppLogger.i("LocationTracker", "LocationTracker: EXITED ${target.name} (dist=${distance.toInt()}m > ${target.radiusMeters.toInt()}m)")
                scope.launch {
                    EventBus.emit(
                        AutomationEvent.LocationEvent(
                            locationName = target.name,
                            isEntering = false,
                            latitude = currentLocation.latitude,
                            longitude = currentLocation.longitude
                        )
                    )
                }
            }
        }
    }
}
