package com.ndev.moodyroutine.data.model

data class TriggerConfig(
    val type: TriggerType,
    val params: Map<String, String> = emptyMap()
)

data class ActionConfig(
    val type: ActionType,
    val params: Map<String, String> = emptyMap()
)

enum class TriggerMatchType { ANY, ALL }

data class Routine(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val iconName: String = "AutoAwesome",
    val isEnabled: Boolean = true,
    val triggers: List<TriggerConfig>,
    val actions: List<ActionConfig>,
    val triggerMatchType: TriggerMatchType = TriggerMatchType.ANY,
    val createdAt: Long = System.currentTimeMillis(),
    val lastTriggeredAt: Long? = null
)

data class Mode(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val iconName: String = "Palette",
    val colorHex: String = "#6750A4",
    val isEnabled: Boolean = true,
    val isActive: Boolean = false,
    val actions: List<ActionConfig>,
    val autoTriggers: List<TriggerConfig> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
