package com.ndev.moodyroutine.ui.screens.modes

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
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
import com.ndev.moodyroutine.ui.util.HumanFormatter
import com.ndev.moodyroutine.ui.util.UiIcons
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ModeCreateViewModel(application: Application) : AndroidViewModel(application) {
    private val db = MoodyRoutineDatabase.getInstance(application)
    private val repository = ModeRepository(db.modeDao())

    var modeId: Long? = null
    val name = MutableStateFlow("")
    val description = MutableStateFlow("")
    val iconName = MutableStateFlow("work")
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
            val existing = modeId?.let { repository.getModeById(it).firstOrNull() }
            val mode = Mode(
                id = modeId ?: 0,
                name = name.value.ifBlank { "Custom Mode" },
                description = description.value,
                iconName = iconName.value,
                colorHex = colorHex.value,
                isActive = existing?.isActive ?: true,
                actions = actions.value,
                autoTriggers = autoTriggers.value,
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
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
    val selectedIcon by viewModel.iconName.collectAsState()
    val selectedColorHex by viewModel.colorHex.collectAsState()
    val actions by viewModel.actions.collectAsState()
    val triggers by viewModel.autoTriggers.collectAsState()

    var showActionPicker by remember { mutableStateOf(false) }
    var showTriggerPicker by remember { mutableStateOf(false) }

    val currentColor = try {
        Color(android.graphics.Color.parseColor(selectedColorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (modeIdStr != null) "Edit mode" else "Create mode",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.saveMode { navController.popBackStack() } },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
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
            // Header Card with Icon preview and Name input
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
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(currentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = UiIcons.getModeIcon(selectedIcon),
                                contentDescription = null,
                                tint = currentColor,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { viewModel.name.value = it },
                            placeholder = { Text("Mode name (e.g. Work, Sleep, Study)") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { viewModel.description.value = it },
                            placeholder = { Text("Description (optional)") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Icon Picker Row
            item {
                Text("Select icon", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val iconOptions = listOf("sleep", "driving", "exercise", "work", "relax", "game", "movie", "book", "custom")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(iconOptions) { iconKey ->
                        val isSelected = selectedIcon.equals(iconKey, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) currentColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) currentColor else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { viewModel.iconName.value = iconKey },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = UiIcons.getModeIcon(iconKey),
                                contentDescription = iconKey,
                                tint = if (isSelected) currentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            // Color Swatch Row
            item {
                Text("Select color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(ModeColors) { color ->
                        val hex = String.format("#%06X", 0xFFFFFF and color.toArgb())
                        val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { viewModel.colorHex.value = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            // TRIGGER SECTION
            item {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text("Trigger", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Optional triggers to activate this mode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (triggers.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showTriggerPicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.AddCircleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                "Set when this mode turns on (time, place...)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(triggers) { index, trigger ->
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
                                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = {
                                val list = triggers.toMutableList()
                                list.removeAt(index)
                                viewModel.autoTriggers.value = list
                            }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { showTriggerPicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.AddCircleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Add condition",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ACTION SECTION
            item {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text("Action", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("What happens when this mode turns on", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (actions.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showActionPicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.AddCircleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                "Add settings (volume, DND, brightness...)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(actions) { index, action ->
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
                                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = {
                                val list = actions.toMutableList()
                                list.removeAt(index)
                                viewModel.actions.value = list
                            }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { showActionPicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.AddCircleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Add action",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(32.dp))
            }
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
