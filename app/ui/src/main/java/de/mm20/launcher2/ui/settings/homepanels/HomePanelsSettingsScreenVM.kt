package de.mm20.launcher2.ui.settings.homepanels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.mm20.launcher2.calendar.CalendarRepository
import de.mm20.launcher2.calendar.providers.CalendarList
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class HomePanelsSettingsScreenVM : ViewModel(), KoinComponent {
    private val searchUiSettings: SearchUiSettings by inject()
    private val calendarRepository: CalendarRepository by inject()

    val calendars = calendarRepository.getCalendars()
        .map { calendars ->
            calendars.sortedWith(compareBy<CalendarList>({ it.name.lowercase() }, { it.owner ?: "" }, { it.providerId }))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val dailyScheduleEnabled = searchUiSettings.focusDailyScheduleEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val selectedDailyScheduleCalendar = combine(
        calendars,
        searchUiSettings.focusDailyScheduleCalendarId,
    ) { calendars, selectedId ->
        calendars.firstOrNull { it.id == selectedId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val upcomingEventsCalendarIds = searchUiSettings.focusUpcomingEventsCalendarIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptySet())

    val selectedUpcomingCalendars = combine(
        calendars,
        upcomingEventsCalendarIds,
    ) { calendars, selectedIds ->
        calendars.filter { it.id in selectedIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun setUpcomingEventsCalendarIds(calendarIds: Set<String>) {
        searchUiSettings.setFocusUpcomingEventsCalendarIds(calendarIds)
    }
}
