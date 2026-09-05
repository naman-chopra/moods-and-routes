package com.ndev.moodyroutine.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ndev.moodyroutine.data.model.ActionConfig
import com.ndev.moodyroutine.data.model.ActionType
import com.ndev.moodyroutine.data.model.TriggerConfig
import com.ndev.moodyroutine.data.model.TriggerMatchType
import com.ndev.moodyroutine.data.model.TriggerType

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTriggerConfigList(value: List<TriggerConfig>): String {
        val type = object : TypeToken<List<TriggerConfig>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toTriggerConfigList(value: String): List<TriggerConfig> {
        val type = object : TypeToken<List<TriggerConfig>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromActionConfigList(value: List<ActionConfig>): String {
        val type = object : TypeToken<List<ActionConfig>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toActionConfigList(value: String): List<ActionConfig> {
        val type = object : TypeToken<List<ActionConfig>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromTriggerMatchType(value: TriggerMatchType): String {
        return value.name
    }

    @TypeConverter
    fun toTriggerMatchType(value: String): TriggerMatchType {
        return TriggerMatchType.valueOf(value)
    }

    @TypeConverter
    fun fromTriggerType(value: TriggerType): String {
        return value.name
    }

    @TypeConverter
    fun toTriggerType(value: String): TriggerType {
        return TriggerType.valueOf(value)
    }

    @TypeConverter
    fun fromActionType(value: ActionType): String {
        return value.name
    }

    @TypeConverter
    fun toActionType(value: String): ActionType {
        return ActionType.valueOf(value)
    }
}
