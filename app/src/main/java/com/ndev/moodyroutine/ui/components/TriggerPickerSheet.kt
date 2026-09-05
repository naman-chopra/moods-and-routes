package com.ndev.moodyroutine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ndev.moodyroutine.data.model.TriggerCategory
import com.ndev.moodyroutine.data.model.TriggerConfig
import com.ndev.moodyroutine.data.model.TriggerType
import com.ndev.moodyroutine.ui.dialogs.*
import com.ndev.moodyroutine.ui.util.UiIcons

private val allTriggerTypes = TriggerType.values().toList()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerPickerSheet(
    onDismiss: () -> Unit,
    onTriggerSelected: (TriggerConfig) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var configuringTriggerType by remember { mutableStateOf<TriggerType?>(null) }

    when (configuringTriggerType) {
        TriggerType.TIME_OF_DAY -> {
            TimeConfigDialog(
                onDismiss = { configuringTriggerType = null },
                onConfirm = { time, days ->
                    onTriggerSelected(
                        TriggerConfig(
                            type = TriggerType.TIME_OF_DAY,
                            params = mapOf("time" to time, "days" to days)
                        )
                    )
                    configuringTriggerType = null
                }
            )
        }
        TriggerType.LOCATION_ARRIVE -> {
            LocationConfigDialog(
                isArrive = true,
                onDismiss = { configuringTriggerType = null },
                onConfirm = { name, radius ->
                    onTriggerSelected(
                        TriggerConfig(
                            type = TriggerType.LOCATION_ARRIVE,
                            params = mapOf("locationName" to name, "radius" to radius.toString())
                        )
                    )
                    configuringTriggerType = null
                }
            )
        }
        TriggerType.LOCATION_LEAVE -> {
            LocationConfigDialog(
                isArrive = false,
                onDismiss = { configuringTriggerType = null },
                onConfirm = { name, radius ->
                    onTriggerSelected(
                        TriggerConfig(
                            type = TriggerType.LOCATION_LEAVE,
                            params = mapOf("locationName" to name, "radius" to radius.toString())
                        )
                    )
                    configuringTriggerType = null
                }
            )
        }
        TriggerType.BATTERY_LEVEL -> {
            BatteryConfigDialog(
                onDismiss = { configuringTriggerType = null },
                onConfirm = { level, comparison ->
                    onTriggerSelected(
                        TriggerConfig(
                            type = TriggerType.BATTERY_LEVEL,
                            params = mapOf("level" to level.toString(), "comparison" to comparison)
                        )
                    )
                    configuringTriggerType = null
                }
            )
        }
        TriggerType.WIFI_SPECIFIC_NETWORK -> {
            WifiConfigDialog(
                onDismiss = { configuringTriggerType = null },
                onConfirm = { name ->
                    onTriggerSelected(
                        TriggerConfig(
                            type = TriggerType.WIFI_SPECIFIC_NETWORK,
                            params = mapOf("wifiName" to name)
                        )
                    )
                    configuringTriggerType = null
                }
            )
        }
        TriggerType.BLUETOOTH_SPECIFIC_DEVICE -> {
            BluetoothConfigDialog(
                onDismiss = { configuringTriggerType = null },
                onConfirm = { name ->
                    onTriggerSelected(
                        TriggerConfig(
                            type = TriggerType.BLUETOOTH_SPECIFIC_DEVICE,
                            params = mapOf("deviceName" to name)
                        )
                    )
                    configuringTriggerType = null
                }
            )
        }
        TriggerType.APP_OPENED -> {
            AppPickerDialog(
                onDismiss = { configuringTriggerType = null },
                onAppSelected = { pkg, name ->
                    onTriggerSelected(
                        TriggerConfig(
                            type = TriggerType.APP_OPENED,
                            params = mapOf("packageName" to pkg, "appName" to name)
                        )
                    )
                    configuringTriggerType = null
                }
            )
        }
        TriggerType.APP_CLOSED -> {
            AppPickerDialog(
                onDismiss = { configuringTriggerType = null },
                onAppSelected = { pkg, name ->
                    onTriggerSelected(
                        TriggerConfig(
                            type = TriggerType.APP_CLOSED,
                            params = mapOf("packageName" to pkg, "appName" to name)
                        )
                    )
                    configuringTriggerType = null
                }
            )
        }
        else -> {}
    }

    // Memoize filtering and grouping to prevent scroll jank
    val filteredTypes = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            allTriggerTypes
        } else {
            allTriggerTypes.filter {
                it.displayName.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val grouped = remember(filteredTypes) {
        filteredTypes.groupBy { it.category }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Add condition",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search conditions...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                grouped.forEach { (category, types) ->
                    item(key = "category_${category.name}") {
                        Text(
                            text = when (category) {
                                TriggerCategory.TIME -> "Time"
                                TriggerCategory.LOCATION -> "Place and location"
                                TriggerCategory.BATTERY -> "Battery"
                                TriggerCategory.CONNECTIVITY -> "Connections"
                                TriggerCategory.APP -> "Apps"
                                TriggerCategory.DEVICE -> "Device events"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(types, key = { it.name }) { type ->
                        val icon = UiIcons.getTriggerIcon(type)
                        val color = UiIcons.getTriggerColor(type.category)

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    when (type) {
                                        TriggerType.TIME_OF_DAY,
                                        TriggerType.LOCATION_ARRIVE,
                                        TriggerType.LOCATION_LEAVE,
                                        TriggerType.BATTERY_LEVEL,
                                        TriggerType.WIFI_SPECIFIC_NETWORK,
                                        TriggerType.BLUETOOTH_SPECIFIC_DEVICE,
                                        TriggerType.APP_OPENED,
                                        TriggerType.APP_CLOSED -> {
                                            configuringTriggerType = type
                                        }
                                        else -> {
                                            onTriggerSelected(TriggerConfig(type = type, params = emptyMap()))
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = type.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = type.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
