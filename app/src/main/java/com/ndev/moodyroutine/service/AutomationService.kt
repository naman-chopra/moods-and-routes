package com.ndev.moodyroutine.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import java.util.Calendar

class AutomationService : Service() {
    private var engine: AutomationEngine? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isReceiverRegistered = false
    private var locationTracker: LocationTracker? = null
    private var geofenceManager: GeofenceManager? = null
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var timeTickerJob: Job? = null
    private var appTrackerJob: Job? = null
    private var lastForegroundApp: String? = null

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
                    Intent.ACTION_POWER_CONNECTED -> {
                        Log.i("MoodyRoutine", "Power connected")
                        EventBus.emit(AutomationEvent.PowerEvent(true))
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        Log.i("MoodyRoutine", "Power disconnected")
                        EventBus.emit(AutomationEvent.PowerEvent(false))
                    }
                    Intent.ACTION_HEADSET_PLUG -> {
                        val state = intent.getIntExtra("state", 0)
                        Log.i("MoodyRoutine", "Headphones plug state: $state")
                        EventBus.emit(AutomationEvent.HeadphoneEvent(state == 1))
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        Log.d("MoodyRoutine", "Screen ON")
                        EventBus.emit(AutomationEvent.ScreenEvent(true))
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        Log.d("MoodyRoutine", "Screen OFF")
                        EventBus.emit(AutomationEvent.ScreenEvent(false))
                    }
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        val name = try { device?.name } catch (_: SecurityException) { null }
                        val address = device?.address
                        Log.i("MoodyRoutine", "Bluetooth device connected: $name ($address)")
                        EventBus.emit(AutomationEvent.BluetoothEvent(isConnected = true, deviceName = name, deviceAddress = address))
                    }
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        val name = try { device?.name } catch (_: SecurityException) { null }
                        val address = device?.address
                        Log.i("MoodyRoutine", "Bluetooth device disconnected: $name ($address)")
                        EventBus.emit(AutomationEvent.BluetoothEvent(isConnected = false, deviceName = name, deviceAddress = address))
                    }
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
            .setContentText("Monitoring for automated triggers & locations")
            .setSmallIcon(android.R.drawable.ic_menu_preferences)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)

        val db = MoodyRoutineDatabase.getInstance(this)
        val routineRepo = RoutineRepository(db.routineDao())
        val modeRepo = ModeRepository(db.modeDao())

        engine = AutomationEngine(
            context = this,
            routineRepository = routineRepo,
            modeRepository = modeRepo,
            conditionEvaluator = ConditionEvaluator(),
            actionExecutor = ActionExecutor(this)
        )
        engine?.start()

        geofenceManager = GeofenceManager(this)
        locationTracker = LocationTracker(this)

        // Observe both routines and modes continuously
        scope.launch {
            combine(
                routineRepo.getEnabledRoutines(),
                modeRepo.getAllModes()
            ) { routines, modes ->
                Pair(routines, modes)
            }.collect { (routines, modes) ->
                locationTracker?.updateTargets(routines, modes)
                geofenceManager?.updateGeofences(routines, modes)
            }
        }

        registerReceivers()
        registerNetworkCallback()
        startTimeTicker()
        startAppTracker()
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(receiver, filter)
        isReceiverRegistered = true
    }

    private fun registerNetworkCallback() {
        try {
            connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val caps = connectivityManager?.getNetworkCapabilities(network)
                    if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                        @Suppress("DEPRECATION")
                        val info = wifiManager?.connectionInfo
                        val rawSsid = info?.ssid?.replace("\"", "")
                        val ssid = if (rawSsid == "<unknown ssid>" || rawSsid.isNullOrBlank()) null else rawSsid
                        Log.i("MoodyRoutine", "Wi-Fi connected: ssid=$ssid")
                        scope.launch {
                            EventBus.emit(AutomationEvent.WifiEvent(isConnected = true, ssid = ssid))
                        }
                    }
                }

                override fun onLost(network: Network) {
                    Log.i("MoodyRoutine", "Wi-Fi disconnected")
                    scope.launch {
                        EventBus.emit(AutomationEvent.WifiEvent(isConnected = false, ssid = null))
                    }
                }
            }

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            Log.e("MoodyRoutine", "Error registering network callback", e)
        }
    }

    private fun startTimeTicker() {
        timeTickerJob = scope.launch {
            while (isActive) {
                val cal = Calendar.getInstance()
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val minute = cal.get(Calendar.MINUTE)
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                EventBus.emit(AutomationEvent.TimeEvent(hour, minute, dayOfWeek))

                // Align to next minute
                val seconds = cal.get(Calendar.SECOND)
                val millisToNextMinute = (60 - seconds) * 1000L
                delay(if (millisToNextMinute > 1000L) millisToNextMinute else 60000L)
            }
        }
    }

    private fun startAppTracker() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager

        appTrackerJob = scope.launch {
            while (isActive) {
                delay(2000L)
                if (powerManager?.isInteractive != true) continue

                try {
                    val time = System.currentTimeMillis()
                    val events = usageStatsManager.queryEvents(time - 3500L, time)
                    val event = UsageEvents.Event()
                    var currentPkg: String? = null

                    while (events.hasNextEvent()) {
                        events.getNextEvent(event)
                        if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                            currentPkg = event.packageName
                        }
                    }

                    if (currentPkg != null && currentPkg != lastForegroundApp && currentPkg != packageName) {
                        val previousPkg = lastForegroundApp
                        lastForegroundApp = currentPkg
                        Log.d("MoodyRoutine", "Foreground app switched: $currentPkg (was $previousPkg)")
                        if (previousPkg != null) {
                            EventBus.emit(AutomationEvent.AppEvent(packageName = previousPkg, isOpened = false))
                        }
                        EventBus.emit(AutomationEvent.AppEvent(packageName = currentPkg, isOpened = true))
                    }
                } catch (_: Exception) {}
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        engine?.stop()
        locationTracker?.stopTracking()
        timeTickerJob?.cancel()
        appTrackerJob?.cancel()
        scope.cancel()

        if (isReceiverRegistered) {
            unregisterReceiver(receiver)
            isReceiverRegistered = false
        }
        networkCallback?.let {
            try { connectivityManager?.unregisterNetworkCallback(it) } catch (_: Exception) {}
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
