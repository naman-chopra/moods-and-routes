package com.ndev.moodyroutine.ui.screens.routines

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
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
import com.ndev.moodyroutine.data.model.TriggerType
import com.ndev.moodyroutine.data.repository.RoutineRepository
import com.ndev.moodyroutine.ui.components.ActionPickerSheet
import com.ndev.moodyroutine.ui.components.OsmLocationPickerDialog
import com.ndev.moodyroutine.ui.components.TriggerPickerSheet
import com.ndev.moodyroutine.ui.dialogs.*
import com.ndev.moodyroutine.ui.util.HumanFormatter
import com.ndev.moodyroutine.ui.util.UiIcons
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
    val revertActionsOnExit = MutableStateFlow(true)

    fun loadRoutine(id: Long) {
        routineId = id
        viewModelScope.launch {
            repository.getRoutineById(id).firstOrNull()?.let { routine: Routine ->
                name.value = routine.name
                triggers.value = routine.triggers
                actions.value = routine.actions
                matchType.value = routine.triggerMatchType
                revertActionsOnExit.value = routine.revertActionsOnExit
            }
        }
    }

    fun saveRoutine(onComplete: () -> Unit) {
        val routineName = if (name.value.isNotBlank()) {
            name.value.trim()
        } else {
            generateDefaultName()
        }

        viewModelScope.launch {
            val routine = Routine(
                id = routineId ?: 0,
                name = routineName,
                description = "",
                iconName = "AutoAwesome",
                isEnabled = true,
                triggers = triggers.value,
                actions = actions.value,
                triggerMatchType = matchType.value,
                revertActionsOnExit = revertActionsOnExit.value,
                createdAt = System.currentTimeMillis(),
                lastTriggeredAt = null
            )
            if (routineId == null) repository.insertRoutine(routine) else repository.updateRoutine(routine)
            onComplete()
        }
    }

    private fun generateDefaultName(): String {
        val firstTrigger = triggers.value.firstOrNull()
        return if (firstTrigger != null) {
            val (title, _) = HumanFormatter.formatTrigger(firstTrigger)
            "$title Routine"
        } else {
            "New Routine"
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
    val revertActionsOnExit by viewModel.revertActionsOnExit.collectAsState()

    var showTriggerPicker by remember { mutableStateOf(false) }
    var showActionPicker by remember { mutableStateOf(false) }
    var editingTriggerIndex by remember { mutableStateOf<Int?>(null) }

    val canSave = triggers.isNotEmpty() && actions.isNotEmpty()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (routineIdStr != null) "Edit routine" else "Add routine",
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
                        onClick = { viewModel.saveRoutine { navController.popBackStack() } },
                        enabled = canSave,
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
            // Name Field
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { viewModel.name.value = it },
                            placeholder = { Text("Routine name (e.g. Morning Focus)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // IF SECTION HEADER
            item {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "If",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "What will trigger this routine",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // IF CARDS OR EMPTY ADD BUTTON
            if (triggers.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showTriggerPicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = "Add condition",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Add what will trigger this routine",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Time, place, Wi-Fi, battery, and more",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(triggers) { index, trigger ->
                    val (title, subtitle) = HumanFormatter.formatTrigger(trigger)
                    val icon = UiIcons.getTriggerIcon(trigger.type)
                    val color = UiIcons.getTriggerColor(trigger.type.category)

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { editingTriggerIndex = index }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
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
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = {
                                    val list = triggers.toMutableList()
                                    list.removeAt(index)
                                    viewModel.triggers.value = list
                                }
                            ) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (triggers.size > 1) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = matchType == TriggerMatchType.ALL,
                                    onClick = { viewModel.matchType.value = TriggerMatchType.ALL },
                                    label = { Text("All conditions") },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                FilterChip(
                                    selected = matchType == TriggerMatchType.ANY,
                                    onClick = { viewModel.matchType.value = TriggerMatchType.ANY },
                                    label = { Text("Any condition") },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }

                        TextButton(
                            onClick = { showTriggerPicker = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add condition", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // CONNECTOR LINE
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // THEN SECTION HEADER
            item {
                Column {
                    Text(
                        text = "Then",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "What this routine will do",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // THEN CARDS OR EMPTY ADD BUTTON
            if (actions.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showActionPicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = "Add action",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Add what this routine will do",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Sound, brightness, apps, notifications, and more",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(actions) { index, action ->
                    val (title, subtitle) = HumanFormatter.formatAction(action)
                    val icon = UiIcons.getActionIcon(action.type)
                    val color = UiIcons.getActionColor(action.type.category)

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
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
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = {
                                    val list = actions.toMutableList()
                                    list.removeAt(index)
                                    viewModel.actions.value = list
                                }
                            ) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showActionPicker = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add action", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Revert actions on exit toggle
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "When routine ends",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Control what happens when conditions no longer match",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Revert actions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Restore ringer, volume, and settings back to what they were before the routine ran",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Switch(
                            checked = revertActionsOnExit,
                            onCheckedChange = { viewModel.revertActionsOnExit.value = it }
                        )
                    }
                }
            }

            // Spacing at bottom
            item {
                Spacer(modifier = Modifier.height(32.dp))
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

    editingTriggerIndex?.let { index ->
        val trigger = triggers.getOrNull(index)
        if (trigger != null) {
            when (trigger.type) {
                TriggerType.TIME_OF_DAY -> {
                    TimeConfigDialog(
                        initialTime = trigger.params["time"] ?: "08:00",
                        initialDays = trigger.params["days"] ?: "1,2,3,4,5,6,7",
                        onDismiss = { editingTriggerIndex = null },
                        onConfirm = { time, days ->
                            val list = triggers.toMutableList()
                            list[index] = trigger.copy(params = mapOf("time" to time, "days" to days))
                            viewModel.triggers.value = list
                            editingTriggerIndex = null
                        }
                    )
                }
                TriggerType.TIME_RANGE -> {
                    TimeRangeConfigDialog(
                        initialStartTime = trigger.params["startTime"] ?: "09:00",
                        initialEndTime = trigger.params["endTime"] ?: "17:00",
                        onDismiss = { editingTriggerIndex = null },
                        onConfirm = { startTime, endTime ->
                            val list = triggers.toMutableList()
                            list[index] = trigger.copy(params = mapOf("startTime" to startTime, "endTime" to endTime))
                            viewModel.triggers.value = list
                            editingTriggerIndex = null
                        }
                    )
                }
                TriggerType.DAY_OF_WEEK -> {
                    DayOfWeekConfigDialog(
                        initialDays = trigger.params["days"] ?: "1,2,3,4,5,6,7",
                        onDismiss = { editingTriggerIndex = null },
                        onConfirm = { days ->
                            val list = triggers.toMutableList()
                            list[index] = trigger.copy(params = mapOf("days" to days))
                            viewModel.triggers.value = list
                            editingTriggerIndex = null
                        }
                    )
                }
                TriggerType.LOCATION_ARRIVE,
                TriggerType.LOCATION_LEAVE -> {
                    OsmLocationPickerDialog(
                        initialIsArrive = trigger.type == TriggerType.LOCATION_ARRIVE,
                        initialLocationName = trigger.params["locationName"] ?: "Home",
                        initialRadius = trigger.params["radius"]?.toIntOrNull() ?: 150,
                        onDismiss = { editingTriggerIndex = null },
                        onConfirm = { isArrive, name, address, lat, lng, radius ->
                            val type = if (isArrive) TriggerType.LOCATION_ARRIVE else TriggerType.LOCATION_LEAVE
                            val list = triggers.toMutableList()
                            list[index] = TriggerConfig(
                                type = type,
                                params = buildMap {
                                    put("locationName", name)
                                    if (address.isNotBlank()) put("address", address)
                                    if (lat != 0.0 || lng != 0.0) {
                                        put("latitude", lat.toString())
                                        put("longitude", lng.toString())
                                    }
                                    put("radius", radius.toString())
                                }
                            )
                            viewModel.triggers.value = list
                            editingTriggerIndex = null
                        }
                    )
                }
                TriggerType.BATTERY_LEVEL -> {
                    BatteryConfigDialog(
                        initialLevel = trigger.params["level"]?.toIntOrNull() ?: 20,
                        initialComparison = trigger.params["comparison"] ?: "below",
                        onDismiss = { editingTriggerIndex = null },
                        onConfirm = { level, comparison ->
                            val list = triggers.toMutableList()
                            list[index] = trigger.copy(params = mapOf("level" to level.toString(), "comparison" to comparison))
                            viewModel.triggers.value = list
                            editingTriggerIndex = null
                        }
                    )
                }
                TriggerType.WIFI_SPECIFIC_NETWORK -> {
                    WifiConfigDialog(
                        initialName = trigger.params["wifiName"] ?: "",
                        onDismiss = { editingTriggerIndex = null },
                        onConfirm = { name ->
                            val list = triggers.toMutableList()
                            list[index] = trigger.copy(params = mapOf("wifiName" to name))
                            viewModel.triggers.value = list
                            editingTriggerIndex = null
                        }
                    )
                }
                TriggerType.BLUETOOTH_SPECIFIC_DEVICE -> {
                    BluetoothConfigDialog(
                        initialName = trigger.params["deviceName"] ?: "",
                        onDismiss = { editingTriggerIndex = null },
                        onConfirm = { name, address ->
                            val list = triggers.toMutableList()
                            list[index] = trigger.copy(params = buildMap {
                                put("deviceName", name)
                                if (!address.isNullOrBlank()) put("deviceAddress", address)
                            })
                            viewModel.triggers.value = list
                            editingTriggerIndex = null
                        }
                    )
                }
                TriggerType.APP_OPENED -> {
                    AppPickerDialog(
                        onDismiss = { editingTriggerIndex = null },
                        onAppSelected = { pkg, name ->
                            val list = triggers.toMutableList()
                            list[index] = trigger.copy(params = mapOf("packageName" to pkg, "appName" to name))
                            viewModel.triggers.value = list
                            editingTriggerIndex = null
                        }
                    )
                }
                TriggerType.APP_CLOSED -> {
                    AppPickerDialog(
                        onDismiss = { editingTriggerIndex = null },
                        onAppSelected = { pkg, name ->
                            val list = triggers.toMutableList()
                            list[index] = trigger.copy(params = mapOf("packageName" to pkg, "appName" to name))
                            viewModel.triggers.value = list
                            editingTriggerIndex = null
                        }
                    )
                }
                else -> {
                    editingTriggerIndex = null
                }
            }
        }
    }
}
