package de.mm20.launcher2.ui.launcher.focus

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class FocusHomeCommandType {
    StartSession,
    ResumeFocus,
    OpenTodayPlan,
}

data class FocusHomeCommand(
    val id: String = UUID.randomUUID().toString(),
    val type: FocusHomeCommandType,
)

object FocusHomeCommandCenter {
    private val _pendingCommand = MutableStateFlow<FocusHomeCommand?>(null)
    val pendingCommand: StateFlow<FocusHomeCommand?> = _pendingCommand.asStateFlow()

    fun dispatch(type: FocusHomeCommandType) {
        _pendingCommand.value = FocusHomeCommand(type = type)
    }

    fun consume(id: String) {
        if (_pendingCommand.value?.id == id) {
            _pendingCommand.value = null
        }
    }
}
