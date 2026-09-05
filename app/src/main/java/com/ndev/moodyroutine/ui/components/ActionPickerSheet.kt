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
import com.ndev.moodyroutine.data.model.ActionCategory
import com.ndev.moodyroutine.data.model.ActionConfig
import com.ndev.moodyroutine.data.model.ActionType
import com.ndev.moodyroutine.ui.dialogs.*
import com.ndev.moodyroutine.ui.util.UiIcons

private val allActionTypes = ActionType.values().toList()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionPickerSheet(
    onDismiss: () -> Unit,
    onActionSelected: (ActionConfig) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var configuringActionType by remember { mutableStateOf<ActionType?>(null) }

    when (configuringActionType) {
        ActionType.SET_VOLUME_RING,
        ActionType.SET_VOLUME_MEDIA,
        ActionType.SET_VOLUME_ALARM,
        ActionType.SET_VOLUME_NOTIFICATION -> {
            val title = when (configuringActionType) {
                ActionType.SET_VOLUME_RING -> "Ring Volume"
                ActionType.SET_VOLUME_MEDIA -> "Media Volume"
                ActionType.SET_VOLUME_ALARM -> "Alarm Volume"
                else -> "Notification Volume"
            }
            VolumeConfigDialog(
                title = title,
                onDismiss = { configuringActionType = null },
                onConfirm = { volume ->
                    configuringActionType?.let { type ->
                        onActionSelected(
                            ActionConfig(type = type, params = mapOf("volume" to volume.toString()))
                        )
                    }
                    configuringActionType = null
                }
            )
        }
        ActionType.SET_BRIGHTNESS -> {
            BrightnessConfigDialog(
                onDismiss = { configuringActionType = null },
                onConfirm = { brightness ->
                    onActionSelected(
                        ActionConfig(
                            type = ActionType.SET_BRIGHTNESS,
                            params = mapOf("brightness" to brightness.toString())
                        )
                    )
                    configuringActionType = null
                }
            )
        }
        ActionType.OPEN_APP -> {
            AppPickerDialog(
                onDismiss = { configuringActionType = null },
                onAppSelected = { pkg, name ->
                    onActionSelected(
                        ActionConfig(
                            type = ActionType.OPEN_APP,
                            params = mapOf("packageName" to pkg, "appName" to name)
                        )
                    )
                    configuringActionType = null
                }
            )
        }
        ActionType.CLOSE_APP -> {
            AppPickerDialog(
                onDismiss = { configuringActionType = null },
                onAppSelected = { pkg, name ->
                    onActionSelected(
                        ActionConfig(
                            type = ActionType.CLOSE_APP,
                            params = mapOf("packageName" to pkg, "appName" to name)
                        )
                    )
                    configuringActionType = null
                }
            )
        }
        ActionType.SEND_NOTIFICATION -> {
            NotificationConfigDialog(
                onDismiss = { configuringActionType = null },
                onConfirm = { title, message ->
                    onActionSelected(
                        ActionConfig(
                            type = ActionType.SEND_NOTIFICATION,
                            params = mapOf("title" to title, "message" to message)
                        )
                    )
                    configuringActionType = null
                }
            )
        }
        else -> {}
    }

    // Memoize filtering and grouping to prevent scroll jank
    val filteredTypes = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            allActionTypes
        } else {
            allActionTypes.filter {
                it.displayName.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val grouped = remember(filteredTypes) {
        filteredTypes.groupBy { it.category }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Add action",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search actions...") },
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
                    item(
                        key = "category_${category.name}",
                        contentType = "category_header"
                    ) {
                        Text(
                            text = when (category) {
                                ActionCategory.SOUND -> "Sound and vibration"
                                ActionCategory.DISPLAY -> "Display"
                                ActionCategory.APPS -> "Apps"
                                ActionCategory.DEVICE -> "Features & device"
                                ActionCategory.NOTIFICATION -> "Notifications"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(
                        items = types,
                        key = { it.name },
                        contentType = { "action_item" }
                    ) { type ->
                        ActionPickerItem(
                            type = type,
                            onClick = {
                                when (type) {
                                    ActionType.SET_VOLUME_RING,
                                    ActionType.SET_VOLUME_MEDIA,
                                    ActionType.SET_VOLUME_ALARM,
                                    ActionType.SET_VOLUME_NOTIFICATION,
                                    ActionType.SET_BRIGHTNESS,
                                    ActionType.OPEN_APP,
                                    ActionType.CLOSE_APP,
                                    ActionType.SEND_NOTIFICATION -> {
                                        configuringActionType = type
                                    }
                                    else -> {
                                        onActionSelected(ActionConfig(type = type, params = emptyMap()))
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ActionPickerItem(
    type: ActionType,
    onClick: () -> Unit
) {
    val icon = remember(type) { UiIcons.getActionIcon(type) }
    val color = remember(type.category) { UiIcons.getActionColor(type.category) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
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
