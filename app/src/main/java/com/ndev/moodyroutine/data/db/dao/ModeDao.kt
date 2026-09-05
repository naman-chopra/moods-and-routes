package com.ndev.moodyroutine.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ndev.moodyroutine.data.db.entity.ModeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModeDao {
    @Query("SELECT * FROM modes ORDER BY createdAt DESC")
    fun getAll(): Flow<List<ModeEntity>>

    @Query("SELECT * FROM modes WHERE id = :id")
    fun getById(id: Long): Flow<ModeEntity?>

    @Query("SELECT * FROM modes WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActive(): Flow<List<ModeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mode: ModeEntity): Long

    @Update
    suspend fun update(mode: ModeEntity)

    @Delete
    suspend fun delete(mode: ModeEntity)

    @Query("UPDATE modes SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Query("UPDATE modes SET isActive = 0")
    suspend fun deactivateAll()
}
