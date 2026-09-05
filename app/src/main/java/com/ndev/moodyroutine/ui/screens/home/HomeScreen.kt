package com.ndev.moodyroutine.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ndev.moodyroutine.data.model.Mode
import com.ndev.moodyroutine.data.model.Routine
import com.ndev.moodyroutine.ui.navigation.Screen

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
        topBar = {
            LargeTopAppBar(
                title = { Text("MoodyRoutine", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
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
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Modes") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Routines") }
                )
            }
            
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
                    onRoutineToggle = { routine, enabled -> viewModel.toggleRoutineEnabled(routine.id, enabled) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModesTabContent(
    modes: List<Mode>,
    onModeClick: (Mode) -> Unit,
    onModeToggle: (Mode, Boolean) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(modes) { mode ->
            val color = try { Color(android.graphics.Color.parseColor(mode.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .combinedClickable(
                        onClick = { onModeClick(mode) },
                        onLongClick = { onModeToggle(mode, !mode.isActive) }
                    ),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (mode.isActive) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(mode.iconName, style = MaterialTheme.typography.headlineMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(mode.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = mode.isActive,
                            onCheckedChange = { onModeToggle(mode, it) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesTabContent(
    routines: List<Routine>,
    onRoutineClick: (Routine) -> Unit,
    onRoutineToggle: (Routine, Boolean) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(routines) { routine ->
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                onClick = { onRoutineClick(routine) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(routine.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (routine.triggers.isNotEmpty()) "If ${routine.triggers.first().type.name}" else "No triggers",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = routine.isEnabled,
                        onCheckedChange = { onRoutineToggle(routine, it) }
                    )
                }
            }
        }
    }
}
