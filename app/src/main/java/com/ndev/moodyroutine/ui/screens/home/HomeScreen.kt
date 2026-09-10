package com.ndev.moodyroutine.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ndev.moodyroutine.data.model.Mode
import com.ndev.moodyroutine.data.model.Routine
import com.ndev.moodyroutine.ui.components.MoodyBottomBar
import com.ndev.moodyroutine.ui.components.MoodyTab
import com.ndev.moodyroutine.ui.navigation.Screen
import com.ndev.moodyroutine.ui.screens.settings.SettingsContent
import com.ndev.moodyroutine.ui.util.HumanFormatter
import com.ndev.moodyroutine.ui.util.UiIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    var currentTab by rememberSaveable { mutableStateOf(MoodyTab.MODES) }
    val modes by viewModel.modes.collectAsState()
    val routines by viewModel.routines.collectAsState()

    var selectedModeIds by remember { mutableStateOf(setOf<Long>()) }
    var selectedRoutineIds by remember { mutableStateOf(setOf<Long>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isSelectionMode = when (currentTab) {
        MoodyTab.MODES -> selectedModeIds.isNotEmpty()
        MoodyTab.ROUTINES -> selectedRoutineIds.isNotEmpty()
        MoodyTab.SETTINGS -> false
    }
    val currentSelectedCount = when (currentTab) {
        MoodyTab.MODES -> selectedModeIds.size
        MoodyTab.ROUTINES -> selectedRoutineIds.size
        MoodyTab.SETTINGS -> 0
    }
    val allCurrentIds = when (currentTab) {
        MoodyTab.MODES -> modes.map { it.id }.toSet()
        MoodyTab.ROUTINES -> routines.map { it.id }.toSet()
        MoodyTab.SETTINGS -> emptySet()
    }
    val isAllSelected = allCurrentIds.isNotEmpty() && currentSelectedCount == allCurrentIds.size

    // Clear selection when changing tabs
    LaunchedEffect(currentTab) {
        selectedModeIds = emptySet()
        selectedRoutineIds = emptySet()
    }

    if (showDeleteConfirm) {
        val itemType = if (currentTab == MoodyTab.MODES) "mode" else "routine"
        val count = currentSelectedCount
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete $count $itemType${if (count > 1) "s" else ""}") },
            text = { Text("Are you sure you want to delete the selected $itemType${if (count > 1) "s" else ""}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentTab == MoodyTab.MODES) {
                            viewModel.bulkDeleteModes(selectedModeIds)
                            selectedModeIds = emptySet()
                        } else {
                            viewModel.bulkDeleteRoutines(selectedRoutineIds)
                            selectedRoutineIds = emptySet()
                        }
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            isSelectionMode -> "$currentSelectedCount selected"
                            currentTab == MoodyTab.MODES -> "Modes"
                            currentTab == MoodyTab.ROUTINES -> "Routines"
                            else -> "Settings"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            selectedModeIds = emptySet()
                            selectedRoutineIds = emptySet()
                        }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear selection")
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        TextButton(onClick = {
                            if (currentTab == MoodyTab.MODES) {
                                selectedModeIds = if (isAllSelected) emptySet() else allCurrentIds
                            } else {
                                selectedRoutineIds = if (isAllSelected) emptySet() else allCurrentIds
                            }
                        }) {
                            Text(
                                text = if (isAllSelected) "Deselect all" else "Select all",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (!isSelectionMode) {
                MoodyBottomBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it }
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode && currentTab != MoodyTab.SETTINGS) {
                FloatingActionButton(
                    onClick = {
                        if (currentTab == MoodyTab.MODES) {
                            navController.navigate(Screen.ModeCreate.createRoute())
                        } else {
                            navController.navigate(Screen.RoutineCreate.createRoute())
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            when (currentTab) {
                MoodyTab.MODES -> {
                    ModesTabContent(
                        modes = modes,
                        selectedIds = selectedModeIds,
                        onModeClick = { mode ->
                            if (selectedModeIds.isNotEmpty()) {
                                selectedModeIds = if (mode.id in selectedModeIds) selectedModeIds - mode.id else selectedModeIds + mode.id
                            } else {
                                navController.navigate(Screen.ModeDetail.createRoute(mode.id))
                            }
                        },
                        onModeLongClick = { mode ->
                            selectedModeIds = if (mode.id in selectedModeIds) selectedModeIds - mode.id else selectedModeIds + mode.id
                        },
                        onModeToggle = { mode, enabled -> viewModel.toggleModeEnabled(mode.id, enabled) }
                    )
                }
                MoodyTab.ROUTINES -> {
                    RoutinesTabContent(
                        routines = routines,
                        selectedIds = selectedRoutineIds,
                        onRoutineClick = { routine ->
                            if (selectedRoutineIds.isNotEmpty()) {
                                selectedRoutineIds = if (routine.id in selectedRoutineIds) selectedRoutineIds - routine.id else selectedRoutineIds + routine.id
                            } else {
                                navController.navigate(Screen.RoutineDetail.createRoute(routine.id))
                            }
                        },
                        onRoutineLongClick = { routine ->
                            selectedRoutineIds = if (routine.id in selectedRoutineIds) selectedRoutineIds - routine.id else selectedRoutineIds + routine.id
                        },
                        onRoutineToggle = { routine, enabled -> viewModel.toggleRoutineEnabled(routine.id, enabled) },
                        onAddRoutineClick = { navController.navigate(Screen.RoutineCreate.createRoute()) }
                    )
                }
                MoodyTab.SETTINGS -> {
                    SettingsContent(
                        modifier = Modifier.fillMaxSize(),
                        onNavigateToLogs = { navController.navigate(Screen.DiagnosticLogs.route) }
                    )
                }
            }

            // Bottom Contextual Action Bar when multi-selecting
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 8.dp,
                    shadowElevation = 10.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Enable Button
                        TextButton(
                            onClick = {
                                if (currentTab == MoodyTab.MODES) {
                                    viewModel.bulkToggleModesEnabled(selectedModeIds, true)
                                    selectedModeIds = emptySet()
                                } else {
                                    viewModel.bulkToggleRoutinesEnabled(selectedRoutineIds, true)
                                    selectedRoutineIds = emptySet()
                                }
                            }
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.PlayCircleOutline, contentDescription = "Enable", tint = MaterialTheme.colorScheme.primary)
                                Text("Enable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        // Disable Button
                        TextButton(
                            onClick = {
                                if (currentTab == MoodyTab.MODES) {
                                    viewModel.bulkToggleModesEnabled(selectedModeIds, false)
                                    selectedModeIds = emptySet()
                                } else {
                                    viewModel.bulkToggleRoutinesEnabled(selectedRoutineIds, false)
                                    selectedRoutineIds = emptySet()
                                }
                            }
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.PauseCircleOutline, contentDescription = "Disable", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Disable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Delete Button
                        TextButton(
                            onClick = { showDeleteConfirm = true }
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                Text("Delete", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModesTabContent(
    modes: List<Mode>,
    selectedIds: Set<Long>,
    onModeClick: (Mode) -> Unit,
    onModeLongClick: (Mode) -> Unit,
    onModeToggle: (Mode, Boolean) -> Unit
) {
    val isSelectionActive = selectedIds.isNotEmpty()

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(modes, key = { it.id }) { mode ->
            val isSelected = mode.id in selectedIds
            val modeColor = try {
                Color(android.graphics.Color.parseColor(mode.colorHex))
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }

            val cardBg by animateColorAsState(
                targetValue = when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    mode.isActive -> modeColor.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surface
                },
                label = "cardBg"
            )

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                tonalElevation = if (mode.isActive || isSelected) 4.dp else 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 2.dp else if (mode.isActive) 1.5.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else if (mode.isActive) modeColor.copy(alpha = 0.5f) else Color.Transparent,
                        shape = RoundedCornerShape(22.dp)
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .combinedClickable(
                        onClick = { onModeClick(mode) },
                        onLongClick = { onModeLongClick(mode) }
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelectionActive) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onModeClick(mode) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(modeColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = UiIcons.getModeIcon(mode.iconName),
                            contentDescription = null,
                            tint = modeColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = mode.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (mode.isActive) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                            }
                        }

                        val subtitleText = when {
                            mode.isActive -> "Active"
                            !mode.isEnabled -> "Disabled"
                            mode.description.isNotBlank() -> mode.description
                            else -> ""
                        }

                        if (subtitleText.isNotBlank()) {
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (mode.isActive) Color(0xFF10B981) else if (!mode.isEnabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (mode.isActive) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }

                    if (!isSelectionActive) {
                        Switch(
                            checked = mode.isEnabled,
                            onCheckedChange = { onModeToggle(mode, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                checkedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(130.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoutinesTabContent(
    routines: List<Routine>,
    selectedIds: Set<Long>,
    onRoutineClick: (Routine) -> Unit,
    onRoutineLongClick: (Routine) -> Unit,
    onRoutineToggle: (Routine, Boolean) -> Unit,
    onAddRoutineClick: () -> Unit
) {
    val isSelectionActive = selectedIds.isNotEmpty()

    if (routines.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = "No routines yet",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Create routines to automate device settings based on what you do, where you go, or your phone's battery and connections.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onAddRoutineClick,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add routine", fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(routines, key = { it.id }) { routine ->
                val isSelected = routine.id in selectedIds
                val firstTrigger = routine.triggers.firstOrNull()
                val icon = firstTrigger?.let { UiIcons.getTriggerIcon(it.type) } ?: Icons.Rounded.AutoAwesome
                val color = firstTrigger?.let { UiIcons.getTriggerColor(it.type.category) } ?: MaterialTheme.colorScheme.primary

                val triggerSummary = if (firstTrigger != null) {
                    val (_, subtitle) = HumanFormatter.formatTrigger(firstTrigger)
                    subtitle
                } else {
                    "Manual routine"
                }

                val actionSummary = routine.actions.joinToString(", ") { action ->
                    val (title, _) = HumanFormatter.formatAction(action)
                    title
                }.ifBlank { "No actions" }

                val cardBg by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
                    label = "cardBg"
                )

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = cardBg,
                    tonalElevation = if (isSelected) 4.dp else 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(22.dp)
                        )
                        .clip(RoundedCornerShape(22.dp))
                        .combinedClickable(
                            onClick = { onRoutineClick(routine) },
                            onLongClick = { onRoutineLongClick(routine) }
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelectionActive) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onRoutineClick(routine) },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = routine.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (routine.isActive) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            if (routine.isActive) {
                                Text(
                                    text = "Active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = "If $triggerSummary",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                            Text(
                                text = "Then $actionSummary",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        if (!isSelectionActive) {
                            Switch(
                                checked = routine.isEnabled,
                                onCheckedChange = { onRoutineToggle(routine, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    checkedBorderColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(130.dp))
            }
        }
    }
}
