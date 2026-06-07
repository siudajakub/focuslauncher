package de.mm20.launcher2.ui.settings.focussystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import de.mm20.launcher2.calendar.providers.CalendarList
import de.mm20.launcher2.preferences.FocusAdaptiveFrictionMode
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.SmallMessage
import de.mm20.launcher2.ui.component.preferences.ListPreference
import de.mm20.launcher2.ui.component.preferences.Preference
import de.mm20.launcher2.ui.component.preferences.PreferenceCategory
import de.mm20.launcher2.ui.component.preferences.PreferenceScreen
import de.mm20.launcher2.ui.component.preferences.SwitchPreference
import de.mm20.launcher2.ui.locals.LocalBackStack
import de.mm20.launcher2.ui.settings.focushabits.DailyHabitsSettingsRoute
import de.mm20.launcher2.ui.settings.focusschedule.DailyScheduleSettingsRoute
import de.mm20.launcher2.ui.settings.focusschedule.ScheduleDockSettingsRoute
import de.mm20.launcher2.ui.settings.focusapps.FocusAppsSettingsRoute
import de.mm20.launcher2.ui.settings.focusreport.FocusReportSettingsRoute
import de.mm20.launcher2.ui.settings.focussupport.FocusSupportSettingsRoute
import de.mm20.launcher2.ui.settings.homepanels.HomePanelsSettingsRoute
import de.mm20.launcher2.ui.settings.search.SearchSettingsRoute
import de.mm20.launcher2.ui.settings.searchactions.SearchActionsSettingsRoute
import kotlinx.serialization.Serializable

@Serializable
data object FocusSystemSettingsRoute : NavKey

@Composable
fun FocusSystemSettingsScreen() {
    val viewModel: FocusSystemSettingsScreenVM = viewModel()
    val backStack = LocalBackStack.current
    val shouldPromoteQuickStart = viewModel.shouldPromoteQuickStart.collectAsStateWithLifecycle().value
    var showQuickStartSheet by rememberSaveable(shouldPromoteQuickStart) { mutableStateOf(shouldPromoteQuickStart) }

    val focusModeEnabled = viewModel.focusModeEnabled.collectAsStateWithLifecycle().value
    val hideDistractingApps = viewModel.hideDistractingApps.collectAsStateWithLifecycle().value
    val focusEnableDnd = viewModel.focusEnableDnd.collectAsStateWithLifecycle().value
    val fadeDistractingApps = viewModel.fadeDistractingApps.collectAsStateWithLifecycle().value
    val noIconsMode = viewModel.noIconsMode.collectAsStateWithLifecycle().value
    val commuteModeEnabled = viewModel.commuteModeEnabled.collectAsStateWithLifecycle().value
    val atAGlanceEnabled = viewModel.atAGlanceEnabled.collectAsStateWithLifecycle().value
    val reviewSuggestionsEnabled = viewModel.reviewSuggestionsEnabled.collectAsStateWithLifecycle().value
    val environmentContextEnabled = viewModel.environmentContextEnabled.collectAsStateWithLifecycle().value
    val environmentChargingContextEnabled = viewModel.environmentChargingContextEnabled.collectAsStateWithLifecycle().value
    val environmentExplainabilityEnabled = viewModel.environmentExplainabilityEnabled.collectAsStateWithLifecycle().value
    val dailyScheduleEnabled = viewModel.dailyScheduleEnabled.collectAsStateWithLifecycle().value
    val selectedDailyScheduleCalendar = viewModel.selectedDailyScheduleCalendar.collectAsStateWithLifecycle().value
    val applyToPersonalProfile = viewModel.applyToPersonalProfile.collectAsStateWithLifecycle().value
    val applyToWorkProfile = viewModel.applyToWorkProfile.collectAsStateWithLifecycle().value
    val applyToPrivateProfile = viewModel.applyToPrivateProfile.collectAsStateWithLifecycle().value
    val adaptiveFrictionMode = viewModel.adaptiveFrictionMode.collectAsStateWithLifecycle().value

    PreferenceScreen(title = stringResource(R.string.focus_system_title)) {
        item {
            PreferenceCategory(title = stringResource(R.string.focus_system_quick_start_title)) {
                SmallMessage(
                    modifier = Modifier.padding(bottom = 12.dp),
                    icon = R.drawable.emoji_objects_24px,
                    text = stringResource(R.string.focus_system_quick_start_summary),
                )
                Preference(
                    title = stringResource(R.string.focus_system_quick_start_open_title),
                    summary = stringResource(R.string.focus_system_quick_start_open_summary),
                    icon = R.drawable.emoji_objects_24px,
                    onClick = { showQuickStartSheet = true },
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
        item {
            PreferenceCategory(title = stringResource(R.string.focus_system_core_title)) {
                SwitchPreference(
                    title = stringResource(R.string.focus_settings_mode_enabled),
                    summary = stringResource(R.string.focus_settings_mode_enabled_summary),
                    icon = R.drawable.timer_24px,
                    value = focusModeEnabled,
                    onValueChanged = viewModel::setFocusModeEnabled,
                )
                SwitchPreference(
                    title = stringResource(R.string.focus_settings_hide_distracting),
                    summary = stringResource(R.string.focus_settings_hide_distracting_summary),
                    icon = R.drawable.visibility_off_24px,
                    value = hideDistractingApps,
                    onValueChanged = viewModel::setHideDistractingApps,
                )
                SwitchPreference(
                    title = stringResource(R.string.focus_settings_enable_dnd),
                    summary = stringResource(R.string.focus_settings_enable_dnd_summary),
                    icon = R.drawable.notifications_24px,
                    value = focusEnableDnd,
                    onValueChanged = viewModel::setFocusEnableDnd,
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
                Preference(
                    title = stringResource(R.string.focus_apps_title),
                    summary = stringResource(R.string.focus_apps_summary),
                    icon = R.drawable.apps_24px,
                    onClick = { backStack.add(FocusAppsSettingsRoute) },
                )
                Preference(
                    title = stringResource(R.string.focus_system_rules_title),
                    summary = stringResource(R.string.focus_system_rules_summary),
                    icon = R.drawable.tune_24px,
                    onClick = { backStack.add(SearchSettingsRoute) },
                )
                Preference(
                    title = stringResource(R.string.focus_system_quick_actions_title),
                    summary = stringResource(R.string.focus_system_quick_actions_summary),
                    icon = R.drawable.manage_search_24px,
                    onClick = viewModel::installFocusQuickActions,
                )
                Preference(
                    title = stringResource(R.string.focus_support_title),
                    summary = stringResource(R.string.focus_support_summary),
                    icon = R.drawable.emoji_objects_24px,
                    onClick = { backStack.add(FocusSupportSettingsRoute) },
                )
            }
        }
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
                Preference(
                    title = stringResource(R.string.home_panels_title),
                    summary = stringResource(R.string.focus_system_home_panels_summary),
                    icon = R.drawable.home_24px,
                    onClick = { backStack.add(HomePanelsSettingsRoute) },
                )
            }
        }
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
                Preference(
                    title = stringResource(R.string.focus_report_title),
                    summary = stringResource(R.string.focus_report_summary),
                    icon = R.drawable.query_stats_24px,
                    onClick = { backStack.add(FocusReportSettingsRoute) },
                )
            }
        }
    }

    if (showQuickStartSheet) {
        FocusSystemQuickStartSheet(
            expanded = true,
            onDismissRequest = { showQuickStartSheet = false },
            onBalancedPreset = {
                viewModel.applyBalancedPreset()
                showQuickStartSheet = false
            },
            onHardFocusPreset = {
                viewModel.applyHardFocusPreset()
                showQuickStartSheet = false
            },
            onMinimalPreset = {
                viewModel.applyMinimalPreset()
                showQuickStartSheet = false
            },
            onOpenFocusApps = {
                showQuickStartSheet = false
                backStack.add(FocusAppsSettingsRoute)
            },
            onOpenSchedule = {
                showQuickStartSheet = false
                backStack.add(DailyScheduleSettingsRoute)
            },
            onOpenHabits = {
                showQuickStartSheet = false
                backStack.add(DailyHabitsSettingsRoute)
            },
            onOpenReport = {
                showQuickStartSheet = false
                backStack.add(FocusReportSettingsRoute)
            },
        )
    }
}

private fun formatCalendarSummary(calendar: CalendarList): String {
    return if (calendar.owner.isNullOrBlank()) {
        calendar.name
    } else {
        "${calendar.name} · ${calendar.owner}"
    }
}
