package com.ndev.moodyroutine.ui.dialogs

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Launch
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.ndev.moodyroutine.ui.theme.SamsungBlue
import com.ndev.moodyroutine.ui.util.HumanFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TimeConfigDialog(
    initialTime: String = "08:00",
    initialDays: String = "1,2,3,4,5,6,7",
    onDismiss: () -> Unit,
    onConfirm: (time: String, days: String) -> Unit
) {
    val timeParts = initialTime.split(":").mapNotNull { it.toIntOrNull() }
    var hour by remember { mutableIntStateOf(if (timeParts.size >= 2) timeParts[0] else 8) }
    var minute by remember { mutableIntStateOf(if (timeParts.size >= 2) timeParts[1] else 0) }

    val daysSet = remember {
        mutableStateListOf<Int>().apply {
            addAll(initialDays.split(",").mapNotNull { it.trim().toIntOrNull() })
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Time & Days", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Time display / slider
                Text(
                    text = String.format("%02d:%02d", hour, minute),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hour: $hour", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = hour.toFloat(),
                        onValueChange = { hour = it.toInt() },
                        valueRange = 0f..23f,
                        steps = 22,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Minute: $minute", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = minute.toFloat(),
                        onValueChange = { minute = it.toInt() },
                        valueRange = 0f..59f,
                        steps = 58,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                }

                // Days of week
                Text("Repeat on", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
                val dayLabels = listOf(
                    1 to ("S" to "Sun"),
                    2 to ("M" to "Mon"),
                    3 to ("T" to "Tue"),
                    4 to ("W" to "Wed"),
                    5 to ("T" to "Thu"),
                    6 to ("F" to "Fri"),
                    7 to ("S" to "Sat")
                )

                // Presets segmented bar
                val allDaysTime = listOf(1, 2, 3, 4, 5, 6, 7)
                val weekdaysTime = listOf(2, 3, 4, 5, 6)
                val weekendsTime = listOf(1, 7)
                val currentSortedTime = daysSet.sorted()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val presets = listOf(
                        "Every day" to allDaysTime,
                        "Weekdays" to weekdaysTime,
                        "Weekends" to weekendsTime
                    )
                    presets.forEach { (label, days) ->
                        val isSelected = currentSortedTime == days
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable {
                                    daysSet.clear()
                                    daysSet.addAll(days)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dayLabels.forEach { (dayNum, labels) ->
                        val (letter, shortName) = labels
                        val isSelected = daysSet.contains(dayNum)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (isSelected) daysSet.remove(dayNum) else daysSet.add(dayNum)
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letter,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = shortName,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val daysStr = if (daysSet.isEmpty()) "1,2,3,4,5,6,7" else daysSet.sorted().joinToString(",")
                    onConfirm(String.format("%02d:%02d", hour, minute), daysStr)
                }
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DayOfWeekConfigDialog(
    initialDays: String = "1,2,3,4,5,6,7",
    onDismiss: () -> Unit,
    onConfirm: (days: String) -> Unit
) {
    val daysSet = remember {
        mutableStateListOf<Int>().apply {
            val parsed = initialDays.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (parsed.isNotEmpty()) {
                addAll(parsed)
            } else {
                addAll(listOf(1, 2, 3, 4, 5, 6, 7))
            }
        }
    }

    val allDays = listOf(1, 2, 3, 4, 5, 6, 7)
    val weekdays = listOf(2, 3, 4, 5, 6)
    val weekends = listOf(1, 7)

    val currentSorted = daysSet.sorted()

    val summaryText = if (daysSet.isEmpty()) {
        "Select at least one day"
    } else {
        HumanFormatter.formatDays(currentSorted.joinToString(","))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Specific days of week",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (daysSet.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Presets segmented bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val presets = listOf(
                        "Every day" to allDays,
                        "Weekdays" to weekdays,
                        "Weekends" to weekends
                    )
                    presets.forEach { (label, days) ->
                        val isSelected = currentSorted == days
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable {
                                    daysSet.clear()
                                    daysSet.addAll(days)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 7 circular day chips (One UI style: Sunday to Saturday)
                val dayItems = listOf(
                    1 to ("S" to "Sun"),
                    2 to ("M" to "Mon"),
                    3 to ("T" to "Tue"),
                    4 to ("W" to "Wed"),
                    5 to ("T" to "Thu"),
                    6 to ("F" to "Fri"),
                    7 to ("S" to "Sat")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dayItems.forEach { (dayNum, labels) ->
                        val (letter, shortName) = labels
                        val isSelected = daysSet.contains(dayNum)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (isSelected) {
                                        daysSet.remove(dayNum)
                                    } else {
                                        daysSet.add(dayNum)
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letter,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = shortName,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val daysStr = daysSet.sorted().joinToString(",")
                    onConfirm(daysStr)
                },
                enabled = daysSet.isNotEmpty()
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TimeRangeConfigDialog(
    initialStartTime: String = "09:00",
    initialEndTime: String = "17:00",
    onDismiss: () -> Unit,
    onConfirm: (startTime: String, endTime: String) -> Unit
) {
    val startParts = initialStartTime.split(":").mapNotNull { it.toIntOrNull() }
    var startHour by remember { mutableIntStateOf(if (startParts.size >= 2) startParts[0] else 9) }
    var startMinute by remember { mutableIntStateOf(if (startParts.size >= 2) startParts[1] else 0) }

    val endParts = initialEndTime.split(":").mapNotNull { it.toIntOrNull() }
    var endHour by remember { mutableIntStateOf(if (endParts.size >= 2) endParts[0] else 17) }
    var endMinute by remember { mutableIntStateOf(if (endParts.size >= 2) endParts[1] else 0) }

    var selectedTab by remember { mutableIntStateOf(0) }

    val startStr = String.format("%02d:%02d", startHour, startMinute)
    val endStr = String.format("%02d:%02d", endHour, endMinute)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Time Range", fontWeight = FontWeight.Bold)
                Text(
                    text = "$startStr – $endStr",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("Start: $startStr") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("End: $endStr") },
                        modifier = Modifier.weight(1f)
                    )
                }

                val currentHour = if (selectedTab == 0) startHour else endHour
                val currentMinute = if (selectedTab == 0) startMinute else endMinute

                Text(
                    text = if (selectedTab == 0) "Adjust start time: $startStr" else "Adjust end time: $endStr",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hour: $currentHour", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = currentHour.toFloat(),
                        onValueChange = {
                            if (selectedTab == 0) startHour = it.toInt() else endHour = it.toInt()
                        },
                        valueRange = 0f..23f,
                        steps = 22,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Minute: $currentMinute", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = currentMinute.toFloat(),
                        onValueChange = {
                            if (selectedTab == 0) startMinute = it.toInt() else endMinute = it.toInt()
                        },
                        valueRange = 0f..59f,
                        steps = 58,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = {
                            startHour = 9; startMinute = 0
                            endHour = 17; endMinute = 0
                        },
                        label = { Text("Work (9-5)", fontSize = 12.sp) }
                    )
                    AssistChip(
                        onClick = {
                            startHour = 6; startMinute = 0
                            endHour = 12; endMinute = 0
                        },
                        label = { Text("Morning", fontSize = 12.sp) }
                    )
                    AssistChip(
                        onClick = {
                            startHour = 22; startMinute = 0
                            endHour = 7; endMinute = 0
                        },
                        label = { Text("Night", fontSize = 12.sp) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(startStr, endStr)
                }
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun BatteryConfigDialog(
    initialLevel: Int = 20,
    initialComparison: String = "below",
    onDismiss: () -> Unit,
    onConfirm: (level: Int, comparison: String) -> Unit
) {
    var level by remember { mutableIntStateOf(initialLevel) }
    var comparison by remember { mutableStateOf(initialComparison) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Battery Level", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "$level%",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Slider(
                    value = level.toFloat(),
                    onValueChange = { level = it.toInt() },
                    valueRange = 1f..100f,
                    steps = 98,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = comparison == "below",
                        onClick = { comparison = "below" },
                        label = { Text("Equal to or below") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = comparison == "above",
                        onClick = { comparison = "above" },
                        label = { Text("Equal to or above") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(level, comparison) }) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun LocationConfigDialog(
    initialIsArrive: Boolean = true,
    initialLocationName: String = "Home",
    initialRadius: Int = 150,
    onDismiss: () -> Unit,
    onConfirm: (isArrive: Boolean, locationName: String, address: String, latitude: Double, longitude: Double, radius: Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isArrive by remember { mutableStateOf(initialIsArrive) }
    var locationName by remember { mutableStateOf(initialLocationName) }
    var addressText by remember { mutableStateOf("") }
    var latitude by remember { mutableDoubleStateOf(0.0) }
    var longitude by remember { mutableDoubleStateOf(0.0) }
    var hasCoordinates by remember { mutableStateOf(false) }
    var radius by remember { mutableIntStateOf(initialRadius) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Address>>(emptyList()) }
    var isLocating by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isLocating = true
            statusMessage = "Locating your position..."
            try {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { loc ->
                        isLocating = false
                        if (loc != null) {
                            latitude = loc.latitude
                            longitude = loc.longitude
                            hasCoordinates = true
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val geocoder = Geocoder(context, Locale.getDefault())
                                    val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                                    if (!addresses.isNullOrEmpty()) {
                                        val addr = addresses[0]
                                        val fullAddr = addr.getAddressLine(0) ?: ""
                                        val featureName = addr.featureName ?: addr.subLocality ?: addr.locality ?: "My Location"
                                        withContext(Dispatchers.Main) {
                                            addressText = fullAddr
                                            if (locationName.isBlank() || locationName == "Home") {
                                                locationName = featureName
                                            }
                                            statusMessage = "Location updated"
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        statusMessage = "Location found: ${String.format("%.4f, %.4f", loc.latitude, loc.longitude)}"
                                    }
                                }
                            }
                        } else {
                            statusMessage = "Unable to fetch GPS. Ensure location is on."
                        }
                    }
                    .addOnFailureListener {
                        isLocating = false
                        statusMessage = "Location request failed"
                    }
            } catch (e: SecurityException) {
                isLocating = false
                statusMessage = "Location permission denied"
            }
        } else {
            statusMessage = "Location permission is required"
        }
    }

    fun requestCurrentLocation() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    fun searchAddress(query: String) {
        if (query.isBlank()) return
        isSearching = true
        statusMessage = "Searching..."
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val results = geocoder.getFromLocationName(query, 4) ?: emptyList()
                withContext(Dispatchers.Main) {
                    searchResults = results
                    isSearching = false
                    statusMessage = if (results.isEmpty()) "No matching address found" else null
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isSearching = false
                    statusMessage = "Search error: ${e.message}"
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Place", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // When I arrive vs When I leave (Samsung One UI)
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

                // Place Name input
                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = { Text("Place name") },
                    placeholder = { Text("e.g. Home, Office, Gym") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick preset chips (LazyRow to prevent any overflow/word wrapping!)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf("Home", "Work", "Gym", "School", "Office", "Cafe")) { preset ->
                        FilterChip(
                            selected = locationName.equals(preset, ignoreCase = true),
                            onClick = {
                                locationName = preset
                                searchAddress(preset)
                            },
                            label = { Text(preset) }
                        )
                    }
                }

                // Location selector options (Current GPS or Address Search)
                Button(
                    onClick = { requestCurrentLocation() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Locating...")
                    } else {
                        Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Use current location", fontWeight = FontWeight.SemiBold)
                    }
                }

                // Address search field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Or search address/city...") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { searchAddress(searchQuery) },
                        enabled = searchQuery.isNotBlank()
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Search Results list
                if (searchResults.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            searchResults.forEach { addr ->
                                val line = addr.getAddressLine(0) ?: "${addr.latitude}, ${addr.longitude}"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            latitude = addr.latitude
                                            longitude = addr.longitude
                                            hasCoordinates = true
                                            addressText = line
                                            val name = addr.featureName ?: addr.locality ?: locationName
                                            if (locationName.isBlank() || locationName == "Home") {
                                                locationName = name
                                            }
                                            searchResults = emptyList()
                                            searchQuery = ""
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(line, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                }
                            }
                        }
                    }
                }

                // Status message
                if (statusMessage != null) {
                    Text(
                        text = statusMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Selected Coordinates Card
                if (hasCoordinates) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (addressText.isNotBlank()) addressText else "$locationName pinned",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${String.format("%.4f", latitude)}°, ${String.format("%.4f", longitude)}°",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Radius slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Target area radius", style = MaterialTheme.typography.bodyMedium)
                        Text("${radius}m", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        isArrive,
                        locationName.trim().ifBlank { if (isArrive) "Home" else "Work" },
                        addressText,
                        latitude,
                        longitude,
                        radius
                    )
                },
                enabled = locationName.isNotBlank()
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun WifiConfigDialog(
    initialName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit
) {
    var networkName by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wi-Fi Network", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Enter the name (SSID) of the Wi-Fi network that triggers this routine.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = networkName,
                    onValueChange = { networkName = it },
                    label = { Text("Network Name (SSID)") },
                    placeholder = { Text("e.g. Home-5G or Office-WiFi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(networkName.trim()) },
                enabled = networkName.isNotBlank()
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

data class PairedBluetoothDeviceItem(
    val name: String,
    val address: String,
    val majorClass: Int = BluetoothClass.Device.Major.UNCATEGORIZED
)

@Composable
fun BluetoothConfigDialog(
    initialName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String, address: String?) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var isManualEntry by remember { mutableStateOf(false) }
    var manualName by remember { mutableStateOf(initialName) }

    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    var permissionGranted by remember { mutableStateOf(hasPermission) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
    }

    val pairedDevices = remember(permissionGranted) {
        if (!permissionGranted) {
            emptyList()
        } else {
            try {
                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val adapter = bluetoothManager?.adapter ?: @Suppress("DEPRECATION") BluetoothAdapter.getDefaultAdapter()
                val devices = adapter?.bondedDevices?.toList() ?: emptyList()
                devices.map { device ->
                    val name = try {
                        device.name
                    } catch (_: SecurityException) {
                        null
                    } ?: "Unknown Device"
                    val address = device.address ?: ""
                    val major = try {
                        device.bluetoothClass?.majorDeviceClass ?: BluetoothClass.Device.Major.UNCATEGORIZED
                    } catch (_: Exception) {
                        BluetoothClass.Device.Major.UNCATEGORIZED
                    }
                    PairedBluetoothDeviceItem(name = name, address = address, majorClass = major)
                }.sortedBy { it.name.lowercase() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    val filteredDevices = remember(pairedDevices, searchQuery) {
        if (searchQuery.isBlank()) pairedDevices else {
            pairedDevices.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.address.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.80f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Bluetooth Device",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isManualEntry) "Enter name manually" else "Select from paired devices",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isManualEntry) {
                    // Manual entry mode
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Enter the name of the Bluetooth device that triggers this routine.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = manualName,
                            onValueChange = { manualName = it },
                            label = { Text("Device Name") },
                            placeholder = { Text("e.g. Galaxy Buds or Car Audio") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { isManualEntry = false }) {
                            Text("Back to list")
                        }
                        Button(
                            onClick = { onConfirm(manualName.trim(), null) },
                            enabled = manualName.isNotBlank(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Done")
                        }
                    }
                } else if (!permissionGranted) {
                    // Need Bluetooth permission
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bluetooth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Bluetooth Permission Needed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "MoodyRoutine needs permission to view your paired Bluetooth devices.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                } else {
                                    permissionGranted = true
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Grant Permission")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { isManualEntry = true }) {
                            Text("Enter name manually instead")
                        }
                    }
                } else {
                    // Paired devices list mode
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search paired devices...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredDevices.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BluetoothSearching,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = if (pairedDevices.isEmpty()) "No paired Bluetooth devices found." else "No matching devices found.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                TextButton(onClick = { isManualEntry = true }) {
                                    Text("Enter name manually")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredDevices, key = { it.address }) { item ->
                                val icon = when (item.majorClass) {
                                    BluetoothClass.Device.Major.AUDIO_VIDEO -> Icons.Rounded.Headphones
                                    BluetoothClass.Device.Major.WEARABLE -> Icons.Rounded.Watch
                                    BluetoothClass.Device.Major.PHONE -> Icons.Rounded.Smartphone
                                    BluetoothClass.Device.Major.COMPUTER -> Icons.Rounded.Laptop
                                    BluetoothClass.Device.Major.PERIPHERAL -> Icons.Rounded.Keyboard
                                    else -> Icons.Rounded.Bluetooth
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onConfirm(item.name, item.address) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (item.address.isNotBlank()) {
                                                Text(
                                                    text = item.address,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Icon(
                                            imageVector = Icons.Rounded.ChevronRight,
                                            contentDescription = "Select",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { isManualEntry = true },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Or enter device name manually", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BluetoothConfigDialog(
    initialName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit
) {
    BluetoothConfigDialog(
        initialName = initialName,
        onDismiss = onDismiss,
        onConfirm = { name, _ -> onConfirm(name) }
    )
}

data class AppItem(val packageName: String, val appName: String)

@Composable
fun AppPickerDialog(
    onDismiss: () -> Unit,
    onAppSelected: (packageName: String, appName: String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)
            val list = resolveInfos.map {
                AppItem(
                    packageName = it.activityInfo.packageName,
                    appName = it.loadLabel(pm).toString()
                )
            }.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }
            apps = list
            isLoading = false
        }
    }

    val filteredApps = if (searchQuery.isBlank()) apps else apps.filter {
        it.appName.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Select Application", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredApps) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onAppSelected(app.packageName, app.appName) }
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Apps,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun VolumeConfigDialog(
    initialVolume: Int = 50,
    title: String = "Volume",
    onDismiss: () -> Unit,
    onConfirm: (volume: Int) -> Unit
) {
    var volume by remember { mutableIntStateOf(initialVolume) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "$volume%",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Slider(
                    value = volume.toFloat(),
                    onValueChange = { volume = it.toInt() },
                    valueRange = 0f..100f,
                    steps = 99,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(volume) }) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun BrightnessConfigDialog(
    initialBrightness: Int = 50,
    onDismiss: () -> Unit,
    onConfirm: (brightness: Int) -> Unit
) {
    var brightness by remember { mutableIntStateOf(initialBrightness) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Screen Brightness", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "$brightness%",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Slider(
                    value = brightness.toFloat(),
                    onValueChange = { brightness = it.toInt() },
                    valueRange = 0f..100f,
                    steps = 99,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(brightness) }) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun NotificationConfigDialog(
    initialTitle: String = "Routine Alert",
    initialMessage: String = "",
    onDismiss: () -> Unit,
    onConfirm: (title: String, message: String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var message by remember { mutableStateOf(initialMessage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Notification", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Notification Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message / Content") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title.trim(), message.trim()) },
                enabled = title.isNotBlank()
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AppAndActionPickerDialog(
    onDismiss: () -> Unit,
    onActionSelected: (packageName: String, appName: String, actionName: String, shortcutUri: String?) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var appGroups by remember { mutableStateOf<List<com.ndev.moodyroutine.util.AppGroup>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var pendingAppGroup by remember { mutableStateOf<com.ndev.moodyroutine.util.AppGroup?>(null) }
    val createShortcutLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val group = pendingAppGroup
        if (result.resultCode == android.app.Activity.RESULT_OK && group != null && result.data != null) {
            @Suppress("DEPRECATION")
            val shortcutIntent = result.data?.getParcelableExtra<Intent>(Intent.EXTRA_SHORTCUT_INTENT)
            val shortcutName = result.data?.getStringExtra(Intent.EXTRA_SHORTCUT_NAME) ?: "Custom shortcut"
            val uri = shortcutIntent?.toUri(Intent.URI_INTENT_SCHEME)
            if (uri != null) {
                onActionSelected(group.packageName, group.appName, shortcutName, uri)
            }
        }
        pendingAppGroup = null
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val list = com.ndev.moodyroutine.util.AppShortcutManager.getAppGroups(context)
            appGroups = list
            isLoading = false
        }
    }

    val filteredGroups = remember(searchQuery, appGroups) {
        if (searchQuery.isBlank()) appGroups
        else appGroups.filter { group ->
            group.appName.contains(searchQuery, ignoreCase = true) ||
            group.actions.any { it.actionName.contains(searchQuery, ignoreCase = true) }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Open an app or do an app action",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps or actions...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredGroups) { group ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Apps,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = group.appName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                for (action in group.actions) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                if (action.isInteractiveCreator && action.creatorComponent != null) {
                                                    pendingAppGroup = group
                                                    val compParts = action.creatorComponent.split("/")
                                                    val createIntent = Intent(Intent.ACTION_CREATE_SHORTCUT).apply {
                                                        setClassName(compParts[0], compParts[1])
                                                    }
                                                    createShortcutLauncher.launch(createIntent)
                                                } else {
                                                    onActionSelected(
                                                        group.packageName,
                                                        group.appName,
                                                        action.actionName,
                                                        action.shortcutUri
                                                    )
                                                }
                                            }
                                            .padding(vertical = 8.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (action.shortcutUri == null) Icons.AutoMirrored.Rounded.Launch else Icons.Rounded.Star,
                                            contentDescription = null,
                                            tint = if (action.shortcutUri == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = action.actionName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun WaitDelayConfigDialog(
    initialSeconds: Int = 5,
    onDismiss: () -> Unit,
    onConfirm: (seconds: Int) -> Unit
) {
    var seconds by remember { mutableIntStateOf(initialSeconds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wait Before Next Action", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (seconds >= 60) "${seconds / 60}m ${seconds % 60}s" else "$seconds seconds",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Slider(
                    value = seconds.toFloat(),
                    onValueChange = { seconds = it.toInt() },
                    valueRange = 1f..120f,
                    steps = 118,
                    modifier = Modifier.fillMaxWidth()
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = listOf(3, 5, 10, 15, 30, 60, 120)
                    items(presets) { s ->
                        AssistChip(
                            onClick = { seconds = s },
                            label = { Text(if (s >= 60) "${s / 60}m" else "${s}s") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(seconds) }) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun MultiAppPickerDialog(
    initialSelected: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onConfirm: (selectedPackages: List<String>, selectedNames: List<String>) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    val selectedPackages = remember { mutableStateListOf<String>().apply { addAll(initialSelected) } }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val list = resolveInfos.map {
                AppItem(
                    packageName = it.activityInfo.packageName,
                    appName = it.loadLabel(pm).toString()
                )
            }.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }
            apps = list
            isLoading = false
        }
    }

    val filteredApps = remember(searchQuery, apps) {
        if (searchQuery.isBlank()) apps else apps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Restrict App Usage",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Selected apps will be blocked while this mode is active",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${selectedPackages.size} selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = {
                        if (selectedPackages.size == apps.size) {
                            selectedPackages.clear()
                        } else {
                            selectedPackages.clear()
                            selectedPackages.addAll(apps.map { it.packageName })
                        }
                    }) {
                        Text(if (selectedPackages.size == apps.size) "Deselect all" else "Select all")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredApps) { app ->
                            val isChecked = selectedPackages.contains(app.packageName)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (isChecked) selectedPackages.remove(app.packageName)
                                        else selectedPackages.add(app.packageName)
                                    }
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedPackages.add(app.packageName)
                                        else selectedPackages.remove(app.packageName)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val names = apps.filter { selectedPackages.contains(it.packageName) }.map { it.appName }
                            onConfirm(selectedPackages.toList(), names)
                        },
                        enabled = selectedPackages.isNotEmpty()
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
fun WallpaperConfigDialog(
    title: String = "Set Wallpaper",
    initialUri: String = "",
    onDismiss: () -> Unit,
    onConfirm: (uriString: String) -> Unit
) {
    var selectedUri by remember { mutableStateOf(initialUri) }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select an image from your device to set as wallpaper.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (selectedUri.isNotBlank()) {
                    Text(
                        text = "Image selected",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Rounded.Wallpaper, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedUri.isBlank()) "Choose Image" else "Change Image")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedUri) },
                enabled = selectedUri.isNotBlank()
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

