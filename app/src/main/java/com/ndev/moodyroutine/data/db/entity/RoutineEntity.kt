package com.ndev.moodyroutine.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ndev.moodyroutine.data.model.ActionConfig
import com.ndev.moodyroutine.data.model.Routine
import com.ndev.moodyroutine.data.model.TriggerConfig
import com.ndev.moodyroutine.data.model.TriggerMatchType

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val iconName: String,
    val isEnabled: Boolean,
    val triggers: List<TriggerConfig>,
    val actions: List<ActionConfig>,
    val triggerMatchType: TriggerMatchType,
    val createdAt: Long,
    val lastTriggeredAt: Long?
) {
    fun toDomainModel() = Routine(
        id = id,
        name = name,
        description = description,
        iconName = iconName,
        isEnabled = isEnabled,
        triggers = triggers,
        actions = actions,
        triggerMatchType = triggerMatchType,
        createdAt = createdAt,
        lastTriggeredAt = lastTriggeredAt
    )

    companion object {
        fun fromDomainModel(routine: Routine) = RoutineEntity(
            id = routine.id,
            name = routine.name,
            description = routine.description,
            iconName = routine.iconName,
            isEnabled = routine.isEnabled,
            triggers = routine.triggers,
            actions = routine.actions,
            triggerMatchType = routine.triggerMatchType,
            createdAt = routine.createdAt,
            lastTriggeredAt = routine.lastTriggeredAt
        )
    }
}
