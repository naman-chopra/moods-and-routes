package com.ndev.moodyroutine.service

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.ndev.moodyroutine.MainActivity
import com.ndev.moodyroutine.R
import com.ndev.moodyroutine.data.model.ActionType
import com.ndev.moodyroutine.data.model.Mode
import com.ndev.moodyroutine.ui.util.HumanFormatter
import com.ndev.moodyroutine.ui.util.UiIcons
import com.ndev.moodyroutine.util.AppLogger
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
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

class AutomationService : Service() {
    companion object {
        const val CHANNEL_SERVICE = "moody_routine_service"
        const val CHANNEL_ACTIVE_MODES = "active_modes_channel"
        const val CHANNEL_BLOCKER = "moody_routine_blocker"
        const val NOTIFICATION_ID_SERVICE = 1
        const val NOTIFICATION_ID_MODE_BASE = 10000
        const val ACTION_TURN_OFF_MODE = "com.ndev.moodyroutine.action.TURN_OFF_MODE"
        const val EXTRA_MODE_ID = "extra_mode_id"

        private val snoozedApps = java.util.concurrent.ConcurrentHashMap<String, Long>()

        fun temporarilyAllowApp(packageName: String, durationMs: Long) {
            snoozedApps[packageName] = System.currentTimeMillis() + durationMs
        }

        fun isAppSnoozed(packageName: String): Boolean {
            val expiry = snoozedApps[packageName] ?: return false
            if (System.currentTimeMillis() > expiry) {
                snoozedApps.remove(packageName)
                return false
            }
            return true
        }
    }

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
    private var modeRepository: ModeRepository? = null
    private var actionExecutor: ActionExecutor? = null
    private val currentlyNotifiedModeIds = mutableSetOf<Long>()
    private var activeRestrictedApps: Map<String, Mode> = emptyMap()

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
                        AppLogger.i("AutomationService", "Power connected")
                        EventBus.emit(AutomationEvent.PowerEvent(true))
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        AppLogger.i("AutomationService", "Power disconnected")
                        EventBus.emit(AutomationEvent.PowerEvent(false))
                    }
                    Intent.ACTION_HEADSET_PLUG -> {
                        val state = intent.getIntExtra("state", 0)
                        AppLogger.i("AutomationService", "Headphones plug state: $state")
                        EventBus.emit(AutomationEvent.HeadphoneEvent(state == 1))
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        AppLogger.d("AutomationService", "Screen ON")
                        EventBus.emit(AutomationEvent.ScreenEvent(true))
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        AppLogger.d("AutomationService", "Screen OFF")
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
                        AppLogger.i("AutomationService", "Bluetooth device connected: $name ($address)")
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
                        AppLogger.i("AutomationService", "Bluetooth device disconnected: $name ($address)")
                        EventBus.emit(AutomationEvent.BluetoothEvent(isConnected = false, deviceName = name, deviceAddress = address))
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        GeofenceReceiver.serviceStartTime = System.currentTimeMillis()
        AppLogger.i("AutomationService", "AutomationService created")
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "moody_routine_service")
            .setContentTitle("MoodyRoutine is active")
            .setContentText("Monitoring for automated triggers & locations")
            .setSmallIcon(android.R.drawable.ic_menu_preferences)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    1,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    1,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(1, notification)
            }
        } catch (e: Exception) {
            AppLogger.e("AutomationService", "Failed to start foreground service", e)
            try {
                // Fallback to special use only if location fails
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                }
            } catch (e2: Exception) {
                AppLogger.e("AutomationService", "Fallback startForeground also failed", e2)
            }
        }

        val db = MoodyRoutineDatabase.getInstance(this)
        val routineRepo = RoutineRepository(db.routineDao())
        val modeRepo = ModeRepository(db.modeDao())
        modeRepository = modeRepo

        val actionExec = ActionExecutor(this)
        actionExecutor = actionExec

        engine = AutomationEngine(
            context = this,
            routineRepository = routineRepo,
            modeRepository = modeRepo,
            conditionEvaluator = ConditionEvaluator(),
            actionExecutor = actionExec,
            locationTrackerProvider = { locationTracker }
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

        // Observe active modes and show persistent notifications
        scope.launch {
            modeRepo.getActiveModes().collect { activeModes ->
                updateActiveModeNotifications(activeModes)
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
                        AppLogger.i("AutomationService", "Wi-Fi connected: ssid=$ssid")
                        scope.launch {
                            EventBus.emit(AutomationEvent.WifiEvent(isConnected = true, ssid = ssid))
                        }
                    }
                }

                override fun onLost(network: Network) {
                    AppLogger.i("AutomationService", "Wi-Fi disconnected")
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
            AppLogger.e("AutomationService", "Error registering network callback", e)
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
                        AppLogger.d("AutomationService", "Foreground app switched: $currentPkg (was $previousPkg)")
                        if (previousPkg != null) {
                            EventBus.emit(AutomationEvent.AppEvent(packageName = previousPkg, isOpened = false))
                        }
                        EventBus.emit(AutomationEvent.AppEvent(packageName = currentPkg, isOpened = true))

                        // Check if currentPkg is restricted under an active mode
                        val blockingMode = activeRestrictedApps[currentPkg]
                        if (blockingMode != null && !isAppSnoozed(currentPkg)) {
                            AppLogger.i("AutomationService", "Restricted app launched: $currentPkg during ${blockingMode.name}")
                            val appLabel = try {
                                val pm = packageManager
                                val appInfo = pm.getApplicationInfo(currentPkg, 0)
                                pm.getApplicationLabel(appInfo).toString()
                            } catch (_: Exception) {
                                currentPkg
                            }
                            val blockIntent = Intent(this@AutomationService, com.ndev.moodyroutine.ui.activity.AppBlockActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                putExtra(com.ndev.moodyroutine.ui.activity.AppBlockActivity.EXTRA_PACKAGE_NAME, currentPkg)
                                putExtra(com.ndev.moodyroutine.ui.activity.AppBlockActivity.EXTRA_APP_NAME, appLabel)
                                putExtra(com.ndev.moodyroutine.ui.activity.AppBlockActivity.EXTRA_MODE_NAME, blockingMode.name)
                                putExtra(com.ndev.moodyroutine.ui.activity.AppBlockActivity.EXTRA_MODE_ID, blockingMode.id)
                            }
                            val options = ActivityOptions.makeBasic()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                options.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                            }
                            val pi = PendingIntent.getActivity(
                                this@AutomationService,
                                (currentPkg.hashCode() and 0xffff),
                                blockIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )

                            // Show full screen intent notification
                            val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                            val blockNotif = NotificationCompat.Builder(this@AutomationService, CHANNEL_BLOCKER)
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setContentTitle("Stay focused - ${blockingMode.name}")
                                .setContentText("$appLabel is blocked during ${blockingMode.name}")
                                .setPriority(NotificationCompat.PRIORITY_MAX)
                                .setCategory(NotificationCompat.CATEGORY_ALARM)
                                .setFullScreenIntent(pi, true)
                                .setAutoCancel(true)
                                .build()
                            notifManager?.notify(9999, blockNotif)

                            try {
                                pi.send(this@AutomationService, 0, null, null, null, null, options.toBundle())
                            } catch (_: Exception) {
                                startActivity(blockIntent)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_TURN_OFF_MODE) {
            val modeId = intent.getLongExtra(EXTRA_MODE_ID, -1L)
            if (modeId != -1L) {
                scope.launch {
                    val repo = modeRepository ?: return@launch
                    val mode = repo.getModeById(modeId).firstOrNull()
                    if (mode != null) {
                        AppLogger.i("AutomationService", "Turning off mode ${mode.name} via persistent notification action")
                        repo.setModeActive(mode.id, false)
                        if (mode.revertActionsOnExit) {
                            actionExecutor?.revertMode(mode)
                        }
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun updateActiveModeNotifications(activeModes: List<Mode>) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        // Update active restricted apps mapping from active modes
        val restrictedMap = mutableMapOf<String, Mode>()
        for (m in activeModes) {
            for (action in m.actions) {
                if (action.type == ActionType.RESTRICT_APPS) {
                    val pkgs = action.params["restrictedPackages"]?.split(",") ?: emptyList()
                    for (pkg in pkgs) {
                        val trimmed = pkg.trim()
                        if (trimmed.isNotBlank()) {
                            restrictedMap[trimmed] = m
                        }
                    }
                }
            }
        }
        activeRestrictedApps = restrictedMap

        // 1. Cancel any legacy separate mode notifications
        for (id in currentlyNotifiedModeIds) {
            manager.cancel(NOTIFICATION_ID_MODE_BASE + id.toInt())
        }
        currentlyNotifiedModeIds.clear()

        if (activeModes.isEmpty()) {
            // Restore default idle service notification
            val openIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                this,
                NOTIFICATION_ID_SERVICE,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val idleNotification = NotificationCompat.Builder(this, CHANNEL_SERVICE)
                .setContentTitle("MoodyRoutine is active")
                .setContentText("Monitoring for automated triggers & locations")
                .setSmallIcon(com.ndev.moodyroutine.R.drawable.ic_mode_custom)
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(openPendingIntent)
                .build()

            manager.notify(NOTIFICATION_ID_SERVICE, idleNotification)
            AppLogger.i("AutomationService", "Updated service notification to idle state")
        } else {
            val mode = activeModes.first()
            val notifId = NOTIFICATION_ID_SERVICE

            val openIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                this,
                notifId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val turnOffIntent = Intent(this, AutomationService::class.java).apply {
                action = ACTION_TURN_OFF_MODE
                putExtra(EXTRA_MODE_ID, mode.id)
            }
            val turnOffPendingIntent = PendingIntent.getService(
                this,
                notifId,
                turnOffIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val actionSummary = if (mode.actions.isNotEmpty()) {
                mode.actions.joinToString(", ") { HumanFormatter.formatAction(it).first }
            } else {
                "Mode is currently active"
            }

            val iconResId = UiIcons.getModeDrawableRes(mode.iconName)
            val largeIconBitmap = UiIcons.getModeLargeIconBitmap(this, mode.iconName, mode.colorHex)
            val modeColor = try {
                android.graphics.Color.parseColor(mode.colorHex)
            } catch (_: Exception) {
                0xFF3B82F6.toInt()
            }

            val title = if (activeModes.size == 1) {
                "${mode.name} mode is on"
            } else {
                "${activeModes.joinToString { it.name }} are on"
            }

            val builder = NotificationCompat.Builder(this, CHANNEL_SERVICE)
                .setContentTitle(title)
                .setContentText(actionSummary)
                .setStyle(NotificationCompat.BigTextStyle().bigText("${mode.name} mode is currently active.\nActions: $actionSummary"))
                .setSmallIcon(iconResId)
                .setColor(modeColor)
                .setColorized(true)
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(openPendingIntent)
                .setGroup("moody_routine_active_modes")
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Turn off",
                    turnOffPendingIntent
                )

            if (largeIconBitmap != null) {
                builder.setLargeIcon(largeIconBitmap)
            }

            val notification = builder.build()
            manager.notify(notifId, notification)
            AppLogger.i("AutomationService", "Updated service notification for active mode: ${mode.name} with icon ${mode.iconName}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        engine?.stop()
        geofenceManager?.removeAllGeofences()
        locationTracker?.stopTracking()
        timeTickerJob?.cancel()
        appTrackerJob?.cancel()
        scope.cancel()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        for (id in currentlyNotifiedModeIds) {
            manager?.cancel(NOTIFICATION_ID_MODE_BASE + id.toInt())
        }
        currentlyNotifiedModeIds.clear()

        if (isReceiverRegistered) {
            unregisterReceiver(receiver)
            isReceiverRegistered = false
        }
        networkCallback?.let {
            try { connectivityManager?.unregisterNetworkCallback(it) } catch (_: Exception) {}
        }
        AppLogger.i("AutomationService", "AutomationService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Automation Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val activeModesChannel = NotificationChannel(
                CHANNEL_ACTIVE_MODES,
                "Active Modes",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Persistent notifications for currently active modes"
            }
            val blockerChannel = NotificationChannel(
                CHANNEL_BLOCKER,
                "App Blocker",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Fullscreen blocker alerts for restricted apps"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(activeModesChannel)
            manager.createNotificationChannel(blockerChannel)
        }
    }
}
