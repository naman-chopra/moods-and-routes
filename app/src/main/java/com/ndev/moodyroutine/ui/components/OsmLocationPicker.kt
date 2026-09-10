package com.ndev.moodyroutine.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

        // 1. Try native Android Geocoder first (backed by Google Play Services, fast)
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(query, 5)
                if (!addresses.isNullOrEmpty()) {
                    return@withContext addresses.map { addr ->
                        val full = addr.getAddressLine(0) ?: ""
                        val title = addr.featureName?.takeIf { it.isNotBlank() && it != addr.postalCode }
                            ?: addr.subLocality
                            ?: addr.locality
                            ?: full.split(",").firstOrNull()?.trim()
                            ?: query
                        OsmPlaceSuggestion(
                            title = title,
                            subtitle = full,
                            latitude = addr.latitude,
                            longitude = addr.longitude
                        )
                    }
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.w("OsmGeocoderHelper", "Native Geocoder search failed: ${e.message}")
        }

        // 2. Fallback to OpenStreetMap Nominatim
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = URL("https://nominatim.openstreetmap.org/search?q=$encoded&format=json&addressdetails=1&limit=5")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", USER_AGENT)
                connectTimeout = 3000
                readTimeout = 3000
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
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.w("OsmGeocoderHelper", "Nominatim search failed: ${e.message}")
        }

        emptyList()
    }

    suspend fun reverseGeocode(context: Context, lat: Double, lon: Double): Pair<String, String>? = withContext(Dispatchers.IO) {
        // 1. Try native Geocoder first (Google Play Services backed, instant)
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val full = addr.getAddressLine(0) ?: ""
                    val plusCodeRegex = Regex("^[A-Z0-9]{4,8}\\+[A-Z0-9]+$")
                    val feature = addr.featureName?.takeIf {
                        it.isNotBlank() && it != addr.postalCode && it != addr.subThoroughfare && !it.matches(plusCodeRegex)
                    }
                        ?: addr.subLocality
                        ?: addr.locality
                        ?: full.split(",").map { it.trim() }.firstOrNull { it.isNotBlank() && !it.matches(plusCodeRegex) }
                        ?: full.split(",").firstOrNull()?.trim()
                        ?: "Selected Location"
                    if (full.isNotBlank()) {
                        return@withContext Pair(feature, full)
                    }
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.w("OsmGeocoderHelper", "Native reverse geocode failed: ${e.message}")
        }

        // 2. Fallback to OpenStreetMap Nominatim
        try {
            val url = URL("https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=json")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", USER_AGENT)
                connectTimeout = 3000
                readTimeout = 3000
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
                        ?: displayName.split(",").firstOrNull()?.trim()
                        ?: "Selected Location"
                }
                return@withContext Pair(feature, displayName)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.w("OsmGeocoderHelper", "Nominatim reverse geocode failed: ${e.message}")
        }

        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OsmLocationPickerDialog(
    initialIsArrive: Boolean = true,
    initialLocationName: String = "",
    initialAddress: String = "",
    initialLatitude: Double? = null,
    initialLongitude: Double? = null,
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

    val hasExplicitInitialLocation = initialLatitude != null && initialLongitude != null && (initialLatitude != 0.0 || initialLongitude != 0.0)
    var userTouchedMap by remember { mutableStateOf(false) }
    var isProgrammaticMove by remember { mutableStateOf(false) }

    var isArrive by remember { mutableStateOf(initialIsArrive) }
    var locationName by remember { mutableStateOf(initialLocationName) }
    var customNameOverride by remember { mutableStateOf<String?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var tempRenameText by remember { mutableStateOf("") }
    var addressText by remember { mutableStateOf(initialAddress) }
    var currentGeoPoint by remember {
        mutableStateOf(
            if (hasExplicitInitialLocation) GeoPoint(initialLatitude!!, initialLongitude!!)
            else GeoPoint(28.6139, 77.2090)
        )
    }
    var radius by remember { mutableIntStateOf(initialRadius) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchSuggestions by remember { mutableStateOf<List<OsmPlaceSuggestion>>(emptyList()) }
    var isSelectingSuggestion by remember { mutableStateOf(false) }
    var isLocating by remember { mutableStateOf(false) }

    // Bottom drawer expand/collapse state
    var isDrawerExpanded by remember { mutableStateOf(true) }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    var isInfoExpanded by remember { mutableStateOf(false) }

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

    fun triggerReverseGeocode(point: GeoPoint, immediate: Boolean = false) {
        if (isSelectingSuggestion) return
        reverseGeocodeJob?.cancel()
        reverseGeocodeJob = coroutineScope.launch {
            if (!immediate) {
                delay(400) // debounce
            }
            val result = OsmGeocoderHelper.reverseGeocode(context, point.latitude, point.longitude)
            if (result != null) {
                addressText = result.second
                locationName = result.first
            } else {
                val coordsText = "${String.format(Locale.US, "%.5f", point.latitude)}°, ${String.format(Locale.US, "%.5f", point.longitude)}°"
                addressText = coordsText
                locationName = coordsText
            }
        }
    }

    fun locateCurrentPosition(isUserExplicit: Boolean = false) {
        isLocating = true
        try {
            // Fast path: use last known location immediately if available
            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                if (lastLoc != null && (!userTouchedMap || isUserExplicit)) {
                    val pt = GeoPoint(lastLoc.latitude, lastLoc.longitude)
                    currentGeoPoint = pt
                    isProgrammaticMove = true
                    mapViewRef?.let { map ->
                        map.controller.setCenter(pt)
                        map.controller.setZoom(16.5)
                        updateMapCircle(map, pt, radius)
                    }
                    triggerReverseGeocode(pt, immediate = true)
                    coroutineScope.launch {
                        delay(800)
                        isProgrammaticMove = false
                    }
                }
            }

            // Fresh accurate location
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    isLocating = false
                    if (loc != null && (!userTouchedMap || isUserExplicit)) {
                        val newPoint = GeoPoint(loc.latitude, loc.longitude)
                        currentGeoPoint = newPoint
                        isProgrammaticMove = true
                        mapViewRef?.let { map ->
                            map.controller.setCenter(newPoint)
                            map.controller.setZoom(16.5)
                            updateMapCircle(map, newPoint, radius)
                        }
                        triggerReverseGeocode(newPoint, immediate = true)
                        coroutineScope.launch {
                            delay(800)
                            isProgrammaticMove = false
                        }
                    }
                }
                .addOnFailureListener { isLocating = false }
        } catch (_: SecurityException) {
            isLocating = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            locateCurrentPosition(isUserExplicit = true)
        }
    }

    // Auto-locate GPS position on launch ONLY if no initial location was provided
    LaunchedEffect(Unit) {
        if (!hasExplicitInitialLocation) {
            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (hasFine || hasCoarse) {
                locateCurrentPosition(isUserExplicit = false)
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        } else if (initialAddress.isBlank()) {
            triggerReverseGeocode(currentGeoPoint, immediate = true)
        }
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
        val fabBottomPadding by animateDpAsState(
            targetValue = if (isDrawerExpanded) 430.dp else 140.dp,
            label = "fabPadding"
        )

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
                // OpenStreetMap View
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(16.0)
                            controller.setCenter(currentGeoPoint)

                            setOnTouchListener { _, event ->
                                if (event.action == MotionEvent.ACTION_DOWN) {
                                    userTouchedMap = true
                                }
                                false
                            }

                            // Tap anywhere to re-center map to that spot
                            val eventsReceiver = object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    p ?: return false
                                    userTouchedMap = true
                                    currentGeoPoint = p
                                    isProgrammaticMove = true
                                    controller.animateTo(p, zoomLevelDouble, 400L)
                                    updateMapCircle(this@apply, p, radius)
                                    triggerReverseGeocode(p, immediate = true)
                                    coroutineScope.launch {
                                        delay(800)
                                        isProgrammaticMove = false
                                    }
                                    isInfoExpanded = false
                                    return true
                                }

                                override fun longPressHelper(p: GeoPoint?): Boolean = false
                            }
                            overlays.add(0, MapEventsOverlay(eventsReceiver))

                            // Pan / Scroll listener: update center & circle
                            val listener = object : MapListener {
                                override fun onScroll(event: ScrollEvent?): Boolean {
                                    if (isProgrammaticMove || isSelectingSuggestion) return false
                                    if (!userTouchedMap && !hasExplicitInitialLocation) return false
                                    val center = mapCenter as? GeoPoint ?: return false
                                    currentGeoPoint = center
                                    updateMapCircle(this@apply, center, radius)
                                    triggerReverseGeocode(center, immediate = false)
                                    return false
                                }

                                override fun onZoom(event: ZoomEvent?): Boolean {
                                    if (isProgrammaticMove || isSelectingSuggestion) return false
                                    if (!userTouchedMap && !hasExplicitInitialLocation) return false
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
                        .statusBarsPadding()
                        .padding(16.dp)
                        .zIndex(4f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row containing separate Back Button and Search Box Card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Separate Floating Back Button
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            shadowElevation = 6.dp,
                            modifier = Modifier.size(48.dp)
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Search Box Card
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { innerTextField ->
                                        Box(contentAlignment = Alignment.CenterStart) {
                                            if (searchQuery.isEmpty()) {
                                                Text(
                                                    text = "Search places...",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                                if (isSearching) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else if (searchQuery.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            searchQuery = ""
                                            searchSuggestions = emptyList()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
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
                                                userTouchedMap = true
                                                customNameOverride = null
                                                isSelectingSuggestion = true
                                                isProgrammaticMove = true
                                                reverseGeocodeJob?.cancel()

                                                searchQuery = suggestion.title
                                                locationName = suggestion.title
                                                addressText = suggestion.subtitle
                                                val pt = GeoPoint(suggestion.latitude, suggestion.longitude)
                                                currentGeoPoint = pt
                                                mapViewRef?.let { map ->
                                                    map.controller.setCenter(pt)
                                                    map.controller.setZoom(16.5)
                                                    updateMapCircle(map, pt, radius)
                                                }
                                                searchSuggestions = emptyList()
                                                coroutineScope.launch {
                                                    delay(800)
                                                    isSelectingSuggestion = false
                                                    isProgrammaticMove = false
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

                }

                // Floating Action Buttons (Info, Zoom & My Location)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = fabBottomPadding)
                        .navigationBarsPadding()
                        .zIndex(3f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Small 'i' Button that expands to show move map dialog
                    Surface(
                        onClick = { isInfoExpanded = !isInfoExpanded },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp,
                        shadowElevation = 3.dp,
                        modifier = if (isInfoExpanded) Modifier.height(40.dp) else Modifier.size(40.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = if (isInfoExpanded) 12.dp else 0.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = "Map instructions",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            AnimatedVisibility(
                                visible = isInfoExpanded,
                                enter = fadeIn() + expandHorizontally(),
                                exit = fadeOut() + shrinkHorizontally()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Move map to position pin or tap anywhere",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Close",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Zoom In Button
                    Surface(
                        onClick = { mapViewRef?.controller?.zoomIn() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp,
                        shadowElevation = 3.dp,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = "Zoom In",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Zoom Out Button
                    Surface(
                        onClick = { mapViewRef?.controller?.zoomOut() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp,
                        shadowElevation = 3.dp,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Remove,
                                contentDescription = "Zoom Out",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Re-Center on GPS
                    Surface(
                        onClick = {
                            userTouchedMap = false
                            customNameOverride = null
                            searchQuery = ""
                            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (hasFine || hasCoarse) {
                                locateCurrentPosition(isUserExplicit = true)
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp,
                        shadowElevation = 3.dp,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isLocating) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    Icons.Rounded.MyLocation,
                                    contentDescription = "My location",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Floating Place Card (Full-screen Map background with floating card overlay)
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 14.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 54.dp)
                        .navigationBarsPadding()
                        .align(Alignment.BottomCenter)
                        .zIndex(5f)
                        .animateContentSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header Container: Place details (Tap anywhere to expand/collapse)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isDrawerExpanded = !isDrawerExpanded }
                        ) {
                            // Place name & Address details row (Always visible)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
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
                                     val derivedTitle = addressText.split(",")
                                         .map { it.trim() }
                                         .firstOrNull { it.isNotBlank() && !it.matches(Regex("^[A-Z0-9]{4,8}\\+[A-Z0-9]+$")) }
                                         ?: addressText.split(",").firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
                                         ?: locationName.takeIf { it.isNotBlank() }
                                         ?: if (isLocating) "Locating..." else "Selected Location"
                                     val displayTitle = customNameOverride?.takeIf { it.isNotBlank() } ?: derivedTitle

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = displayTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        IconButton(
                                            onClick = {
                                                tempRenameText = displayTitle
                                                showRenameDialog = true
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Edit,
                                                contentDescription = "Rename location",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (isLocating && addressText.isBlank()) "Locating current position..."
                                               else if (addressText.isNotBlank()) addressText
                                               else "${String.format(Locale.US, "%.5f", currentGeoPoint.latitude)}°, ${String.format(Locale.US, "%.5f", currentGeoPoint.longitude)}°",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = if (isDrawerExpanded) 3 else 1
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.5f", currentGeoPoint.latitude)}, ${String.format(Locale.US, "%.5f", currentGeoPoint.longitude)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
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
                        }

                        // Expandable Content (When I arrive/leave chips + Radius Slider + Done Button)
                        AnimatedVisibility(
                            visible = isDrawerExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(14.dp),
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

                                // Dedicated "Done" button inside expanded card
                                Button(
                                    onClick = {
                                         val resolvedName = customNameOverride?.trim()?.takeIf { it.isNotBlank() }
                                             ?: addressText.split(",")
                                                 .map { it.trim() }
                                                 .firstOrNull { it.isNotBlank() && !it.matches(Regex("^[A-Z0-9]{4,8}\\+[A-Z0-9]+$")) }
                                             ?: addressText.split(",").firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
                                             ?: locationName.trim().ifBlank { initialLocationName.ifBlank { "Selected Location" } }
                                        onConfirm(
                                            isArrive,
                                            resolvedName,
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

        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Rename Location") },
                text = {
                    OutlinedTextField(
                        value = tempRenameText,
                        onValueChange = { tempRenameText = it },
                        label = { Text("Location Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (tempRenameText.isNotBlank()) {
                                customNameOverride = tempRenameText.trim()
                            }
                            showRenameDialog = false
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                customNameOverride = null
                                triggerReverseGeocode(currentGeoPoint, immediate = true)
                                showRenameDialog = false
                            }
                        ) {
                            Text("Reset to Auto")
                        }
                        TextButton(onClick = { showRenameDialog = false }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
    }
