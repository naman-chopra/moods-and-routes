package com.ndev.moodyroutine.ui.screens.modes

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ndev.moodyroutine.data.db.MoodyRoutineDatabase
import com.ndev.moodyroutine.data.model.Mode
import com.ndev.moodyroutine.data.repository.ModeRepository
import com.ndev.moodyroutine.ui.navigation.Screen
import com.ndev.moodyroutine.ui.util.HumanFormatter
import com.ndev.moodyroutine.ui.util.UiIcons
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ModeDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val db = MoodyRoutineDatabase.getInstance(application)
    private val repository = ModeRepository(db.modeDao())
    private val _mode = MutableStateFlow<Mode?>(null)
    val mode: StateFlow<Mode?> = _mode

    fun loadMode(id: Long) {
        viewModelScope.launch { repository.getModeById(id).collect { _mode.value = it } }
    }

    fun deleteMode(onComplete: () -> Unit) {
        val currentMode = _mode.value
        if (currentMode != null) {
            viewModelScope.launch {
                repository.deleteMode(currentMode)
                onComplete()
            }
        }
    }

    fun toggleActive(active: Boolean) {
        val currentMode = _mode.value
        if (currentMode != null) {
            viewModelScope.launch { repository.setModeActive(currentMode.id, active) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeDetailScreen(
    navController: NavController,
    viewModel: ModeDetailViewModel = viewModel()
) {
    val modeId = navController.currentBackStackEntry?.arguments?.getLong("modeId") ?: 0L
    LaunchedEffect(modeId) { viewModel.loadMode(modeId) }
    val mode by viewModel.mode.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val currentMode = mode ?: return

    val modeColor = try {
        Color(android.graphics.Color.parseColor(currentMode.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(currentMode.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.ModeCreate.createRoute(currentMode.id)) }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Hero Card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(modeColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = UiIcons.getModeIcon(currentMode.iconName),
                                contentDescription = null,
                                tint = modeColor,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = currentMode.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        if (currentMode.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentMode.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { viewModel.toggleActive(!currentMode.isActive) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentMode.isActive) modeColor else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth(0.7f).height(48.dp)
                        ) {
                            Text(
                                text = if (currentMode.isActive) "Turn off" else "Turn on",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // TRIGGER SECTION
            if (currentMode.autoTriggers.isNotEmpty()) {
                item {
                    Text(
                        text = "Trigger",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(currentMode.autoTriggers) { trigger ->
                    val (title, subtitle) = HumanFormatter.formatTrigger(trigger)
                    val icon = UiIcons.getTriggerIcon(trigger.type)
                    val color = UiIcons.getTriggerColor(trigger.type.category)

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // ACTION SECTION
            item {
                Text(
                    text = "Action",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (currentMode.actions.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No settings configured for this mode.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(currentMode.actions) { action ->
                    val (title, subtitle) = HumanFormatter.formatAction(action)
                    val icon = UiIcons.getActionIcon(action.type)
                    val color = UiIcons.getActionColor(action.type.category)

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete mode?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete \"${currentMode.name}\"?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteMode { navController.popBackStack() } },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}
