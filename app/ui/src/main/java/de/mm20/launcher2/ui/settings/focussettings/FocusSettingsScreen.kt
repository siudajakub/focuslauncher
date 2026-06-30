package de.mm20.launcher2.ui.settings.focussettings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import de.mm20.launcher2.calendar.providers.CalendarList
import de.mm20.launcher2.preferences.FocusAdaptiveFrictionMode
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.MissingPermissionBanner
import de.mm20.launcher2.ui.component.SmallMessage
import de.mm20.launcher2.ui.component.preferences.ListPreference
import de.mm20.launcher2.ui.component.preferences.Preference
import de.mm20.launcher2.ui.component.preferences.PreferenceCategory
import de.mm20.launcher2.ui.component.preferences.PreferenceScreen
import de.mm20.launcher2.ui.component.preferences.SliderPreference
import de.mm20.launcher2.ui.component.preferences.SwitchPreference
import de.mm20.launcher2.ui.component.preferences.TextPreference
import de.mm20.launcher2.ui.launcher.focus.FocusInsightsRoute
import de.mm20.launcher2.ui.locals.LocalBackStack
import de.mm20.launcher2.ui.settings.focusapps.FocusAppsSettingsRoute
import de.mm20.launcher2.ui.settings.focushabits.DailyHabitsSettingsRoute
import de.mm20.launcher2.ui.settings.focusschedule.DailyScheduleSettingsRoute
import de.mm20.launcher2.ui.settings.focusschedule.ScheduleDockSettingsRoute
import de.mm20.launcher2.ui.settings.focussupport.FocusSupportSettingsRoute
import de.mm20.launcher2.ui.settings.focussystem.FocusQuickStartRoute
import de.mm20.launcher2.ui.settings.focussystem.FocusSystemSettingsScreenVM
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
data object FocusSettingsRoute : NavKey

@Composable
fun FocusSettingsScreen() {
    val viewModel: FocusSystemSettingsScreenVM = viewModel()
    val backStack = LocalBackStack.current
    val context = LocalContext.current
    val searchUiSettings = koinInject<SearchUiSettings>()

    var notificationsGranted by remember { mutableStateOf(hasNotificationPermission(context)) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationsGranted = granted }

    // Usage Access (and POST_NOTIFICATIONS, if granted from the system dialog) are
    // resolved outside this composition, so re-check them whenever the screen resumes.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshUsageAccess()
        notificationsGranted = hasNotificationPermission(context)
        onPauseOrDispose { }
    }

    val startHour by searchUiSettings.focusPlanTimelineStartHour.collectAsStateWithLifecycle(initialValue = 9)
    val endHour by searchUiSettings.focusPlanTimelineEndHour.collectAsStateWithLifecycle(initialValue = 22)
    val durations by searchUiSettings.focusPlanDurations.collectAsStateWithLifecycle(initialValue = listOf(15, 30, 45, 60, 90, 120, 180, 240))

    val focusModeEnabled = viewModel.focusModeEnabled.collectAsStateWithLifecycle().value
    val hideDistractingApps = viewModel.hideDistractingApps.collectAsStateWithLifecycle().value
    val focusEnableDnd = viewModel.focusEnableDnd.collectAsStateWithLifecycle().value
    val fadeDistractingApps = viewModel.fadeDistractingApps.collectAsStateWithLifecycle().value
    val noIconsMode = viewModel.noIconsMode.collectAsStateWithLifecycle().value
    val adaptiveFrictionMode = viewModel.adaptiveFrictionMode.collectAsStateWithLifecycle().value
    val commuteModeEnabled = viewModel.commuteModeEnabled.collectAsStateWithLifecycle().value
    val atAGlanceEnabled = viewModel.atAGlanceEnabled.collectAsStateWithLifecycle().value
    val reviewSuggestionsEnabled = viewModel.reviewSuggestionsEnabled.collectAsStateWithLifecycle().value
    val environmentContextEnabled = viewModel.environmentContextEnabled.collectAsStateWithLifecycle().value
    val environmentChargingContextEnabled = viewModel.environmentChargingContextEnabled.collectAsStateWithLifecycle().value
    val environmentExplainabilityEnabled = viewModel.environmentExplainabilityEnabled.collectAsStateWithLifecycle().value
    val applyToPersonalProfile = viewModel.applyToPersonalProfile.collectAsStateWithLifecycle().value
    val applyToWorkProfile = viewModel.applyToWorkProfile.collectAsStateWithLifecycle().value
    val applyToPrivateProfile = viewModel.applyToPrivateProfile.collectAsStateWithLifecycle().value
    val dailyScheduleEnabled = viewModel.dailyScheduleEnabled.collectAsStateWithLifecycle().value
    val selectedDailyScheduleCalendar = viewModel.selectedDailyScheduleCalendar.collectAsStateWithLifecycle().value
    val timeBlindnessEnabled = viewModel.focusTimeBlindnessRemindersEnabled.collectAsStateWithLifecycle().value
    val timeBlindnessIntervalMinutes = viewModel.focusTimeBlindnessIntervalMinutes.collectAsStateWithLifecycle().value
    val usageAccessGranted = viewModel.usageAccessGranted.collectAsStateWithLifecycle().value

    PreferenceScreen(title = stringResource(R.string.focus_settings_title)) {
        // Master toggle + inline status summary (replaces the legacy status nav-row).
        item {
            PreferenceCategory(title = stringResource(R.string.focus_settings_status_title)) {
                SwitchPreference(
                    title = if (focusModeEnabled) {
                        stringResource(R.string.focus_settings_status_focus_enabled)
                    } else {
                        stringResource(R.string.focus_settings_status_focus_off)
                    },
                    summary = stringResource(R.string.focus_settings_mode_enabled_summary),
                    icon = R.drawable.timer_24px,
                    value = focusModeEnabled,
                    onValueChanged = { enabled ->
                        viewModel.setFocusModeEnabled(enabled)
                        // The gate "time" fires an AppSessionExpiryWorker notification when it
                        // ends; without POST_NOTIFICATIONS that reminder silently never shows.
                        if (enabled && !hasNotificationPermission(context)) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
                Preference(
                    title = stringResource(R.string.focus_system_quick_start_title),
                    summary = stringResource(R.string.focus_system_quick_start_open_summary),
                    icon = R.drawable.emoji_objects_24px,
                    onClick = { backStack.add(FocusQuickStartRoute) },
                )
            }
        }

        // Modes & friction
        item {
            PreferenceCategory(title = stringResource(R.string.focus_settings_modes_friction_title)) {
                SwitchPreference(
                    title = stringResource(R.string.focus_settings_hide_distracting),
                    summary = stringResource(R.string.focus_settings_hide_distracting_summary),
                    icon = R.drawable.visibility_off_24px,
                    value = hideDistractingApps,
                    onValueChanged = viewModel::setHideDistractingApps,
                )
                SwitchPreference(
                    title = stringResource(R.string.focus_settings_fade_distracting),
                    summary = stringResource(R.string.focus_settings_fade_distracting_summary),
                    icon = R.drawable.visibility_24px,
                    value = fadeDistractingApps,
                    onValueChanged = viewModel::setFadeDistractingApps,
                )
                SwitchPreference(
                    title = stringResource(R.string.focus_settings_no_icons),
                    summary = stringResource(R.string.focus_settings_no_icons_summary),
                    icon = R.drawable.text_fields_24px,
                    value = noIconsMode,
                    onValueChanged = viewModel::setNoIconsMode,
                )
                SwitchPreference(
                    title = stringResource(R.string.focus_settings_enable_dnd),
                    summary = stringResource(R.string.focus_settings_enable_dnd_summary),
                    icon = R.drawable.notifications_24px,
                    value = focusEnableDnd,
                    onValueChanged = viewModel::setFocusEnableDnd,
                )
                ListPreference(
                    title = stringResource(R.string.focus_system_adaptive_friction_title),
                    items = listOf(
                        stringResource(R.string.focus_system_adaptive_friction_auto) to FocusAdaptiveFrictionMode.Auto,
                        stringResource(R.string.focus_system_adaptive_friction_light) to FocusAdaptiveFrictionMode.Light,
                        stringResource(R.string.focus_system_adaptive_friction_normal) to FocusAdaptiveFrictionMode.Normal,
                        stringResource(R.string.focus_system_adaptive_friction_strict) to FocusAdaptiveFrictionMode.Strict,
                    ),
                    value = adaptiveFrictionMode,
                    onValueChanged = viewModel::setAdaptiveFrictionMode,
                    icon = R.drawable.tune_24px,
                )
                SmallMessage(
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                    icon = R.drawable.emoji_objects_24px,
                    text = stringResource(R.string.focus_system_quick_start_summary),
                )
                Preference(
                    title = stringResource(R.string.focus_system_preset_balanced_title),
                    summary = stringResource(R.string.focus_system_preset_balanced_summary),
                    icon = R.drawable.tune_24px,
                    onClick = viewModel::applyBalancedPreset,
                )
                Preference(
                    title = stringResource(R.string.focus_system_preset_hard_title),
                    summary = stringResource(R.string.focus_system_preset_hard_summary),
                    icon = R.drawable.lock_24px,
                    onClick = viewModel::applyHardFocusPreset,
                )
                Preference(
                    title = stringResource(R.string.focus_system_preset_minimal_title),
                    summary = stringResource(R.string.focus_system_preset_minimal_summary),
                    icon = R.drawable.visibility_off_24px,
                    onClick = viewModel::applyMinimalPreset,
                )
            }
        }

        // Focus apps
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

        // Day: schedule, dock, habits, plan timeline
        item {
            PreferenceCategory(title = stringResource(R.string.focus_system_day_title)) {
                Preference(
                    title = stringResource(R.string.focus_daily_schedule_title),
                    summary = when {
                        !dailyScheduleEnabled -> stringResource(R.string.focus_system_status_off)
                        selectedDailyScheduleCalendar != null -> formatCalendarSummary(selectedDailyScheduleCalendar)
                        else -> stringResource(R.string.focus_system_status_enabled)
                    },
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
                    title = stringResource(R.string.focus_settings_timeline_start_hour),
                    value = startHour,
                    onValueChanged = { searchUiSettings.setFocusPlanTimelineStartHour(it) },
                    min = 0,
                    max = 23,
                    label = { Text(String.format("%02d:00", it)) }
                )
                SliderPreference(
                    title = stringResource(R.string.focus_settings_timeline_end_hour),
                    value = endHour,
                    onValueChanged = { searchUiSettings.setFocusPlanTimelineEndHour(it) },
                    min = 1,
                    max = 24,
                    label = { Text(String.format("%02d:00", it)) }
                )
                TextPreference(
                    title = stringResource(R.string.focus_settings_event_durations),
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

        // Time awareness
        item {
            PreferenceCategory(title = stringResource(R.string.focus_system_time_awareness_title)) {
                SmallMessage(
                    modifier = Modifier.padding(bottom = 12.dp),
                    icon = R.drawable.alarm_24px,
                    text = stringResource(R.string.focus_settings_time_blindness_summary),
                )
                SwitchPreference(
                    title = stringResource(R.string.focus_settings_time_blindness_enabled),
                    summary = stringResource(R.string.focus_settings_time_blindness_enabled_summary),
                    icon = R.drawable.alarm_24px,
                    value = timeBlindnessEnabled,
                    onValueChanged = { enabled ->
                        viewModel.setFocusTimeBlindnessRemindersEnabled(enabled)
                        if (enabled && !hasNotificationPermission(context)) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
                if (timeBlindnessEnabled && !usageAccessGranted) {
                    MissingPermissionBanner(
                        modifier = Modifier.padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
                        text = stringResource(R.string.focus_settings_time_blindness_usage_access),
                        onClick = { viewModel.openUsageAccessSettings() },
                    )
                }
                if (timeBlindnessEnabled && !notificationsGranted) {
                    MissingPermissionBanner(
                        modifier = Modifier.padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
                        text = stringResource(R.string.focus_settings_time_blindness_notifications),
                        onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    )
                }
                if (timeBlindnessEnabled) {
                    ListPreference(
                        title = stringResource(R.string.focus_settings_time_blindness_interval),
                        items = listOf(5, 10, 15, 20, 30, 45, 60).map {
                            stringResource(R.string.focus_settings_time_blindness_interval_value, it) to it
                        },
                        value = timeBlindnessIntervalMinutes,
                        onValueChanged = viewModel::setFocusTimeBlindnessIntervalMinutes,
                        icon = R.drawable.timer_24px,
                    )
                }
            }
        }

        // Intelligence / context
        item {
            PreferenceCategory(title = stringResource(R.string.focus_system_intelligence_title)) {
                SwitchPreference(
                    title = stringResource(R.string.focus_system_personal_profile_title),
                    summary = stringResource(R.string.focus_system_personal_profile_summary),
                    icon = R.drawable.person_24px,
                    value = applyToPersonalProfile,
                    onValueChanged = viewModel::setApplyToPersonalProfile,
                )
                SwitchPreference(
                    title = stringResource(R.string.focus_system_work_profile_title),
                    summary = stringResource(R.string.focus_system_work_profile_summary),
                    icon = R.drawable.person_24px,
                    value = applyToWorkProfile,
                    onValueChanged = viewModel::setApplyToWorkProfile,
                )
                SwitchPreference(
                    title = stringResource(R.string.focus_system_private_profile_title),
                    summary = stringResource(R.string.focus_system_private_profile_summary),
                    icon = R.drawable.lock_24px,
                    value = applyToPrivateProfile,
                    onValueChanged = viewModel::setApplyToPrivateProfile,
                )
                SwitchPreference(
                    title = stringResource(R.string.focus_system_at_a_glance_title),
                    summary = stringResource(R.string.focus_system_at_a_glance_summary),
                    icon = R.drawable.visibility_24px,
                    value = atAGlanceEnabled,
                    onValueChanged = viewModel::setAtAGlanceEnabled,
                )
                SwitchPreference(
                    title = stringResource(R.string.focus_system_commute_mode_title),
                    summary = stringResource(R.string.focus_system_commute_mode_summary),
                    icon = R.drawable.directions_car_24px,
                    value = commuteModeEnabled,
                    onValueChanged = viewModel::setCommuteModeEnabled,
                )
                SwitchPreference(
                    title = stringResource(R.string.focus_system_context_title),
                    summary = stringResource(R.string.focus_system_context_summary),
                    icon = R.drawable.travel_explore_24px,
                    value = environmentContextEnabled,
                    onValueChanged = viewModel::setEnvironmentContextEnabled,
                )
                SwitchPreference(
                    title = stringResource(R.string.focus_system_context_charging_title),
                    summary = stringResource(R.string.focus_system_context_charging_summary),
                    icon = R.drawable.battery_4_bar_24px,
                    value = environmentChargingContextEnabled,
                    onValueChanged = viewModel::setEnvironmentChargingContextEnabled,
                )
                SwitchPreference(
                    title = stringResource(R.string.focus_system_explainability_title),
                    summary = stringResource(R.string.focus_system_explainability_summary),
                    icon = R.drawable.info_24px,
                    value = environmentExplainabilityEnabled,
                    onValueChanged = viewModel::setEnvironmentExplainabilityEnabled,
                )
                SwitchPreference(
                    title = stringResource(R.string.focus_system_review_title),
                    summary = stringResource(R.string.focus_system_review_summary),
                    icon = R.drawable.query_stats_24px,
                    value = reviewSuggestionsEnabled,
                    onValueChanged = viewModel::setReviewSuggestionsEnabled,
                )
            }
        }

        // Support
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

        // Insights / reports
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
    }
}

private fun hasNotificationPermission(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
}

private fun formatCalendarSummary(calendar: CalendarList): String {
    return if (calendar.owner.isNullOrBlank()) {
        calendar.name
    } else {
        "${calendar.name} · ${calendar.owner}"
    }
}
