package com.ndev.moodyroutine.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ndev.moodyroutine.data.db.dao.ModeDao
import com.ndev.moodyroutine.data.db.dao.RoutineDao
import com.ndev.moodyroutine.data.db.entity.ModeEntity
import com.ndev.moodyroutine.data.db.entity.RoutineEntity

@Database(entities = [RoutineEntity::class, ModeEntity::class], version = 1, exportSchema = false)
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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
