package com.ndev.moodyroutine.engine

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object EventBus {
    private val _events = MutableSharedFlow<AutomationEvent>(replay = 0, extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    suspend fun emit(event: AutomationEvent) {
        _events.emit(event)
    }
}
