package com.ndev.moodyroutine.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ndev.moodyroutine.data.db.dao.ModeDao
import com.ndev.moodyroutine.data.db.dao.RoutineDao
import com.ndev.moodyroutine.data.db.entity.ModeEntity
import com.ndev.moodyroutine.data.db.entity.RoutineEntity
import com.ndev.moodyroutine.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [RoutineEntity::class, ModeEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class MoodyRoutineDatabase : RoomDatabase() {

    abstract fun routineDao(): RoutineDao
    abstract fun modeDao(): ModeDao

    companion object {
        @Volatile
        private var INSTANCE: MoodyRoutineDatabase? = null

        fun getInstance(context: Context): MoodyRoutineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MoodyRoutineDatabase::class.java,
                    "moody_routine_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                seedInitialData(getInstance(context))
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedInitialData(database: MoodyRoutineDatabase) {
            val modeDao = database.modeDao()
            val defaultModes = listOf(
                Mode(
                    id = 1,
                    name = "Sleep",
                    description = "Turn on sleep mode to get ready for bed and sleep without interruptions",
                    iconName = "sleep",
                    colorHex = "#7F56D9",
                    isActive = false,
                    actions = listOf(
                        ActionConfig(ActionType.SET_DND_ON),
                        ActionConfig(ActionType.ENABLE_DARK_MODE),
                        ActionConfig(ActionType.SET_BRIGHTNESS, mapOf("brightness" to "20"))
                    ),
                    autoTriggers = listOf(
                        TriggerConfig(TriggerType.TIME_OF_DAY, mapOf("time" to "22:30", "days" to "1,2,3,4,5,6,7"))
                    )
                ),
                Mode(
                    id = 2,
                    name = "Driving",
                    description = "Keep distractions to a minimum while driving",
                    iconName = "driving",
                    colorHex = "#12B76A",
                    isActive = false,
                    actions = listOf(
                        ActionConfig(ActionType.SET_DND_PRIORITY_ONLY),
                        ActionConfig(ActionType.SET_VOLUME_MEDIA, mapOf("volume" to "80"))
                    ),
                    autoTriggers = listOf(
                        TriggerConfig(TriggerType.BLUETOOTH_CONNECTED)
                    )
                ),
                Mode(
                    id = 3,
                    name = "Exercise",
                    description = "Focus on your workout and track your health",
                    iconName = "exercise",
                    colorHex = "#F79009",
                    isActive = false,
                    actions = listOf(
                        ActionConfig(ActionType.SET_VOLUME_MEDIA, mapOf("volume" to "70")),
                        ActionConfig(ActionType.SET_DND_PRIORITY_ONLY)
                    ),
                    autoTriggers = listOf(
                        TriggerConfig(TriggerType.HEADPHONE_CONNECTED)
                    )
                ),
                Mode(
                    id = 4,
                    name = "Relax",
                    description = "Take time to unwind and disconnect",
                    iconName = "relax",
                    colorHex = "#06AED4",
                    isActive = false,
                    actions = listOf(
                        ActionConfig(ActionType.SET_VOLUME_RING, mapOf("volume" to "30")),
                        ActionConfig(ActionType.SET_BRIGHTNESS, mapOf("brightness" to "40"))
                    )
                ),
                Mode(
                    id = 5,
                    name = "Work",
                    description = "Stay focused and minimize distractions at work",
                    iconName = "work",
                    colorHex = "#3872FF",
                    isActive = false,
                    actions = listOf(
                        ActionConfig(ActionType.SET_RINGER_VIBRATE),
                        ActionConfig(ActionType.SET_DND_PRIORITY_ONLY)
                    ),
                    autoTriggers = listOf(
                        TriggerConfig(TriggerType.TIME_OF_DAY, mapOf("time" to "09:00", "days" to "2,3,4,5,6"))
                    )
                )
            )

            defaultModes.forEach { mode ->
                modeDao.insert(ModeEntity.fromDomainModel(mode))
            }
        }
    }
}
