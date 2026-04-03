package de.mm20.launcher2.ui.settings.homepanels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import de.mm20.launcher2.calendar.providers.CalendarList
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.DismissableBottomSheet
import de.mm20.launcher2.ui.component.preferences.Preference
import de.mm20.launcher2.ui.component.preferences.PreferenceCategory
import de.mm20.launcher2.ui.component.preferences.PreferenceScreen
import de.mm20.launcher2.ui.locals.LocalBackStack
import de.mm20.launcher2.ui.settings.focushabits.DailyHabitsSettingsRoute
import de.mm20.launcher2.ui.settings.focusschedule.DailyScheduleSettingsRoute
import de.mm20.launcher2.ui.settings.focusschedule.ScheduleDockSettingsRoute
import kotlinx.serialization.Serializable

@Serializable
data object HomePanelsSettingsRoute : NavKey

@Composable
fun HomePanelsSettingsScreen() {
    val viewModel: HomePanelsSettingsScreenVM = viewModel()
    val backStack = LocalBackStack.current
    val selectedDailyScheduleCalendar by viewModel.selectedDailyScheduleCalendar.collectAsStateWithLifecycle(null)
    val selectedUpcomingCalendars by viewModel.selectedUpcomingCalendars.collectAsStateWithLifecycle(emptyList())
    val calendars by viewModel.calendars.collectAsStateWithLifecycle(emptyList())
    var showUpcomingPicker by remember { mutableStateOf(false) }

    PreferenceScreen(title = stringResource(R.string.home_panels_title)) {
        item {
            PreferenceCategory(title = stringResource(R.string.home_panels_title)) {
                Preference(
                    title = stringResource(R.string.focus_daily_schedule_title),
                    summary = selectedDailyScheduleCalendar?.let { formatCalendarSummary(it) }
                        ?: stringResource(R.string.focus_daily_schedule_calendar_none),
                    icon = R.drawable.schedule_24px,
                    onClick = { backStack.add(DailyScheduleSettingsRoute) },
                )
                Preference(
                    title = stringResource(R.string.focus_schedule_dock_title),
                    summary = stringResource(R.string.focus_schedule_dock_summary),
                    icon = R.drawable.apps_24px,
                    onClick = { backStack.add(ScheduleDockSettingsRoute) },
                )
                Preference(
                    title = stringResource(R.string.focus_settings_daily_habits_title),
                    summary = stringResource(R.string.focus_settings_daily_habits_summary),
                    icon = R.drawable.check_24px,
                    onClick = { backStack.add(DailyHabitsSettingsRoute) },
                )
                Preference(
                    title = stringResource(R.string.home_panels_upcoming_events_title),
                    summary = if (selectedUpcomingCalendars.isEmpty()) {
                        stringResource(R.string.home_panels_upcoming_events_all_calendars)
                    } else {
                        selectedUpcomingCalendars.joinToString { it.name }
                    },
                    icon = R.drawable.event_24px,
                    onClick = { showUpcomingPicker = true },
                )
            }
        }
    }

    if (showUpcomingPicker) {
        UpcomingEventsCalendarsSheet(
            calendars = calendars,
            selectedIds = selectedUpcomingCalendars.map { it.id }.toSet(),
            onDismissRequest = { showUpcomingPicker = false },
            onSelectionChanged = viewModel::setUpcomingEventsCalendarIds,
        )
    }
}

private fun formatCalendarSummary(calendar: CalendarList): String {
    return if (calendar.owner.isNullOrBlank()) calendar.name else "${calendar.name} · ${calendar.owner}"
}

@Composable
private fun UpcomingEventsCalendarsSheet(
    calendars: List<CalendarList>,
    selectedIds: Set<String>,
    onDismissRequest: () -> Unit,
    onSelectionChanged: (Set<String>) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filteredCalendars = remember(calendars, query) {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) calendars else calendars.filter {
            normalized in it.name.lowercase() ||
                normalized in (it.owner ?: "").lowercase() ||
                normalized in it.providerId.lowercase()
        }
    }

    DismissableBottomSheet(expanded = true, onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.home_panels_upcoming_events_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text(stringResource(R.string.home_panels_upcoming_events_search)) },
            )
            LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
                items(filteredCalendars, key = { it.id }) { calendar ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectionChanged(
                                    if (calendar.id in selectedIds) selectedIds - calendar.id else selectedIds + calendar.id
                                )
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = calendar.name,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = calendar.owner?.takeIf { it.isNotBlank() } ?: calendar.providerId,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Checkbox(
                            checked = calendar.id in selectedIds,
                            onCheckedChange = {
                                onSelectionChanged(
                                    if (it) selectedIds + calendar.id else selectedIds - calendar.id
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
