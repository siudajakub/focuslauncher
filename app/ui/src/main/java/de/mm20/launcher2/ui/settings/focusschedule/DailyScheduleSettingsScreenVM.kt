package de.mm20.launcher2.ui.settings.focusschedule

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.mm20.launcher2.calendar.CalendarRepository
import de.mm20.launcher2.calendar.providers.CalendarList
import de.mm20.launcher2.permissions.PermissionGroup
import de.mm20.launcher2.permissions.PermissionsManager
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DailyScheduleSettingsScreenVM : ViewModel(), KoinComponent {
    private val searchUiSettings: SearchUiSettings by inject()
    private val calendarRepository: CalendarRepository by inject()
    private val permissionsManager: PermissionsManager by inject()

    val hasCalendarPermission = permissionsManager.hasPermission(PermissionGroup.Calendar)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val dailyScheduleEnabled = searchUiSettings.focusDailyScheduleEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val selectedCalendarId = searchUiSettings.focusDailyScheduleCalendarId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val calendars = calendarRepository.getCalendars()
        .map { calendars ->
            calendars.sortedWith(
                compareBy<CalendarList>({ it.name.lowercase() }, { it.owner ?: "" }, { it.providerId })
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val selectedCalendar = combine(calendars, selectedCalendarId) { calendars, calendarId ->
        calendars.firstOrNull { it.id == calendarId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun requestCalendarPermission(activity: AppCompatActivity) {
        permissionsManager.requestPermission(activity, PermissionGroup.Calendar)
    }

    fun setDailyScheduleEnabled(enabled: Boolean) {
        searchUiSettings.setFocusDailyScheduleEnabled(enabled)
    }

    fun setDailyScheduleCalendarId(calendarId: String?) {
        searchUiSettings.setFocusDailyScheduleCalendarId(calendarId)
    }
}
