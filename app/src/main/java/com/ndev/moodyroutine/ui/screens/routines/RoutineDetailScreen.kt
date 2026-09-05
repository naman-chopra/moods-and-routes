package com.ndev.moodyroutine.ui.screens.routines

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ndev.moodyroutine.data.db.MoodyRoutineDatabase
import com.ndev.moodyroutine.data.model.Routine
import com.ndev.moodyroutine.data.repository.RoutineRepository
import com.ndev.moodyroutine.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RoutineDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val db = MoodyRoutineDatabase.getInstance(application)
    private val repository = RoutineRepository(db.routineDao())
    private val _routine = MutableStateFlow<Routine?>(null)
    val routine: StateFlow<Routine?> = _routine

    fun loadRoutine(id: Long) {
        viewModelScope.launch { repository.getRoutineById(id).collect { _routine.value = it } }
    }

    fun deleteRoutine(onComplete: () -> Unit) {
        val currentRoutine = _routine.value
        if (currentRoutine != null) {
            viewModelScope.launch { 
                repository.deleteRoutine(currentRoutine)
                onComplete()
            }
        }
    }
    
    fun toggleEnabled(enabled: Boolean) {
        val currentRoutine = _routine.value
        if (currentRoutine != null) {
            viewModelScope.launch { repository.setRoutineEnabled(currentRoutine.id, enabled) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(
    navController: NavController,
    viewModel: RoutineDetailViewModel = viewModel()
) {
    val routineId = navController.currentBackStackEntry?.arguments?.getLong("routineId") ?: 0L
    LaunchedEffect(routineId) { viewModel.loadRoutine(routineId) }
    val routine by viewModel.routine.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val currentRoutine = routine
    if (currentRoutine == null) return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentRoutine.name) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.RoutineCreate.createRoute(currentRoutine.id)) }) {
                        Icon(Icons.Default.Edit, "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, "Delete") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Enabled", style = MaterialTheme.typography.titleMedium)
                Switch(checked = currentRoutine.isEnabled, onCheckedChange = { viewModel.toggleEnabled(it) })
            }
            Spacer(modifier = Modifier.height(32.dp))
            
            Text("IF", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            currentRoutine.triggers.forEach { trigger ->
                Text("- ${trigger.type.name}", modifier = Modifier.padding(vertical = 4.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("THEN", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            currentRoutine.actions.forEach { action ->
                Text("- ${action.type.name}", modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Routine") },
            text = { Text("Are you sure you want to delete this routine?") },
            confirmButton = { TextButton(onClick = { viewModel.deleteRoutine { navController.popBackStack() } }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
}
