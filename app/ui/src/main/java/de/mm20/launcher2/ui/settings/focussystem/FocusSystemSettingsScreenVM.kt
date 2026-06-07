package de.mm20.launcher2.ui.settings.focussystem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Intent
import android.content.Context
import de.mm20.launcher2.calendar.CalendarRepository
import de.mm20.launcher2.calendar.providers.CalendarList
import de.mm20.launcher2.preferences.FocusAdaptiveFrictionMode
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import de.mm20.launcher2.ui.launcher.focus.shouldShowFocusQuickStart
import de.mm20.launcher2.searchactions.SearchActionService
import de.mm20.launcher2.searchactions.actions.SearchActionIcon
import de.mm20.launcher2.searchactions.builders.CustomIntentActionBuilder
import de.mm20.launcher2.ui.settings.SettingsActivity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FocusSystemSettingsScreenVM : ViewModel(), KoinComponent {
    private val context: Context by inject()
    private val searchUiSettings: SearchUiSettings by inject()
    private val calendarRepository: CalendarRepository by inject()
    private val searchActionService: SearchActionService by inject()

    val focusModeEnabled = searchUiSettings.focusModeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val hideDistractingApps = searchUiSettings.focusHideDistractingApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    val focusEnableDnd = searchUiSettings.focusEnableDnd
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val fadeDistractingApps = searchUiSettings.focusFadeDistractingApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val noIconsMode = searchUiSettings.focusNoIconsMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val commuteModeEnabled = searchUiSettings.focusCommuteModeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val atAGlanceEnabled = searchUiSettings.focusAtAGlanceEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    val reviewSuggestionsEnabled = searchUiSettings.focusReviewSuggestionsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    val environmentContextEnabled = searchUiSettings.focusEnvironmentContextEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    val environmentChargingContextEnabled = searchUiSettings.focusEnvironmentChargingContextEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    val environmentExplainabilityEnabled = searchUiSettings.focusEnvironmentExplainabilityEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    val dailyScheduleEnabled = searchUiSettings.focusDailyScheduleEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val applyToPersonalProfile = searchUiSettings.focusApplyToPersonalProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    val applyToWorkProfile = searchUiSettings.focusApplyToWorkProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    val applyToPrivateProfile = searchUiSettings.focusApplyToPrivateProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    val adaptiveFrictionMode = searchUiSettings.focusAdaptiveFrictionMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), FocusAdaptiveFrictionMode.Auto)

    val calendars = calendarRepository.getCalendars()
        .map { calendars ->
            calendars.sortedWith(
                compareBy<CalendarList>({ it.name.lowercase() }, { it.owner ?: "" }, { it.providerId })
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val selectedDailyScheduleCalendar = combine(
        calendars,
        searchUiSettings.focusDailyScheduleCalendarId,
    ) { calendars, selectedId ->
        calendars.firstOrNull { it.id == selectedId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val shouldPromoteQuickStart = combine(
        searchUiSettings.focusModeEnabled,
        searchUiSettings.focusEssentialAppKeys,
        searchUiSettings.focusDistractingAppKeys,
        searchUiSettings.focusDailyScheduleEnabled,
        searchUiSettings.focusHabitsEnabled,
    ) { focusModeEnabled, essentialKeys, distractingKeys, dailyScheduleEnabled, habitsEnabled ->
        shouldShowFocusQuickStart(
            focusModeEnabled = focusModeEnabled,
            essentialCount = essentialKeys.size,
            distractingCount = distractingKeys.size,
            dailyScheduleEnabled = dailyScheduleEnabled,
            habitsEnabled = habitsEnabled,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    fun setFocusModeEnabled(enabled: Boolean) {
        searchUiSettings.setFocusModeEnabled(enabled)
    }

    fun setHideDistractingApps(enabled: Boolean) {
        searchUiSettings.setFocusHideDistractingApps(enabled)
    }

    fun setFocusEnableDnd(enabled: Boolean) {
        searchUiSettings.setFocusEnableDnd(enabled)
    }

    fun setFadeDistractingApps(enabled: Boolean) {
        searchUiSettings.setFocusFadeDistractingApps(enabled)
    }

    fun setNoIconsMode(enabled: Boolean) {
        searchUiSettings.setFocusNoIconsMode(enabled)
    }

    fun setCommuteModeEnabled(enabled: Boolean) {
        searchUiSettings.setFocusCommuteModeEnabled(enabled)
    }

    fun setAtAGlanceEnabled(enabled: Boolean) {
        searchUiSettings.setFocusAtAGlanceEnabled(enabled)
    }

    fun setReviewSuggestionsEnabled(enabled: Boolean) {
        searchUiSettings.setFocusReviewSuggestionsEnabled(enabled)
    }

    fun setApplyToPersonalProfile(enabled: Boolean) {
        searchUiSettings.setFocusApplyToPersonalProfile(enabled)
    }

    fun setApplyToWorkProfile(enabled: Boolean) {
        searchUiSettings.setFocusApplyToWorkProfile(enabled)
    }

    fun setApplyToPrivateProfile(enabled: Boolean) {
        searchUiSettings.setFocusApplyToPrivateProfile(enabled)
    }

    fun setAdaptiveFrictionMode(mode: FocusAdaptiveFrictionMode) {
        searchUiSettings.setFocusAdaptiveFrictionMode(mode)
    }

    fun setEnvironmentContextEnabled(enabled: Boolean) {
        searchUiSettings.setFocusEnvironmentContextEnabled(enabled)
    }

    fun setEnvironmentChargingContextEnabled(enabled: Boolean) {
        searchUiSettings.setFocusEnvironmentChargingContextEnabled(enabled)
    }

    fun setEnvironmentExplainabilityEnabled(enabled: Boolean) {
        searchUiSettings.setFocusEnvironmentExplainabilityEnabled(enabled)
    }

    fun installFocusQuickActions() {
        viewModelScope.launch {
            val existing = searchActionService.getSearchActionBuilders().first()
            val focusActions = listOf(
                buildSettingsAction(
                    label = context.getString(de.mm20.launcher2.ui.R.string.focus_system_quick_action_open_system),
                    route = SettingsActivity.ROUTE_FOCUS_SYSTEM,
                    icon = SearchActionIcon.Search,
                ),
                buildSettingsAction(
                    label = context.getString(de.mm20.launcher2.ui.R.string.focus_system_quick_action_open_report),
                    route = SettingsActivity.ROUTE_FOCUS_REPORT,
                    icon = SearchActionIcon.StatsSearch,
                ),
                buildSettingsAction(
                    label = context.getString(de.mm20.launcher2.ui.R.string.focus_system_quick_action_open_apps),
                    route = SettingsActivity.ROUTE_FOCUS_APPS,
                    icon = SearchActionIcon.SearchList,
                ),
            )
            val merged = (existing + focusActions)
                .distinctBy { it.key }
            searchActionService.saveSearchActionBuilders(merged)
        }
    }

    private fun buildSettingsAction(
        label: String,
        route: String,
        icon: SearchActionIcon,
    ): CustomIntentActionBuilder {
        return CustomIntentActionBuilder(
            label = label,
            queryKey = "focus_quick_action_query",
            baseIntent = Intent().apply {
                setClass(context, SettingsActivity::class.java)
                putExtra(SettingsActivity.EXTRA_ROUTE, route)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            icon = icon,
            iconColor = 0,
            customIcon = null,
        )
    }

    fun applyBalancedPreset() {
        searchUiSettings.setFocusModeEnabled(true)
        searchUiSettings.setFocusHideDistractingApps(true)
        searchUiSettings.setFocusDefaultDelaySeconds(4)
        searchUiSettings.setFocusDefaultSessionMinutes(15)
        searchUiSettings.setFocusEnableDnd(true)
        searchUiSettings.setFocusStartRitualEnabled(false)
        searchUiSettings.setFocusMicroStepPromptEnabled(true)
        searchUiSettings.setFocusRecoveryEnabled(true)
        searchUiSettings.setFocusRecoveryResumeTimeoutMinutes(15)
        searchUiSettings.setFocusTransitionWarningsEnabled(true)
        searchUiSettings.setFocusTransitionWarningLeadMinutes(10)
        searchUiSettings.setFocusBlockPrepPromptsEnabled(true)
        searchUiSettings.setFocusPrepLeadTimeMinutes(10)
        searchUiSettings.setFocusBlockAwareSessionSizingEnabled(true)
        searchUiSettings.setFocusEscalatingFrictionEnabled(true)
        searchUiSettings.setFocusEscalationWindowMinutes(15)
        searchUiSettings.setFocusEscalationExtraDelaySeconds(5)
        searchUiSettings.setFocusDistractingSessionCapMinutes(15)
        searchUiSettings.setFocusAtAGlanceEnabled(true)
        searchUiSettings.setFocusCommuteModeEnabled(false)
        searchUiSettings.setFocusEnvironmentContextEnabled(true)
        searchUiSettings.setFocusEnvironmentExplainabilityEnabled(true)
        searchUiSettings.setFocusReviewSuggestionsEnabled(true)
    }

    fun applyHardFocusPreset() {
        searchUiSettings.setFocusModeEnabled(true)
        searchUiSettings.setFocusHideDistractingApps(true)
        searchUiSettings.setFocusDefaultDelaySeconds(10)
        searchUiSettings.setFocusDefaultSessionMinutes(10)
        searchUiSettings.setFocusEnableDnd(true)
        searchUiSettings.setFocusStartRitualEnabled(true)
        searchUiSettings.setFocusMicroStepPromptEnabled(true)
        searchUiSettings.setFocusRecoveryEnabled(true)
        searchUiSettings.setFocusRecoveryResumeTimeoutMinutes(10)
        searchUiSettings.setFocusTransitionWarningsEnabled(true)
        searchUiSettings.setFocusTransitionWarningLeadMinutes(10)
        searchUiSettings.setFocusBlockPrepPromptsEnabled(true)
        searchUiSettings.setFocusPrepLeadTimeMinutes(15)
        searchUiSettings.setFocusBlockAwareSessionSizingEnabled(true)
        searchUiSettings.setFocusEscalatingFrictionEnabled(true)
        searchUiSettings.setFocusEscalationWindowMinutes(20)
        searchUiSettings.setFocusEscalationExtraDelaySeconds(10)
        searchUiSettings.setFocusDistractingSessionCapMinutes(10)
        searchUiSettings.setFocusAtAGlanceEnabled(true)
        searchUiSettings.setFocusCommuteModeEnabled(false)
        searchUiSettings.setFocusEnvironmentContextEnabled(true)
        searchUiSettings.setFocusEnvironmentExplainabilityEnabled(true)
        searchUiSettings.setFocusReviewSuggestionsEnabled(true)
    }

    fun applyMinimalPreset() {
        searchUiSettings.setFocusModeEnabled(true)
        searchUiSettings.setFocusHideDistractingApps(true)
        searchUiSettings.setFocusDefaultDelaySeconds(2)
        searchUiSettings.setFocusDefaultSessionMinutes(20)
        searchUiSettings.setFocusEnableDnd(false)
        searchUiSettings.setFocusStartRitualEnabled(false)
        searchUiSettings.setFocusMicroStepPromptEnabled(false)
        searchUiSettings.setFocusRecoveryEnabled(true)
        searchUiSettings.setFocusRecoveryResumeTimeoutMinutes(15)
        searchUiSettings.setFocusTransitionWarningsEnabled(false)
        searchUiSettings.setFocusBlockPrepPromptsEnabled(false)
        searchUiSettings.setFocusBlockAwareSessionSizingEnabled(false)
        searchUiSettings.setFocusEscalatingFrictionEnabled(false)
        searchUiSettings.setFocusDistractingSessionCapMinutes(20)
        searchUiSettings.setFocusAtAGlanceEnabled(true)
        searchUiSettings.setFocusCommuteModeEnabled(false)
        searchUiSettings.setFocusEnvironmentContextEnabled(false)
        searchUiSettings.setFocusEnvironmentExplainabilityEnabled(false)
        searchUiSettings.setFocusReviewSuggestionsEnabled(true)
    }
}
