package com.ndev.moodyroutine.ui.screens.routines

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.ndev.moodyroutine.data.model.ActionConfig
import com.ndev.moodyroutine.data.model.Routine
import com.ndev.moodyroutine.data.model.TriggerConfig
import com.ndev.moodyroutine.data.model.TriggerMatchType
import com.ndev.moodyroutine.data.repository.RoutineRepository
import com.ndev.moodyroutine.ui.components.ActionPickerSheet
import com.ndev.moodyroutine.ui.components.TriggerPickerSheet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class RoutineCreateViewModel(application: Application) : AndroidViewModel(application) {
    private val db = MoodyRoutineDatabase.getInstance(application)
    private val repository = RoutineRepository(db.routineDao())
    
    var routineId: Long? = null
    val name = MutableStateFlow("")
    val triggers = MutableStateFlow<List<TriggerConfig>>(emptyList())
    val actions = MutableStateFlow<List<ActionConfig>>(emptyList())
    val matchType = MutableStateFlow(TriggerMatchType.ALL)

    fun loadRoutine(id: Long) {
        routineId = id
        viewModelScope.launch {
            repository.getRoutineById(id).firstOrNull()?.let { routine: Routine ->
                name.value = routine.name
                triggers.value = routine.triggers
                actions.value = routine.actions
                matchType.value = routine.triggerMatchType
            }
        }
    }

    fun saveRoutine(onComplete: () -> Unit) {
        viewModelScope.launch {
            val routine = Routine(
                id = routineId ?: 0,
                name = name.value,
                description = "",
                iconName = "⚙️",
                isEnabled = true,
                triggers = triggers.value,
                actions = actions.value,
                triggerMatchType = matchType.value,
                createdAt = System.currentTimeMillis(),
                lastTriggeredAt = null
            )
            if (routineId == null) repository.insertRoutine(routine) else repository.updateRoutine(routine)
            onComplete()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineCreateScreen(
    navController: NavController,
    viewModel: RoutineCreateViewModel = viewModel()
) {
    val routineIdStr = navController.currentBackStackEntry?.arguments?.getString("routineId")
    LaunchedEffect(routineIdStr) {
        routineIdStr?.toLongOrNull()?.let { viewModel.loadRoutine(it) }
    }

    val name by viewModel.name.collectAsState()
    val triggers by viewModel.triggers.collectAsState()
    val actions by viewModel.actions.collectAsState()
    val matchType by viewModel.matchType.collectAsState()

    var showTriggerPicker by remember { mutableStateOf(false) }
    var showActionPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (routineIdStr != null) "Edit Routine" else "Create Routine") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    TextButton(onClick = { viewModel.saveRoutine { navController.popBackStack() } }) { Text("Save") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { viewModel.name.value = it },
                    label = { Text("Routine Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                Text("IF", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Match:")
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = matchType == TriggerMatchType.ALL,
                        onClick = { viewModel.matchType.value = TriggerMatchType.ALL },
                        label = { Text("ALL triggers") }
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = matchType == TriggerMatchType.ANY,
                        onClick = { viewModel.matchType.value = TriggerMatchType.ANY },
                        label = { Text("ANY trigger") }
                    )
                }
            }
            
            items(triggers) { trigger ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(trigger.type.name, modifier = Modifier.padding(16.dp))
                }
            }
            
            item {
                Button(onClick = { showTriggerPicker = true }) { Text("Add Trigger") }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Text("THEN", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            }

            items(actions) { action ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(action.type.name, modifier = Modifier.padding(16.dp))
                }
            }

            item {
                Button(onClick = { showActionPicker = true }) { Text("Add Action") }
            }
        }
    }

    if (showTriggerPicker) {
        TriggerPickerSheet(
            onDismiss = { showTriggerPicker = false },
            onTriggerSelected = { trigger ->
                viewModel.triggers.value = viewModel.triggers.value + trigger
                showTriggerPicker = false
            }
        )
    }

    if (showActionPicker) {
        ActionPickerSheet(
            onDismiss = { showActionPicker = false },
            onActionSelected = { action ->
                viewModel.actions.value = viewModel.actions.value + action
                showActionPicker = false
            }
        )
    }
}
