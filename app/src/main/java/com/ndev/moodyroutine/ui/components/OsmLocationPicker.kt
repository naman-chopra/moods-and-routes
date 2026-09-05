package com.ndev.moodyroutine.ui.components

import android.Manifest
import android.content.Context
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Polygon
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class OsmPlaceSuggestion(
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double
)

private object OsmGeocoderHelper {
    private const val USER_AGENT = "MoodyRoutine-App/1.0 (contact: ndev.hoster@gmail.com)"

    suspend fun searchPlaces(context: Context, query: String): List<OsmPlaceSuggestion> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        // 1. Try OpenStreetMap Nominatim
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = URL("https://nominatim.openstreetmap.org/search?q=$encoded&format=json&addressdetails=1&limit=5")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", USER_AGENT)
                connectTimeout = 4000
                readTimeout = 4000
            }
            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(jsonStr)
                if (array.length() > 0) {
                    val list = mutableListOf<OsmPlaceSuggestion>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val rawName = obj.optString("name")
                        val displayName = obj.optString("display_name")
                        val title = if (rawName.isNotBlank()) rawName else displayName.split(",").firstOrNull()?.trim() ?: query
                        val lat = obj.optDouble("lat")
                        val lon = obj.optDouble("lon")
                        list.add(OsmPlaceSuggestion(title = title, subtitle = displayName, latitude = lat, longitude = lon))
                    }
                    return@withContext list
                }
            }
        } catch (_: Exception) {}

        // 2. Fallback to native Android Geocoder
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocationName(query, 5)
            if (!addresses.isNullOrEmpty()) {
                return@withContext addresses.map { addr ->
                    OsmPlaceSuggestion(
                        title = addr.featureName ?: query,
                        subtitle = addr.getAddressLine(0) ?: "",
                        latitude = addr.latitude,
                        longitude = addr.longitude
                    )
                }
            }
        } catch (_: Exception) {}

        emptyList()
    }

    suspend fun reverseGeocode(context: Context, lat: Double, lon: Double): Pair<String, String>? = withContext(Dispatchers.IO) {
        // 1. Try OpenStreetMap Nominatim
        try {
            val url = URL("https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=json")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", USER_AGENT)
                connectTimeout = 4000
                readTimeout = 4000
            }
            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(jsonStr)
                val rawName = obj.optString("name")
                val displayName = obj.optString("display_name")
                val addr = obj.optJSONObject("address")
                val feature = if (rawName.isNotBlank()) rawName else {
                    addr?.optString("suburb")?.takeIf { it.isNotBlank() }
                        ?: addr?.optString("neighbourhood")?.takeIf { it.isNotBlank() }
                        ?: addr?.optString("road")?.takeIf { it.isNotBlank() }
                        ?: addr?.optString("city")?.takeIf { it.isNotBlank() }
                        ?: "Selected Location"
                }
                return@withContext Pair(feature, displayName)
            }
        } catch (_: Exception) {}

        // 2. Fallback to native Geocoder
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val full = addr.getAddressLine(0) ?: ""
                val feature = addr.featureName ?: addr.subLocality ?: addr.locality ?: "Selected Location"
                return@withContext Pair(feature, full)
            }
        } catch (_: Exception) {}

        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OsmLocationPickerDialog(
    initialIsArrive: Boolean = true,
    initialLocationName: String = "Home",
    initialRadius: Int = 150,
    onDismiss: () -> Unit,
    onConfirm: (isArrive: Boolean, locationName: String, address: String, latitude: Double, longitude: Double, radius: Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Initialize osmdroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    var isArrive by remember { mutableStateOf(initialIsArrive) }
    var locationName by remember { mutableStateOf(initialLocationName) }
    var addressText by remember { mutableStateOf("") }
    var currentGeoPoint by remember { mutableStateOf(GeoPoint(28.6139, 77.2090)) } // default center
    var radius by remember { mutableIntStateOf(initialRadius) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchSuggestions by remember { mutableStateOf<List<OsmPlaceSuggestion>>(emptyList()) }
    var isSelectingSuggestion by remember { mutableStateOf(false) }
    var isLocating by remember { mutableStateOf(false) }

    // Bottom drawer expand/collapse state
    var isDrawerExpanded by remember { mutableStateOf(true) }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var reverseGeocodeJob by remember { mutableStateOf<Job?>(null) }

    fun updateMapCircle(map: MapView, centerPoint: GeoPoint, rad: Int) {
        map.overlays.removeAll { it is Polygon }
        val circle = Polygon().apply {
            points = Polygon.pointsAsCircle(centerPoint, rad.toDouble())
            val fillColor = if (isArrive) 0x333872FF else 0x33E11D48
            val strokeColor = if (isArrive) 0xAA3872FF.toInt() else 0xAAE11D48.toInt()
            fillPaint.color = fillColor
            outlinePaint.color = strokeColor
            outlinePaint.strokeWidth = 3f
        }
        map.overlays.add(circle)
        map.invalidate()
    }

    fun triggerReverseGeocode(point: GeoPoint) {
        reverseGeocodeJob?.cancel()
        reverseGeocodeJob = coroutineScope.launch {
            delay(350) // debounce
            val result = OsmGeocoderHelper.reverseGeocode(context, point.latitude, point.longitude)
            if (result != null) {
                addressText = result.second
                if (locationName.isBlank() || locationName == "Home" || locationName == "Work" || locationName == "Selected Location") {
                    locationName = result.first
                }
            } else {
                addressText = "${String.format(Locale.US, "%.4f", point.latitude)}°, ${String.format(Locale.US, "%.4f", point.longitude)}°"
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isLocating = true
            try {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { loc ->
                        isLocating = false
                        if (loc != null) {
                            val newPoint = GeoPoint(loc.latitude, loc.longitude)
                            currentGeoPoint = newPoint
                            mapViewRef?.let { map ->
                                map.controller.animateTo(newPoint, 16.5, 800L)
                                updateMapCircle(map, newPoint, radius)
                            }
                            triggerReverseGeocode(newPoint)
                        }
                    }
                    .addOnFailureListener { isLocating = false }
            } catch (e: SecurityException) {
                isLocating = false
            }
        }
    }

    // Auto-locate GPS position on launch
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Re-draw circle when radius or arrive/leave mode changes
    LaunchedEffect(radius, isArrive) {
        mapViewRef?.let { map ->
            updateMapCircle(map, currentGeoPoint, radius)
        }
    }

    // Search query autocomplete suggestions (debounced)
    LaunchedEffect(searchQuery) {
        if (isSelectingSuggestion) return@LaunchedEffect
        val q = searchQuery.trim()
        if (q.length >= 2) {
            delay(300)
            isSearching = true
            val results = OsmGeocoderHelper.searchPlaces(context, q)
            searchSuggestions = results
            isSearching = false
        } else {
            searchSuggestions = emptyList()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("Select Place", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // Interactive Map Viewport with Overlays
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // OpenStreetMap View
                    AndroidView(
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                controller.setZoom(16.0)
                                controller.setCenter(currentGeoPoint)

                                // Tap anywhere to re-center map to that spot
                                val eventsReceiver = object : MapEventsReceiver {
                                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                        p ?: return false
                                        currentGeoPoint = p
                                        controller.animateTo(p, zoomLevelDouble, 400L)
                                        updateMapCircle(this@apply, p, radius)
                                        triggerReverseGeocode(p)
                                        return true
                                    }

                                    override fun longPressHelper(p: GeoPoint?): Boolean = false
                                }
                                overlays.add(0, MapEventsOverlay(eventsReceiver))

                                // Pan / Scroll listener: update center & circle
                                val listener = object : MapListener {
                                    override fun onScroll(event: ScrollEvent?): Boolean {
                                        val center = mapCenter as? GeoPoint ?: return false
                                        currentGeoPoint = center
                                        updateMapCircle(this@apply, center, radius)
                                        triggerReverseGeocode(center)
                                        return false
                                    }

                                    override fun onZoom(event: ZoomEvent?): Boolean {
                                        val center = mapCenter as? GeoPoint ?: return false
                                        currentGeoPoint = center
                                        updateMapCircle(this@apply, center, radius)
                                        return false
                                    }
                                }
                                addMapListener(listener)

                                updateMapCircle(this, currentGeoPoint, radius)
                                mapViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Fixed Center Pin (Uber / One UI style) pointing right at screen center
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .zIndex(3f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.offset(y = (-24).dp) // offset so pin tip touches center
                        ) {
                            Icon(
                                Icons.Rounded.LocationOn,
                                contentDescription = "Pin",
                                tint = if (isArrive) MaterialTheme.colorScheme.primary else Color(0xFFE11D48),
                                modifier = Modifier
                                    .size(46.dp)
                                    .shadow(6.dp, CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp, 4.dp)
                                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                            )
                        }
                    }

                    // Top Search Bar & Suggestions Overlay
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .zIndex(4f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Search Box Card
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            shadowElevation = 6.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search address, place, or city...") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSearching) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else if (searchQuery.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            searchQuery = ""
                                            searchSuggestions = emptyList()
                                        }
                                    ) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        // Search Suggestions Dropdown Card
                        AnimatedVisibility(
                            visible = searchSuggestions.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp,
                                shadowElevation = 8.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp)
                                ) {
                                    items(searchSuggestions) { suggestion ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    isSelectingSuggestion = true
                                                    searchQuery = suggestion.title
                                                    locationName = suggestion.title
                                                    addressText = suggestion.subtitle
                                                    val pt = GeoPoint(suggestion.latitude, suggestion.longitude)
                                                    currentGeoPoint = pt
                                                    mapViewRef?.let { map ->
                                                        map.controller.animateTo(pt, 16.5, 800L)
                                                        updateMapCircle(map, pt, radius)
                                                    }
                                                    searchSuggestions = emptyList()
                                                    coroutineScope.launch {
                                                        delay(500)
                                                        isSelectingSuggestion = false
                                                    }
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Rounded.LocationOn,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = suggestion.title,
                                                    fontWeight = FontWeight.SemiBold,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = suggestion.subtitle,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        }

                        // Preset Chips (Smooth LazyRow, no text wrapping)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(listOf("Home", "Work", "Gym", "School", "Office", "Cafe")) { preset ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                    tonalElevation = 3.dp,
                                    shadowElevation = 2.dp
                                ) {
                                    FilterChip(
                                        selected = locationName.equals(preset, ignoreCase = true),
                                        onClick = {
                                            locationName = preset
                                            searchQuery = preset
                                        },
                                        label = { Text(preset, fontWeight = FontWeight.Medium) },
                                        shape = RoundedCornerShape(20.dp),
                                        border = null
                                    )
                                }
                            }
                        }
                    }

                    // Floating Instruction Hint Badge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .zIndex(2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.PanTool,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Move map to position pin or tap anywhere",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Floating Action Buttons (My Location & Zoom)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp)
                            .zIndex(3f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Zoom In Button
                        FloatingActionButton(
                            onClick = { mapViewRef?.controller?.zoomIn() },
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(42.dp)
                                .shadow(3.dp, CircleShape)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
                        }

                        // Zoom Out Button
                        FloatingActionButton(
                            onClick = { mapViewRef?.controller?.zoomOut() },
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(42.dp)
                                .shadow(3.dp, CircleShape)
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
                        }

                        // Re-Center on GPS
                        FloatingActionButton(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(4.dp, CircleShape)
                        ) {
                            if (isLocating) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.MyLocation, contentDescription = "My location")
                            }
                        }
                    }
                }

                // Draggable Bottom Drawer (One UI Style with Smooth Swipe Gestures)
                Surface(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (accumulatedDrag > 35f) {
                                        isDrawerExpanded = false
                                    } else if (accumulatedDrag < -35f) {
                                        isDrawerExpanded = true
                                    }
                                    accumulatedDrag = 0f
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    accumulatedDrag += dragAmount
                                }
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Drag Handle (Swipe up/down or tap to toggle)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isDrawerExpanded = !isDrawerExpanded }
                                .padding(top = 10.dp, bottom = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                        }

                        // Place name & Address details row (Always visible)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isDrawerExpanded = !isDrawerExpanded },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isArrive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else Color(0xFFE11D48).copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isArrive) Icons.Rounded.LocationOn else Icons.Rounded.LocationOff,
                                    contentDescription = null,
                                    tint = if (isArrive) MaterialTheme.colorScheme.primary else Color(0xFFE11D48),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = locationName.ifBlank { "Selected Location" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (addressText.isNotBlank()) addressText else "${String.format(Locale.US, "%.4f", currentGeoPoint.latitude)}°, ${String.format(Locale.US, "%.4f", currentGeoPoint.longitude)}°",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            // Small indicator showing if expanded or collapsed
                            IconButton(onClick = { isDrawerExpanded = !isDrawerExpanded }) {
                                Icon(
                                    if (isDrawerExpanded) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = "Toggle drawer",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Expandable Content (When I arrive/leave chips + Radius Slider)
                        AnimatedVisibility(
                            visible = isDrawerExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // When I arrive vs When I leave selector
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = isArrive,
                                        onClick = { isArrive = true },
                                        label = { Text("When I arrive") },
                                        leadingIcon = if (isArrive) { { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null,
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = !isArrive,
                                        onClick = { isArrive = false },
                                        label = { Text("When I leave") },
                                        leadingIcon = if (!isArrive) { { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Target Area Radius Slider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Target area radius", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            "${radius}m",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Slider(
                                        value = radius.toFloat(),
                                        onValueChange = { radius = it.toInt() },
                                        valueRange = 50f..1000f,
                                        steps = 18,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // Single, dedicated "Done" button (Properly padded above system nav bar)
                        Button(
                            onClick = {
                                onConfirm(
                                    isArrive,
                                    locationName.trim().ifBlank { if (isArrive) "Home" else "Work" },
                                    addressText,
                                    currentGeoPoint.latitude,
                                    currentGeoPoint.longitude,
                                    radius
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
