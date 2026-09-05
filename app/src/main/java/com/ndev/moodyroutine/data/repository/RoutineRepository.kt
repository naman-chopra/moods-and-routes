package com.ndev.moodyroutine.data.repository

import com.ndev.moodyroutine.data.db.dao.RoutineDao
import com.ndev.moodyroutine.data.db.entity.RoutineEntity
import com.ndev.moodyroutine.data.model.Routine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoutineRepository(private val routineDao: RoutineDao) {

    fun getAllRoutines(): Flow<List<Routine>> {
        return routineDao.getAll().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    fun getRoutineById(id: Long): Flow<Routine?> {
        return routineDao.getById(id).map { it?.toDomainModel() }
    }

    fun getEnabledRoutines(): Flow<List<Routine>> {
        return routineDao.getEnabled().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    suspend fun insertRoutine(routine: Routine): Long {
        return routineDao.insert(RoutineEntity.fromDomainModel(routine))
    }

    suspend fun updateRoutine(routine: Routine) {
        routineDao.update(RoutineEntity.fromDomainModel(routine))
    }

    suspend fun deleteRoutine(routine: Routine) {
        routineDao.delete(RoutineEntity.fromDomainModel(routine))
    }

    suspend fun updateLastTriggered(id: Long, timestamp: Long) {
        routineDao.updateLastTriggered(id, timestamp)
    }

    suspend fun setRoutineEnabled(id: Long, enabled: Boolean) {
        routineDao.setEnabled(id, enabled)
    }
}
