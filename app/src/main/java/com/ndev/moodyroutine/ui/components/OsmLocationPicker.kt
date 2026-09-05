package com.ndev.moodyroutine.ui.components

import android.Manifest
import android.content.Context
import android.location.Address
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.util.Locale

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

    // Initialize osmdroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    var isArrive by remember { mutableStateOf(initialIsArrive) }
    var locationName by remember { mutableStateOf(initialLocationName) }
    var addressText by remember { mutableStateOf("") }
    var currentGeoPoint by remember { mutableStateOf(GeoPoint(28.6139, 77.2090)) } // default center
    var hasGotUserLocation by remember { mutableStateOf(false) }
    var radius by remember { mutableIntStateOf(initialRadius) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Address>>(emptyList()) }
    var isLocating by remember { mutableStateOf(false) }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun updateMapMarkerAndCircle(map: MapView, geoPoint: GeoPoint, rad: Int) {
        map.overlays.removeAll { it is Marker || it is Polygon }

        // Geofence Circle overlay
        val circle = Polygon().apply {
            points = Polygon.pointsAsCircle(geoPoint, rad.toDouble())
            val fillColor = if (isArrive) 0x333872FF else 0x33E11D48
            val strokeColor = if (isArrive) 0xAA3872FF.toInt() else 0xAAE11D48.toInt()
            fillPaint.color = fillColor
            outlinePaint.color = strokeColor
            outlinePaint.strokeWidth = 3f
        }
        map.overlays.add(circle)

        // Location Pin Marker
        val marker = Marker(map).apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = locationName
        }
        map.overlays.add(marker)

        map.invalidate()
    }

    fun reverseGeocode(point: GeoPoint) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(point.latitude, point.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val full = addr.getAddressLine(0) ?: ""
                    val feature = addr.featureName ?: addr.subLocality ?: addr.locality ?: "Selected Place"
                    withContext(Dispatchers.Main) {
                        addressText = full
                        if (locationName.isBlank() || locationName == "Home") {
                            locationName = feature
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    addressText = "${String.format("%.4f", point.latitude)}°, ${String.format("%.4f", point.longitude)}°"
                }
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
                            hasGotUserLocation = true
                            mapViewRef?.let { map ->
                                map.controller.animateTo(newPoint, 16.5, 1000L)
                                updateMapMarkerAndCircle(map, newPoint, radius)
                            }
                            reverseGeocode(newPoint)
                        }
                    }
                    .addOnFailureListener { isLocating = false }
            } catch (e: SecurityException) {
                isLocating = false
            }
        }
    }

    // Auto-locate current position on launch
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
            updateMapMarkerAndCircle(map, currentGeoPoint, radius)
        }
    }

    fun searchAddress(query: String) {
        if (query.isBlank()) return
        isSearching = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val results = geocoder.getFromLocationName(query, 4) ?: emptyList()
                withContext(Dispatchers.Main) {
                    searchResults = results
                    isSearching = false
                    if (results.isNotEmpty()) {
                        val first = results[0]
                        val newPoint = GeoPoint(first.latitude, first.longitude)
                        currentGeoPoint = newPoint
                        locationName = first.featureName ?: query
                        addressText = first.getAddressLine(0) ?: ""
                        mapViewRef?.let { map ->
                            map.controller.animateTo(newPoint, 16.5, 1000L)
                            updateMapMarkerAndCircle(map, newPoint, radius)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isSearching = false }
            }
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
                    actions = {
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
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // OpenStreetMap View
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(16.0)
                            controller.setCenter(currentGeoPoint)

                            // Tap on map to place pin
                            val eventsReceiver = object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    p ?: return false
                                    currentGeoPoint = p
                                    updateMapMarkerAndCircle(this@apply, p, radius)
                                    reverseGeocode(p)
                                    return true
                                }

                                override fun longPressHelper(p: GeoPoint?): Boolean = false
                            }
                            overlays.add(0, MapEventsOverlay(eventsReceiver))

                            updateMapMarkerAndCircle(this, currentGeoPoint, radius)
                            mapViewRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Top Controls: Search Bar & Preset Chips
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Search Bar Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
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
                                placeholder = { Text("Search address or place...") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            if (isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchAddress(searchQuery) }) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    // Quick Preset Chips (LazyRow to prevent any text wrapping)
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
                                        searchAddress(preset)
                                    },
                                    label = { Text(preset, fontWeight = FontWeight.Medium) },
                                    shape = RoundedCornerShape(20.dp),
                                    border = null
                                )
                            }
                        }
                    }
                }

                // Floating Re-Center Button (My Location)
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
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 270.dp)
                        .shadow(4.dp, CircleShape)
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.MyLocation, contentDescription = "My location")
                    }
                }

                // Bottom Configuration Card (One UI Style)
                Surface(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
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

                        // Place name & Address details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
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
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = locationName.ifBlank { "Selected Location" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (addressText.isNotBlank()) addressText else "${String.format("%.4f", currentGeoPoint.latitude)}°, ${String.format("%.4f", currentGeoPoint.longitude)}°",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }

                        // Radius Slider with live feedback
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
                                .height(48.dp)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
