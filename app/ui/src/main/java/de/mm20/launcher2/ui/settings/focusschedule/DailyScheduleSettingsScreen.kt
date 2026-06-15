package de.mm20.launcher2.ui.settings.focusschedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import de.mm20.launcher2.ui.component.SmallMessage
import de.mm20.launcher2.ui.component.preferences.Preference
import de.mm20.launcher2.ui.component.preferences.PreferenceCategory
import de.mm20.launcher2.ui.component.preferences.PreferenceScreen
import de.mm20.launcher2.ui.component.preferences.SwitchPreference
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.LocalContext
import de.mm20.launcher2.ui.component.MissingPermissionBanner
import kotlinx.serialization.Serializable

@Serializable
data object DailyScheduleSettingsRoute : NavKey

@Composable
fun DailyScheduleSettingsScreen() {
    val viewModel: DailyScheduleSettingsScreenVM = viewModel()
    val enabled by viewModel.dailyScheduleEnabled.collectAsStateWithLifecycle(null)
    val selectedCalendarId by viewModel.selectedCalendarId.collectAsStateWithLifecycle(null)
    val selectedCalendar by viewModel.selectedCalendar.collectAsStateWithLifecycle(null)
    val calendars by viewModel.calendars.collectAsStateWithLifecycle(emptyList())
    val hasCalendarPermission by viewModel.hasCalendarPermission.collectAsStateWithLifecycle(true)
    val context = LocalContext.current
    var pickerOpen by remember { mutableStateOf(false) }

    PreferenceScreen(title = stringResource(R.string.focus_daily_schedule_title)) {
        item {
            PreferenceCategory(title = stringResource(R.string.focus_daily_schedule_title)) {
                SmallMessage(
                    modifier = Modifier.padding(bottom = 12.dp),
                    icon = R.drawable.schedule_24px,
                    text = stringResource(R.string.focus_daily_schedule_summary),
                )
                SwitchPreference(
                    title = stringResource(R.string.focus_daily_schedule_enabled),
                    summary = stringResource(R.string.focus_daily_schedule_enabled_summary),
                    icon = R.drawable.schedule_24px,
                    value = enabled == true,
                    onValueChanged = viewModel::setDailyScheduleEnabled,
                )
                if (!hasCalendarPermission) {
                    MissingPermissionBanner(
                        modifier = Modifier.padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
                        text = stringResource(R.string.missing_permission_calendar_widget_settings),
                        onClick = {
                            (context as? AppCompatActivity)?.let {
                                viewModel.requestCalendarPermission(it)
                            }
                        }
                    )
                }
                Preference(
                    title = stringResource(R.string.focus_daily_schedule_calendar),
                    summary = selectedCalendar?.let { calendarLabel(it) }
                        ?: stringResource(R.string.focus_daily_schedule_calendar_none),
                    icon = R.drawable.schedule_24px,
                    onClick = { pickerOpen = true },
                )
            }
        }
        if (selectedCalendarId != null) {
            item {
                PreferenceCategory {
                    Preference(
                        title = stringResource(R.string.focus_daily_schedule_calendar_summary),
                        icon = R.drawable.event_24px,
                        onClick = {},
                    )
                }
            }
        }
    }

    if (pickerOpen) {
        CalendarPickerSheet(
            selectedCalendarId = selectedCalendarId,
            calendars = calendars,
            onDismissRequest = { pickerOpen = false },
            onSelect = {
                viewModel.setDailyScheduleCalendarId(it?.id)
                pickerOpen = false
            },
        )
    }
}

private fun calendarLabel(calendar: CalendarList): String {
    return if (calendar.owner.isNullOrBlank()) {
        calendar.name
    } else {
        "${calendar.name} · ${calendar.owner}"
    }
}

@Composable
private fun CalendarPickerSheet(
    selectedCalendarId: String?,
    calendars: List<CalendarList>,
    onDismissRequest: () -> Unit,
    onSelect: (CalendarList?) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filteredCalendars = remember(calendars, query) {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) {
            calendars
        } else {
            calendars.filter {
                normalized in it.name.lowercase() ||
                    normalized in it.id.lowercase() ||
                    normalized in (it.owner ?: "").lowercase() ||
                    normalized in it.providerId.lowercase()
            }
        }
    }

    DismissableBottomSheet(expanded = true, onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.focus_daily_schedule_calendar_picker_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text(stringResource(R.string.focus_daily_schedule_calendar_picker_search)) },
            )
            LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(null) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.focus_daily_schedule_calendar_picker_none),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        RadioButton(
                            selected = selectedCalendarId == null,
                            onClick = { onSelect(null) },
                        )
                    }
                }
                items(filteredCalendars, key = { it.id }) { calendar ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(calendar) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = calendar.name,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = calendar.owner?.takeIf { it.isNotBlank() }
                                    ?: calendar.providerId,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        RadioButton(
                            selected = selectedCalendarId == calendar.id,
                            onClick = { onSelect(calendar) },
                        )
                    }
                }
                if (filteredCalendars.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.focus_daily_schedule_calendar_picker_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                }
            }
        }
    }
}
