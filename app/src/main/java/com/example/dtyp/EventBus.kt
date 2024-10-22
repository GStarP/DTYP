package com.example.dtyp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

// https://asissuthar.medium.com/simple-global-event-bus-using-kotlin-sharedflow-and-koin-4b6fa8cb1a37
@Singleton
class EventBus @Inject constructor() {
    private val _eventFlow = MutableSharedFlow<Event>()

    fun subscribe(scope: CoroutineScope, block: suspend (Event) -> Unit) = _eventFlow.onEach(block).launchIn(scope)
    suspend fun emit(appEvent: Event) = _eventFlow.emit(appEvent)
}

sealed class Event {
    data class CommonEvent(val type: EventType, val data: String?): Event()
}

enum class EventType {
    ServiceStart,
    ServiceStop
}