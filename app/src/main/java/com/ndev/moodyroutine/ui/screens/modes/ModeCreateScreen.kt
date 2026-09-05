package com.ndev.moodyroutine.ui.screens.modes

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ndev.moodyroutine.data.db.MoodyRoutineDatabase
import com.ndev.moodyroutine.data.model.ActionConfig
import com.ndev.moodyroutine.data.model.Mode
import com.ndev.moodyroutine.data.model.TriggerConfig
import com.ndev.moodyroutine.data.repository.ModeRepository
import com.ndev.moodyroutine.ui.components.ActionPickerSheet
import com.ndev.moodyroutine.ui.components.TriggerPickerSheet
import com.ndev.moodyroutine.ui.theme.ModeColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ModeCreateViewModel(application: Application) : AndroidViewModel(application) {
    private val db = MoodyRoutineDatabase.getInstance(application)
    private val repository = ModeRepository(db.modeDao())
    
    var modeId: Long? = null
    val name = MutableStateFlow("")
    val description = MutableStateFlow("")
    val iconName = MutableStateFlow("⭐")
    val colorHex = MutableStateFlow(String.format("#%06X", 0xFFFFFF and ModeColors.first().toArgb()))
    val actions = MutableStateFlow<List<ActionConfig>>(emptyList())
    val autoTriggers = MutableStateFlow<List<TriggerConfig>>(emptyList())

    fun loadMode(id: Long) {
        modeId = id
        viewModelScope.launch {
            repository.getModeById(id).firstOrNull()?.let { mode: Mode ->
                name.value = mode.name
                description.value = mode.description
                iconName.value = mode.iconName
                colorHex.value = mode.colorHex
                actions.value = mode.actions
                autoTriggers.value = mode.autoTriggers
            }
        }
    }

    fun saveMode(onComplete: () -> Unit) {
        viewModelScope.launch {
            val mode = Mode(
                id = modeId ?: 0,
                name = name.value,
                description = description.value,
                iconName = iconName.value,
                colorHex = colorHex.value,
                isActive = false,
                actions = actions.value,
                autoTriggers = autoTriggers.value,
                createdAt = System.currentTimeMillis()
            )
            if (modeId == null) repository.insertMode(mode) else repository.updateMode(mode)
            onComplete()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeCreateScreen(
    navController: NavController,
    viewModel: ModeCreateViewModel = viewModel()
) {
    val modeIdStr = navController.currentBackStackEntry?.arguments?.getString("modeId")
    LaunchedEffect(modeIdStr) {
        modeIdStr?.toLongOrNull()?.let { viewModel.loadMode(it) }
    }

    val name by viewModel.name.collectAsState()
    val description by viewModel.description.collectAsState()
    val selectedColorHex by viewModel.colorHex.collectAsState()
    val actions by viewModel.actions.collectAsState()
    val triggers by viewModel.autoTriggers.collectAsState()

    var showActionPicker by remember { mutableStateOf(false) }
    var showTriggerPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (modeIdStr != null) "Edit Mode" else "Create Mode") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.saveMode { navController.popBackStack() } }) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { viewModel.name.value = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { viewModel.description.value = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Color", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ModeColors) { color ->
                    val hex = String.format("#%06X", 0xFFFFFF and color.toArgb())
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { viewModel.colorHex.value = hex }
                    )
                }
            }

            Text("Actions", style = MaterialTheme.typography.titleMedium)
            actions.forEach { action ->
                Text("- ${action.type.name}")
            }
            Button(onClick = { showActionPicker = true }) { Text("Add Action") }

            Text("Auto-Triggers (Optional)", style = MaterialTheme.typography.titleMedium)
            triggers.forEach { trigger ->
                Text("- ${trigger.type.name}")
            }
            Button(onClick = { showTriggerPicker = true }) { Text("Add Auto-Trigger") }
        }
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

    if (showTriggerPicker) {
        TriggerPickerSheet(
            onDismiss = { showTriggerPicker = false },
            onTriggerSelected = { trigger ->
                viewModel.autoTriggers.value = viewModel.autoTriggers.value + trigger
                showTriggerPicker = false
            }
        )
    }
}
