package de.mm20.launcher2.ui.launcher.focus.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.mm20.launcher2.calendar.CalendarRepository
import de.mm20.launcher2.calendar.providers.CalendarList
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import de.mm20.launcher2.ui.launcher.focus.todoist.TodoistClient
import de.mm20.launcher2.ui.launcher.focus.todoist.TodoistTask
import de.mm20.launcher2.ui.launcher.focus.todoist.TodoistTasksResult
import de.mm20.launcher2.ui.launcher.focus.todoist.tasksForDate
import de.mm20.launcher2.search.CalendarEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.ZoneId

sealed interface FocusPlanTasksState {
    data object Loading : FocusPlanTasksState
    data object TokenMissing : FocusPlanTasksState
    data object InvalidToken : FocusPlanTasksState
    data object ConnectionError : FocusPlanTasksState
    data object ServiceError : FocusPlanTasksState
    data object Empty : FocusPlanTasksState
    data class Ready(val tasks: List<TodoistTask>) : FocusPlanTasksState
}

class FocusPlanVM : ViewModel(), KoinComponent {

    private val calendarRepository: CalendarRepository by inject()
    private val searchUiSettings: SearchUiSettings by inject()

    private var loadTasksJob: Job? = null

    private val _tasksState = MutableStateFlow<FocusPlanTasksState>(FocusPlanTasksState.Loading)
    val tasksState: StateFlow<FocusPlanTasksState> = _tasksState.asStateFlow()

    private val _calendars = MutableStateFlow<List<CalendarList>>(emptyList())
    val calendars: StateFlow<List<CalendarList>> = _calendars.asStateFlow()

    private val _selectedCalendarId = MutableStateFlow<Long?>(null)
    val selectedCalendarId: StateFlow<Long?> = _selectedCalendarId.asStateFlow()

    private val _events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val events: StateFlow<List<CalendarEvent>> = _events.asStateFlow()

    private val _startHour = MutableStateFlow(9)
    val startHour: StateFlow<Int> = _startHour.asStateFlow()

    private val _endHour = MutableStateFlow(22)
    val endHour: StateFlow<Int> = _endHour.asStateFlow()

    private val _durations = MutableStateFlow<List<Int>>(emptyList())
    val durations: StateFlow<List<Int>> = _durations.asStateFlow()

    private var loadEventsJob: Job? = null

    init {
        viewModelScope.launch {
            _selectedCalendarId.value = searchUiSettings.focusPlanSelectedCalendarId.first()
            loadCalendars()
        }
        viewModelScope.launch {
            searchUiSettings.focusPlanTimelineStartHour.collect { _startHour.value = it }
        }
        viewModelScope.launch {
            searchUiSettings.focusPlanTimelineEndHour.collect { _endHour.value = it }
        }
        viewModelScope.launch {
            searchUiSettings.focusPlanDurations.collect { _durations.value = it }
        }
        loadEvents()
    }

    fun refreshTasks() {
        loadTasksJob?.cancel()
        loadTasksJob = viewModelScope.launch {
            val token = searchUiSettings.focusTodoistApiToken.first().trim()
            if (token.isEmpty()) {
                _tasksState.value = FocusPlanTasksState.TokenMissing
                return@launch
            }

            _tasksState.value = FocusPlanTasksState.Loading
            val result = TodoistClient(token).use { it.getActiveTasks() }
            _tasksState.value = when (result) {
                TodoistTasksResult.InvalidToken -> FocusPlanTasksState.InvalidToken
                TodoistTasksResult.ConnectionError -> FocusPlanTasksState.ConnectionError
                TodoistTasksResult.ServiceError -> FocusPlanTasksState.ServiceError
                is TodoistTasksResult.Success -> {
                    val planDate = LocalDate.now(ZoneId.systemDefault()).plusDays(1)
                    val tasks = tasksForDate(result.tasks, planDate)
                    if (tasks.isEmpty()) FocusPlanTasksState.Empty else FocusPlanTasksState.Ready(tasks)
                }
            }
        }
    }

    private fun loadCalendars() {
        viewModelScope.launch {
            calendarRepository.getCalendars("local").collect { lists ->
                _calendars.value = lists
                if (_selectedCalendarId.value == null) {
                    val defaultId = lists.firstNotNullOfOrNull { calendar ->
                        calendar.id.removePrefix("local:").toLongOrNull()
                    }
                    _selectedCalendarId.value = defaultId
                    searchUiSettings.setFocusPlanSelectedCalendarId(defaultId)
                }
            }
        }
    }

    private fun loadEvents() {
        loadEventsJob?.cancel()
        loadEventsJob = viewModelScope.launch {
            val startOfDay = LocalDate.now(ZoneId.systemDefault()).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000L
            calendarRepository.findMany(
                from = startOfDay,
                to = endOfDay
            ).collect { eventList ->
                _events.value = eventList
            }
        }
    }

    fun selectCalendar(id: Long) {
        _selectedCalendarId.value = id
        searchUiSettings.setFocusPlanSelectedCalendarId(id)
    }

    fun scheduleTask(task: TodoistTask, startTime: Long, durationMinutes: Int) {
        viewModelScope.launch {
            val calendarId = _selectedCalendarId.value ?: return@launch
            calendarRepository.insertEvent(
                title = task.content,
                startTime = startTime,
                endTime = startTime + durationMinutes * 60_000L,
                calendarId = calendarId,
            )
            loadEvents()
        }
    }
}
