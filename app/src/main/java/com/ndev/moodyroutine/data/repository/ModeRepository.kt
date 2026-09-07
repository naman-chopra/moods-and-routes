package com.ndev.moodyroutine.data.repository

import com.ndev.moodyroutine.data.db.dao.ModeDao
import com.ndev.moodyroutine.data.db.entity.ModeEntity
import com.ndev.moodyroutine.data.model.Mode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ModeRepository(private val modeDao: ModeDao) {

    fun getAllModes(): Flow<List<Mode>> {
        return modeDao.getAll().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    fun getModeById(id: Long): Flow<Mode?> {
        return modeDao.getById(id).map { it?.toDomainModel() }
    }

    fun getActiveModes(): Flow<List<Mode>> {
        return modeDao.getActive().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    suspend fun insertMode(mode: Mode): Long {
        return modeDao.insert(ModeEntity.fromDomainModel(mode))
    }

    suspend fun updateMode(mode: Mode) {
        modeDao.update(ModeEntity.fromDomainModel(mode))
    }

    suspend fun deleteMode(mode: Mode) {
        modeDao.delete(ModeEntity.fromDomainModel(mode))
    }

    suspend fun setModeEnabled(id: Long, enabled: Boolean) {
        modeDao.setEnabled(id, enabled)
    }

    suspend fun setModeActive(id: Long, active: Boolean) {
        modeDao.setActive(id, active)
    }

    suspend fun deactivateAllModes() {
        modeDao.deactivateAll()
    }
}
