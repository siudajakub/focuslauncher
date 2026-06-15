package de.mm20.launcher2.ui.settings.focussettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.preferences.Preference
import de.mm20.launcher2.ui.component.preferences.PreferenceCategory
import de.mm20.launcher2.ui.component.preferences.PreferenceScreen
import de.mm20.launcher2.ui.locals.LocalBackStack
import de.mm20.launcher2.ui.settings.focusapps.FocusAppsSettingsRoute
import de.mm20.launcher2.ui.settings.focushabits.DailyHabitsSettingsRoute
import de.mm20.launcher2.ui.settings.focusschedule.DailyScheduleSettingsRoute
import de.mm20.launcher2.ui.settings.focusschedule.ScheduleDockSettingsRoute
import de.mm20.launcher2.ui.settings.focussupport.FocusSupportSettingsRoute
import de.mm20.launcher2.ui.settings.focussystem.FocusQuickStartRoute
import de.mm20.launcher2.ui.settings.focussystem.FocusSystemSettingsRoute
import de.mm20.launcher2.ui.settings.focussystem.FocusSystemSettingsScreenVM
import de.mm20.launcher2.ui.launcher.focus.FocusInsightsRoute
import org.koin.compose.koinInject
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import de.mm20.launcher2.ui.component.preferences.SliderPreference
import de.mm20.launcher2.ui.component.preferences.TextPreference
import kotlinx.serialization.Serializable

@Serializable
data object FocusSettingsRoute : NavKey

@Composable
fun FocusSettingsScreen() {
    val viewModel: FocusSystemSettingsScreenVM = viewModel()
    val backStack = LocalBackStack.current
    val searchUiSettings = koinInject<SearchUiSettings>()

    val startHour by searchUiSettings.focusPlanTimelineStartHour.collectAsStateWithLifecycle(initialValue = 9)
    val endHour by searchUiSettings.focusPlanTimelineEndHour.collectAsStateWithLifecycle(initialValue = 22)
    val durations by searchUiSettings.focusPlanDurations.collectAsStateWithLifecycle(initialValue = listOf(15, 30, 45, 60, 90, 120, 180, 240))

    val focusModeEnabled = viewModel.focusModeEnabled.collectAsStateWithLifecycle().value
    val shouldPromoteQuickStart = viewModel.shouldPromoteQuickStart.collectAsStateWithLifecycle().value
    val dailyScheduleEnabled = viewModel.dailyScheduleEnabled.collectAsStateWithLifecycle().value

    PreferenceScreen(title = stringResource(R.string.focus_settings_title)) {
        // Status dashboard
        item {
            PreferenceCategory(title = stringResource(R.string.focus_settings_status_title)) {
                // Focus state
                Preference(
                    title = if (focusModeEnabled) {
                        stringResource(R.string.focus_settings_status_focus_enabled)
                    } else {
                        stringResource(R.string.focus_settings_status_focus_off)
                    },
                    icon = R.drawable.timer_24px,
                    onClick = { backStack.add(FocusSystemSettingsRoute) },
                )

                // Quick Start status
                Preference(
                    title = stringResource(R.string.focus_system_quick_start_title),
                    summary = if (!shouldPromoteQuickStart) {
                        stringResource(R.string.focus_settings_quick_start_card_ready) + " - Tap to run again"
                    } else {
                        stringResource(R.string.focus_settings_quick_start_card_not_started)
                    },
                    icon = R.drawable.emoji_objects_24px,
                    onClick = { backStack.add(FocusQuickStartRoute) },
                )
            }
        }

        // Apps category
        item {
            PreferenceCategory(title = stringResource(R.string.focus_settings_apps_title)) {
                Preference(
                    title = stringResource(R.string.focus_apps_title),
                    summary = stringResource(R.string.focus_apps_summary),
                    icon = R.drawable.apps_24px,
                    onClick = { backStack.add(FocusAppsSettingsRoute) },
                )
            }
        }
        // Schedules and Day category
        item {
            PreferenceCategory(title = stringResource(R.string.focus_settings_schedules_title)) {
                Preference(
                    title = stringResource(R.string.focus_daily_schedule_title),
                    summary = stringResource(R.string.focus_settings_schedules_summary),
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
                SliderPreference(
                    title = "Timeline start hour",
                    value = startHour,
                    onValueChanged = { searchUiSettings.setFocusPlanTimelineStartHour(it) },
                    min = 0,
                    max = 23,
                    label = { Text(String.format("%02d:00", it)) }
                )
                SliderPreference(
                    title = "Timeline end hour",
                    value = endHour,
                    onValueChanged = { searchUiSettings.setFocusPlanTimelineEndHour(it) },
                    min = 1,
                    max = 24,
                    label = { Text(String.format("%02d:00", it)) }
                )
                TextPreference(
                    title = "Event durations (minutes)",
                    value = durations.joinToString(", "),
                    onValueChanged = { textValue ->
                        val newDurations = textValue.split(",")
                            .mapNotNull { it.trim().toIntOrNull() }
                            .filter { it > 0 }
                            .distinct()
                            .sorted()
                        if (newDurations.isNotEmpty()) {
                            searchUiSettings.setFocusPlanDurations(newDurations)
                        }
                    }
                )
            }
        }

        // Reports category
        item {
            PreferenceCategory(title = stringResource(R.string.focus_settings_reports_title)) {
                Preference(
                    title = stringResource(R.string.focus_insights_title),
                    summary = stringResource(R.string.focus_settings_reports_summary),
                    icon = R.drawable.query_stats_24px,
                    onClick = { backStack.add(FocusInsightsRoute) },
                )
            }
        }

        // Guidance and Profiles category
        item {
            PreferenceCategory(title = stringResource(R.string.focus_settings_guidance_title)) {
                Preference(
                    title = stringResource(R.string.focus_support_title),
                    summary = stringResource(R.string.focus_support_summary),
                    icon = R.drawable.emoji_objects_24px,
                    onClick = { backStack.add(FocusSupportSettingsRoute) },
                )
            }
        }
    }
}
