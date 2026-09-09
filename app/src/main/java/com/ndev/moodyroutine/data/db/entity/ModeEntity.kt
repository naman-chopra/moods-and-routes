package com.ndev.moodyroutine.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ndev.moodyroutine.data.model.ActionConfig
import com.ndev.moodyroutine.data.model.Mode
import com.ndev.moodyroutine.data.model.TriggerConfig

@Entity(tableName = "modes")
data class ModeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val iconName: String,
    val colorHex: String,
    val isEnabled: Boolean = true,
    val isActive: Boolean = false,
    val actions: List<ActionConfig>,
    val autoTriggers: List<TriggerConfig>,
    @ColumnInfo(defaultValue = "1")
    val revertActionsOnExit: Boolean = true,
    val createdAt: Long
) {
    fun toDomainModel() = Mode(
        id = id,
        name = name,
        description = description,
        iconName = iconName,
        colorHex = colorHex,
        isEnabled = isEnabled,
        isActive = isActive,
        actions = actions,
        autoTriggers = autoTriggers,
        revertActionsOnExit = revertActionsOnExit,
        createdAt = createdAt
    )

    companion object {
        fun fromDomainModel(mode: Mode) = ModeEntity(
            id = mode.id,
            name = mode.name,
            description = mode.description,
            iconName = mode.iconName,
            colorHex = mode.colorHex,
            isEnabled = mode.isEnabled,
            isActive = mode.isActive,
            actions = mode.actions,
            autoTriggers = mode.autoTriggers,
            revertActionsOnExit = mode.revertActionsOnExit,
            createdAt = mode.createdAt
        )
    }
}
