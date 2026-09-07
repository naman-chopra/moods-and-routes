package com.ndev.moodyroutine.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ndev.moodyroutine.data.db.MoodyRoutineDatabase
import com.ndev.moodyroutine.data.model.Mode
import com.ndev.moodyroutine.data.model.Routine
import com.ndev.moodyroutine.data.repository.ModeRepository
import com.ndev.moodyroutine.data.repository.RoutineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = MoodyRoutineDatabase.getInstance(application)
    private val modeRepository = ModeRepository(db.modeDao())
    private val routineRepository = RoutineRepository(db.routineDao())

    private val _modes = MutableStateFlow<List<Mode>>(emptyList())
    val modes: StateFlow<List<Mode>> = _modes

    private val _routines = MutableStateFlow<List<Routine>>(emptyList())
    val routines: StateFlow<List<Routine>> = _routines

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

    init {
        viewModelScope.launch {
            modeRepository.getAllModes().catch { }.collect { _modes.value = it }
        }
        viewModelScope.launch {
            routineRepository.getAllRoutines().catch { }.collect { _routines.value = it }
        }
    }

    fun toggleModeEnabled(modeId: Long, enabled: Boolean) {
        viewModelScope.launch {
            modeRepository.setModeEnabled(modeId, enabled)
            if (!enabled) {
                modeRepository.setModeActive(modeId, false)
            }
        }
    }

    fun toggleRoutineEnabled(routineId: Long, enabled: Boolean) {
        viewModelScope.launch { routineRepository.setRoutineEnabled(routineId, enabled) }
    }

    fun deleteMode(mode: Mode) {
        viewModelScope.launch { modeRepository.deleteMode(mode) }
    }

    fun deleteRoutine(routine: Routine) {
        viewModelScope.launch { routineRepository.deleteRoutine(routine) }
    }

    fun bulkToggleModesEnabled(modeIds: Set<Long>, enabled: Boolean) {
        viewModelScope.launch {
            modeIds.forEach {
                modeRepository.setModeEnabled(it, enabled)
                if (!enabled) {
                    modeRepository.setModeActive(it, false)
                }
            }
        }
    }

    fun bulkToggleRoutinesEnabled(routineIds: Set<Long>, enabled: Boolean) {
        viewModelScope.launch {
            routineIds.forEach { routineRepository.setRoutineEnabled(it, enabled) }
        }
    }

    fun bulkDeleteModes(modeIds: Set<Long>) {
        viewModelScope.launch {
            val currentModes = _modes.value
            currentModes.filter { it.id in modeIds }.forEach { modeRepository.deleteMode(it) }
        }
    }

    fun bulkDeleteRoutines(routineIds: Set<Long>) {
        viewModelScope.launch {
            val currentRoutines = _routines.value
            currentRoutines.filter { it.id in routineIds }.forEach { routineRepository.deleteRoutine(it) }
        }
    }
}
