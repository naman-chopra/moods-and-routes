package com.ndev.moodyroutine.ui.dialogs

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.ndev.moodyroutine.ui.theme.SamsungBlue
import kotlinx.coroutines.Dispatchers
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
                    1 to "S", 2 to "M", 3 to "T", 4 to "W", 5 to "T", 6 to "F", 7 to "S"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dayLabels.forEach { (dayNum, label) ->
                        val isSelected = daysSet.contains(dayNum)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    if (isSelected) daysSet.remove(dayNum) else daysSet.add(dayNum)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Preset chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                            daysSet.clear()
                            daysSet.addAll(listOf(1, 2, 3, 4, 5, 6, 7))
                        },
                        label = { Text("Every day") }
                    )
                    AssistChip(
                        onClick = {
                            daysSet.clear()
                            daysSet.addAll(listOf(2, 3, 4, 5, 6))
                        },
                        label = { Text("Weekdays") }
                    )
                    AssistChip(
                        onClick = {
                            daysSet.clear()
                            daysSet.addAll(listOf(1, 7))
                        },
                        label = { Text("Weekends") }
                    )
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

@Composable
fun BluetoothConfigDialog(
    initialName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit
) {
    var deviceName by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bluetooth Device", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Enter the name of the Bluetooth device that triggers this routine.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Device Name") },
                    placeholder = { Text("e.g. Galaxy Buds or Car Audio") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(deviceName.trim()) },
                enabled = deviceName.isNotBlank()
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
