package com.ndev.moodyroutine.ui.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ndev.moodyroutine.data.model.Mode
import com.ndev.moodyroutine.data.model.Routine
import com.ndev.moodyroutine.ui.navigation.Screen
import com.ndev.moodyroutine.ui.util.HumanFormatter
import com.ndev.moodyroutine.ui.util.UiIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val modes by viewModel.modes.collectAsState()
    val routines by viewModel.routines.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Modes and Routines",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTabIndex == 0) {
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
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Samsung One UI Tab Switcher
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "Modes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "Routines",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedTabIndex == 0) {
                ModesTabContent(
                    modes = modes,
                    onModeClick = { navController.navigate(Screen.ModeDetail.createRoute(it.id)) },
                    onModeToggle = { mode, active -> viewModel.toggleModeActive(mode.id, active) }
                )
            } else {
                RoutinesTabContent(
                    routines = routines,
                    onRoutineClick = { navController.navigate(Screen.RoutineDetail.createRoute(it.id)) },
                    onRoutineToggle = { routine, enabled -> viewModel.toggleRoutineEnabled(routine.id, enabled) },
                    onAddRoutineClick = { navController.navigate(Screen.RoutineCreate.createRoute()) }
                )
            }
        }
    }
}

@Composable
fun ModesTabContent(
    modes: List<Mode>,
    onModeClick: (Mode) -> Unit,
    onModeToggle: (Mode, Boolean) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(modes, key = { it.id }) { mode ->
            val modeColor = try {
                Color(android.graphics.Color.parseColor(mode.colorHex))
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }

            val cardBg by animateColorAsState(
                targetValue = if (mode.isActive) modeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                label = "cardBg"
            )

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                tonalElevation = if (mode.isActive) 4.dp else 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (mode.isActive) 1.5.dp else 0.dp,
                        color = if (mode.isActive) modeColor.copy(alpha = 0.5f) else Color.Transparent,
                        shape = RoundedCornerShape(22.dp)
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onModeClick(mode) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        Text(
                            text = mode.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (mode.isActive) "Active" else mode.description.ifBlank { "Tap to set up" },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (mode.isActive) modeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (mode.isActive) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }

                    Switch(
                        checked = mode.isActive,
                        onCheckedChange = { onModeToggle(mode, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = modeColor
                        )
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
fun RoutinesTabContent(
    routines: List<Routine>,
    onRoutineClick: (Routine) -> Unit,
    onRoutineToggle: (Routine, Boolean) -> Unit,
    onAddRoutineClick: () -> Unit
) {
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

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { onRoutineClick(routine) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                            Text(
                                text = routine.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
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

                        Switch(
                            checked = routine.isEnabled,
                            onCheckedChange = { onRoutineToggle(routine, it) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}
