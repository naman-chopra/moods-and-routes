package com.ndev.moodyroutine.engine

import android.util.Log
import com.ndev.moodyroutine.data.model.TriggerMatchType
import com.ndev.moodyroutine.data.repository.ModeRepository
import com.ndev.moodyroutine.data.repository.RoutineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AutomationEngine(
    private val routineRepository: RoutineRepository,
    private val modeRepository: ModeRepository,
    private val conditionEvaluator: ConditionEvaluator,
    private val actionExecutor: ActionExecutor
) {
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private val activeTriggers = mutableMapOf<Long, MutableSet<Int>>() // routineId -> satisfied trigger indices

    fun start() {
        if (job != null) return
        job = scope.launch {
            Log.i("MoodyRoutine", "AutomationEngine started")
            EventBus.events.collectLatest { event ->
                Log.d("MoodyRoutine", "Received event: \$event")
                processEventForRoutines(event)
                processEventForModes(event)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        Log.i("MoodyRoutine", "AutomationEngine stopped")
    }

    private suspend fun processEventForRoutines(event: AutomationEvent) {
        routineRepository.getEnabledRoutines().collect { routines ->
            for (routine in routines) {
                var shouldExecute = false
                val satisfiedIndices = activeTriggers.getOrPut(routine.id) { mutableSetOf() }

                for ((index, trigger) in routine.triggers.withIndex()) {
                    if (conditionEvaluator.evaluate(event, trigger)) {
                        satisfiedIndices.add(index)
                    }
                }

                if (routine.triggerMatchType == TriggerMatchType.ANY) {
                    if (satisfiedIndices.isNotEmpty()) {
                        shouldExecute = true
                    }
                } else if (routine.triggerMatchType == TriggerMatchType.ALL) {
                    if (routine.triggers.isNotEmpty() && satisfiedIndices.size >= routine.triggers.size) {
                        shouldExecute = true
                    }
                }

                if (shouldExecute) {
                    Log.i("MoodyRoutine", "Executing routine: \${routine.name}")
                    actionExecutor.executeAll(routine.actions)
                    routineRepository.updateLastTriggered(routine.id, System.currentTimeMillis())
                    satisfiedIndices.clear() // reset after execution
                }
            }
        }
    }

    private suspend fun processEventForModes(event: AutomationEvent) {
        modeRepository.getActiveModes().collect { modes ->
            for (mode in modes) {
                var match = false
                for (trigger in mode.autoTriggers) {
                    if (conditionEvaluator.evaluate(event, trigger)) {
                        match = true
                        break
                    }
                }
                if (match) {
                    Log.i("MoodyRoutine", "Executing mode: \${mode.name}")
                    actionExecutor.executeAll(mode.actions)
                }
            }
        }
    }
}
