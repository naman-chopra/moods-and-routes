package com.ndev.moodyroutine.ui.screens.modes

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ndev.moodyroutine.data.db.MoodyRoutineDatabase
import com.ndev.moodyroutine.data.model.Mode
import com.ndev.moodyroutine.data.repository.ModeRepository
import com.ndev.moodyroutine.ui.navigation.Screen
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

    val currentMode = mode
    if (currentMode == null) return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentMode.name) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.ModeCreate.createRoute(currentMode.id)) }) {
                        Icon(Icons.Default.Edit, "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Delete")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            val color = try { Color(android.graphics.Color.parseColor(currentMode.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(color).align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Text(currentMode.iconName, style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(currentMode.description, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(32.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Active State", style = MaterialTheme.typography.titleMedium)
                Switch(checked = currentMode.isActive, onCheckedChange = { viewModel.toggleActive(it) })
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text("Actions", style = MaterialTheme.typography.titleMedium)
            currentMode.actions.forEach { action ->
                Text("- ${action.type.name}", modifier = Modifier.padding(vertical = 4.dp))
            }
            if (currentMode.autoTriggers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Auto Triggers", style = MaterialTheme.typography.titleMedium)
                currentMode.autoTriggers.forEach { trigger ->
                    Text("- ${trigger.type.name}", modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Mode") },
            text = { Text("Are you sure you want to delete this mode?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteMode { navController.popBackStack() } }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}
