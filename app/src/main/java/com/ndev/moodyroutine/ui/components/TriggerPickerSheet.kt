package com.ndev.moodyroutine.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ndev.moodyroutine.data.model.TriggerConfig
import com.ndev.moodyroutine.data.model.TriggerType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerPickerSheet(
    onDismiss: () -> Unit,
    onTriggerSelected: (TriggerConfig) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item {
                Text("Select Trigger", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
            }
            items(TriggerType.values()) { type ->
                ListItem(
                    headlineContent = { Text(type.displayName) },
                    supportingContent = { Text(type.description) },
                    leadingContent = { Text(type.iconName) },
                    modifier = Modifier.clickable {
                        onTriggerSelected(TriggerConfig(type = type, params = emptyMap()))
                    }
                )
                Divider()
            }
        }
    }
}
