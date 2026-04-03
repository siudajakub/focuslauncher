package de.mm20.launcher2.preferences.ui

import de.mm20.launcher2.preferences.LauncherDataStore
import de.mm20.launcher2.preferences.FocusAdaptiveFrictionMode
import de.mm20.launcher2.preferences.FocusBlockPlan
import de.mm20.launcher2.preferences.FocusExperiment
import de.mm20.launcher2.preferences.FocusHabit
import de.mm20.launcher2.preferences.FocusResumeContext
import de.mm20.launcher2.preferences.ScheduleDockMapping
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class SearchUiSettings internal constructor(
    private val launcherDataStore: LauncherDataStore,
) {
    private fun resolvedProductivityWindows(data: de.mm20.launcher2.preferences.LauncherSettingsData): List<de.mm20.launcher2.preferences.FocusProductivityWindow> {
        val windows = data.focusProductivityWindows
        if (windows.isNotEmpty()) return windows
        return buildList {
            add(
                de.mm20.launcher2.preferences.FocusProductivityWindow(
                    startMinutes = data.focusProductivityWindow1StartMinutes,
                    endMinutes = data.focusProductivityWindow1EndMinutes,
                )
            )
            if (
                data.focusProductivityWindow2StartMinutes != data.focusProductivityWindow1StartMinutes ||
                data.focusProductivityWindow2EndMinutes != data.focusProductivityWindow1EndMinutes
            ) {
                add(
                    de.mm20.launcher2.preferences.FocusProductivityWindow(
                        startMinutes = data.focusProductivityWindow2StartMinutes,
                        endMinutes = data.focusProductivityWindow2EndMinutes,
                    )
                )
            }
        }
    }

    val launchOnEnter
        get() = launcherDataStore.data.map { it.searchLaunchOnEnter }.distinctUntilChanged()

    fun setLaunchOnEnter(launchOnEnter: Boolean) {
        launcherDataStore.update {
            it.copy(searchLaunchOnEnter = launchOnEnter)
        }
    }

    val hiddenItemsButton
        get() = launcherDataStore.data.map { it.hiddenItemsShowButton }.distinctUntilChanged()

    fun setHiddenItemsButton(hiddenItemsButton: Boolean) {
        launcherDataStore.update {
            it.copy(hiddenItemsShowButton = hiddenItemsButton)
        }
    }

    val favorites
        get() = launcherDataStore.data.map { it.favoritesEnabled }.distinctUntilChanged()

    fun setFavorites(favorites: Boolean) {
        launcherDataStore.update {
            it.copy(favoritesEnabled = favorites)
        }
    }

    val allApps
        get() = launcherDataStore.data.map { it.searchAllApps }.distinctUntilChanged()

    fun setAllApps(allAppsGrid: Boolean) {
        launcherDataStore.update {
            it.copy(searchAllApps = allAppsGrid)
        }
    }

    val openKeyboard
        get() = launcherDataStore.data.map { it.searchBarKeyboard }.distinctUntilChanged()

    fun setOpenKeyboard(openKeyboard: Boolean) {
        launcherDataStore.update {
            it.copy(searchBarKeyboard = openKeyboard)
        }
    }

    val reversedResults
        get() = launcherDataStore.data.map { it.searchResultsReversed }.distinctUntilChanged()

    fun setReversedResults(reversedResults: Boolean) {
        launcherDataStore.update {
            it.copy(searchResultsReversed = reversedResults)
        }
    }

    val separateWorkProfile
        get() = launcherDataStore.data.map { it.separateWorkProfile }.distinctUntilChanged()

    fun setSeparateWorkProfile(separateWorkProfile: Boolean) {
        launcherDataStore.update {
            it.copy(separateWorkProfile = separateWorkProfile)
        }
    }

    val focusModeEnabled
        get() = launcherDataStore.data.map { it.focusModeEnabled }.distinctUntilChanged()

    fun setFocusModeEnabled(enabled: Boolean) {
        launcherDataStore.update {
            it.copy(focusModeEnabled = enabled)
        }
    }

    val focusStrictSearch
        get() = launcherDataStore.data.map { it.focusStrictSearch }.distinctUntilChanged()

    fun setFocusStrictSearch(enabled: Boolean) {
        launcherDataStore.update {
            it.copy(focusStrictSearch = enabled)
        }
    }

    val focusEssentialAppKeys
        get() = launcherDataStore.data.map { it.focusEssentialAppKeys }.distinctUntilChanged()

    fun setFocusEssentialAppKeys(keys: Set<String>) {
        launcherDataStore.update { data ->
            data.copy(
                focusEssentialAppKeys = keys,
                focusDistractingAppKeys = data.focusDistractingAppKeys - keys,
            )
        }
    }

    fun addFocusEssentialAppKey(key: String) {
        launcherDataStore.update { data ->
            data.copy(
                focusEssentialAppKeys = data.focusEssentialAppKeys + key,
                focusDistractingAppKeys = data.focusDistractingAppKeys - key,
            )
        }
    }

    fun removeFocusEssentialAppKey(key: String) {
        launcherDataStore.update { data ->
            data.copy(focusEssentialAppKeys = data.focusEssentialAppKeys - key)
        }
    }

    val focusDistractingAppKeys
        get() = launcherDataStore.data.map { it.focusDistractingAppKeys }.distinctUntilChanged()

    fun setFocusDistractingAppKeys(keys: Set<String>) {
        launcherDataStore.update { data ->
            data.copy(
                focusDistractingAppKeys = keys,
                focusEssentialAppKeys = data.focusEssentialAppKeys - keys,
            )
        }
    }

    fun addFocusDistractingAppKey(key: String) {
        launcherDataStore.update { data ->
            data.copy(
                focusDistractingAppKeys = data.focusDistractingAppKeys + key,
                focusEssentialAppKeys = data.focusEssentialAppKeys - key,
            )
        }
    }

    fun removeFocusDistractingAppKey(key: String) {
        launcherDataStore.update { data ->
            data.copy(focusDistractingAppKeys = data.focusDistractingAppKeys - key)
        }
    }

    val focusHideDistractingApps
        get() = launcherDataStore.data.map { it.focusHideDistractingApps }.distinctUntilChanged()

    fun setFocusHideDistractingApps(enabled: Boolean) {
        launcherDataStore.update {
            it.copy(focusHideDistractingApps = enabled)
        }
    }

    val focusDefaultDelaySeconds
        get() = launcherDataStore.data.map { it.focusDefaultDelaySeconds }.distinctUntilChanged()

    fun setFocusDefaultDelaySeconds(seconds: Int) {
        launcherDataStore.update { it.copy(focusDefaultDelaySeconds = seconds) }
    }

    val focusDefaultSessionMinutes
        get() = launcherDataStore.data.map { it.focusDefaultSessionMinutes }.distinctUntilChanged()

    fun setFocusDefaultSessionMinutes(minutes: Int) {
        launcherDataStore.update { it.copy(focusDefaultSessionMinutes = minutes) }
    }

    val focusFadeDistractingApps
        get() = launcherDataStore.data.map { it.focusDesaturateDistractingApps }.distinctUntilChanged()

    fun setFocusFadeDistractingApps(enabled: Boolean) {
        launcherDataStore.update {
            it.copy(focusDesaturateDistractingApps = enabled)
        }
    }

    val focusSessionEndsAt
        get() = launcherDataStore.data.map { it.focusSessionEndsAt }.distinctUntilChanged()

    fun setFocusSessionEndsAt(timestamp: Long) {
        launcherDataStore.update {
            it.copy(focusSessionEndsAt = timestamp)
        }
    }

    val focusNoIconsMode
        get() = launcherDataStore.data.map { it.focusNoIconsMode }.distinctUntilChanged()

    fun setFocusNoIconsMode(enabled: Boolean) {
        launcherDataStore.update {
            it.copy(focusNoIconsMode = enabled)
        }
    }

    val focusEnableDnd
        get() = launcherDataStore.data.map { it.focusEnableDnd }.distinctUntilChanged()

    fun setFocusEnableDnd(enabled: Boolean) {
        launcherDataStore.update {
            it.copy(focusEnableDnd = enabled)
        }
    }

    val focusEmergencyBypassEndsAt
        get() = launcherDataStore.data.map { it.focusEmergencyBypassEndsAt }.distinctUntilChanged()

    fun setFocusEmergencyBypassEndsAt(timestamp: Long) {
        launcherDataStore.update {
            it.copy(focusEmergencyBypassEndsAt = timestamp)
        }
    }

    val focusEmergencyBypassReason
        get() = launcherDataStore.data.map { it.focusEmergencyBypassReason }.distinctUntilChanged()

    fun setFocusEmergencyBypassReason(reason: String?) {
        launcherDataStore.update {
            it.copy(focusEmergencyBypassReason = reason)
        }
    }

    val focusPreviousDndFilter
        get() = launcherDataStore.data.map { it.focusPreviousDndFilter }.distinctUntilChanged()

    fun setFocusPreviousDndFilter(filter: Int) {
        launcherDataStore.update {
            it.copy(focusPreviousDndFilter = filter)
        }
    }

    val focusProductivityTimeEnabled
        get() = launcherDataStore.data.map { it.focusProductivityTimeEnabled }.distinctUntilChanged()

    fun setFocusProductivityTimeEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusProductivityTimeEnabled = enabled) }
    }

    val focusProductivityWindows
        get() = launcherDataStore.data.map(::resolvedProductivityWindows).distinctUntilChanged()

    val focusDailyScheduleEnabled
        get() = launcherDataStore.data.map { it.focusDailyScheduleEnabled }.distinctUntilChanged()

    fun setFocusDailyScheduleEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusDailyScheduleEnabled = enabled) }
    }

    val focusDailyScheduleCalendarId
        get() = launcherDataStore.data.map { it.focusDailyScheduleCalendarId }.distinctUntilChanged()

    fun setFocusDailyScheduleCalendarId(calendarId: String?) {
        launcherDataStore.update { it.copy(focusDailyScheduleCalendarId = calendarId) }
    }

    val focusScheduleDockMappings
        get() = launcherDataStore.data.map { it.focusScheduleDockMappings }.distinctUntilChanged()

    fun upsertFocusScheduleDockMapping(eventName: String, appKeys: List<String>) {
        launcherDataStore.update { data ->
            val updated = data.focusScheduleDockMappings
                .filterNot { it.eventName == eventName } + ScheduleDockMapping(
                eventName = eventName,
                appKeys = appKeys.distinct(),
            )
            data.copy(focusScheduleDockMappings = updated.sortedBy { it.eventName.lowercase() })
        }
    }

    fun removeFocusScheduleDockMapping(eventName: String) {
        launcherDataStore.update { data ->
            data.copy(
                focusScheduleDockMappings = data.focusScheduleDockMappings.filterNot { it.eventName == eventName }
            )
        }
    }

    val focusHabitsEnabled
        get() = launcherDataStore.data.map { it.focusHabitsEnabled }.distinctUntilChanged()

    fun setFocusHabitsEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusHabitsEnabled = enabled) }
    }

    val focusHabits
        get() = launcherDataStore.data.map { it.focusHabits }.distinctUntilChanged()

    fun upsertFocusHabit(habit: FocusHabit) {
        launcherDataStore.update { data ->
            val updated = data.focusHabits.filterNot { it.id == habit.id } + habit
            data.copy(focusHabits = updated.sortedBy { it.deadlineMinutes })
        }
    }

    fun removeFocusHabit(id: String) {
        launcherDataStore.update { data ->
            data.copy(focusHabits = data.focusHabits.filterNot { it.id == id })
        }
    }

    fun setFocusHabitCompleted(id: String, date: String, completed: Boolean) {
        launcherDataStore.update { data ->
            data.copy(
                focusHabits = data.focusHabits.map { habit ->
                    if (habit.id != id) habit else habit.copy(
                        completedDates = if (completed) habit.completedDates + date else habit.completedDates - date
                    )
                }
            )
        }
    }

    val focusUpcomingEventsCalendarIds
        get() = launcherDataStore.data.map { it.focusUpcomingEventsCalendarIds }.distinctUntilChanged()

    fun setFocusUpcomingEventsCalendarIds(calendarIds: Set<String>) {
        launcherDataStore.update { it.copy(focusUpcomingEventsCalendarIds = calendarIds) }
    }

    fun addFocusProductivityWindow() {
        launcherDataStore.update { data ->
            val windows = resolvedProductivityWindows(data)
            if (windows.size >= 3) return@update data
            val lastWindow = windows.lastOrNull()
                ?: de.mm20.launcher2.preferences.FocusProductivityWindow(5 * 60, 9 * 60)
            data.copy(
                focusProductivityWindows = windows + lastWindow
            )
        }
    }

    fun removeFocusProductivityWindow(index: Int) {
        launcherDataStore.update { data ->
            val windows = resolvedProductivityWindows(data)
            if (windows.size <= 1 || index !in windows.indices) return@update data
            data.copy(
                focusProductivityWindows = windows.filterIndexed { currentIndex, _ ->
                    currentIndex != index
                }
            )
        }
    }

    fun updateFocusProductivityWindowStart(index: Int, minutes: Int) {
        launcherDataStore.update { data ->
            val windows = resolvedProductivityWindows(data).toMutableList()
            if (index !in windows.indices) return@update data
            windows[index] = windows[index].copy(startMinutes = minutes)
            data.copy(focusProductivityWindows = windows)
        }
    }

    fun updateFocusProductivityWindowEnd(index: Int, minutes: Int) {
        launcherDataStore.update { data ->
            val windows = resolvedProductivityWindows(data).toMutableList()
            if (index !in windows.indices) return@update data
            windows[index] = windows[index].copy(endMinutes = minutes)
            data.copy(focusProductivityWindows = windows)
        }
    }

    val focusApplyToPersonalProfile
        get() = launcherDataStore.data.map { it.focusApplyToPersonalProfile }.distinctUntilChanged()

    fun setFocusApplyToPersonalProfile(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusApplyToPersonalProfile = enabled) }
    }

    val focusApplyToWorkProfile
        get() = launcherDataStore.data.map { it.focusApplyToWorkProfile }.distinctUntilChanged()

    fun setFocusApplyToWorkProfile(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusApplyToWorkProfile = enabled) }
    }

    val focusApplyToPrivateProfile
        get() = launcherDataStore.data.map { it.focusApplyToPrivateProfile }.distinctUntilChanged()

    fun setFocusApplyToPrivateProfile(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusApplyToPrivateProfile = enabled) }
    }

    val focusCommuteModeEnabled
        get() = launcherDataStore.data.map { it.focusCommuteModeEnabled }.distinctUntilChanged()

    fun setFocusCommuteModeEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusCommuteModeEnabled = enabled) }
    }

    val focusAtAGlanceEnabled
        get() = launcherDataStore.data.map { it.focusAtAGlanceEnabled }.distinctUntilChanged()

    fun setFocusAtAGlanceEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusAtAGlanceEnabled = enabled) }
    }

    val focusStartRitualEnabled
        get() = launcherDataStore.data.map { it.focusStartRitualEnabled }.distinctUntilChanged()

    fun setFocusStartRitualEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusStartRitualEnabled = enabled) }
    }

    val focusMicroStepPromptEnabled
        get() = launcherDataStore.data.map { it.focusMicroStepPromptEnabled }.distinctUntilChanged()

    fun setFocusMicroStepPromptEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusMicroStepPromptEnabled = enabled) }
    }

    val focusRecoveryEnabled
        get() = launcherDataStore.data.map { it.focusRecoveryEnabled }.distinctUntilChanged()

    fun setFocusRecoveryEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusRecoveryEnabled = enabled) }
    }

    val focusRecoveryResumeTimeoutMinutes
        get() = launcherDataStore.data.map { it.focusRecoveryResumeTimeoutMinutes }.distinctUntilChanged()

    fun setFocusRecoveryResumeTimeoutMinutes(minutes: Int) {
        launcherDataStore.update { it.copy(focusRecoveryResumeTimeoutMinutes = minutes) }
    }

    val focusRecoveryFollowsCurrentBlockEnabled
        get() = launcherDataStore.data.map { it.focusRecoveryFollowsCurrentBlockEnabled }.distinctUntilChanged()

    fun setFocusRecoveryFollowsCurrentBlockEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusRecoveryFollowsCurrentBlockEnabled = enabled) }
    }

    val focusTransitionWarningsEnabled
        get() = launcherDataStore.data.map { it.focusTransitionWarningsEnabled }.distinctUntilChanged()

    fun setFocusTransitionWarningsEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusTransitionWarningsEnabled = enabled) }
    }

    val focusTransitionWarningLeadMinutes
        get() = launcherDataStore.data.map { it.focusTransitionWarningLeadMinutes }.distinctUntilChanged()

    fun setFocusTransitionWarningLeadMinutes(minutes: Int) {
        launcherDataStore.update { it.copy(focusTransitionWarningLeadMinutes = minutes) }
    }

    val focusBlockPrepPromptsEnabled
        get() = launcherDataStore.data.map { it.focusBlockPrepPromptsEnabled }.distinctUntilChanged()

    fun setFocusBlockPrepPromptsEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusBlockPrepPromptsEnabled = enabled) }
    }

    val focusPrepLeadTimeMinutes
        get() = launcherDataStore.data.map { it.focusPrepLeadTimeMinutes }.distinctUntilChanged()

    fun setFocusPrepLeadTimeMinutes(minutes: Int) {
        launcherDataStore.update { it.copy(focusPrepLeadTimeMinutes = minutes) }
    }

    val focusBlockAwareSessionSizingEnabled
        get() = launcherDataStore.data.map { it.focusBlockAwareSessionSizingEnabled }.distinctUntilChanged()

    fun setFocusBlockAwareSessionSizingEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusBlockAwareSessionSizingEnabled = enabled) }
    }

    val focusEscalatingFrictionEnabled
        get() = launcherDataStore.data.map { it.focusEscalatingFrictionEnabled }.distinctUntilChanged()

    fun setFocusEscalatingFrictionEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusEscalatingFrictionEnabled = enabled) }
    }

    val focusEscalationWindowMinutes
        get() = launcherDataStore.data.map { it.focusEscalationWindowMinutes }.distinctUntilChanged()

    fun setFocusEscalationWindowMinutes(minutes: Int) {
        launcherDataStore.update { it.copy(focusEscalationWindowMinutes = minutes) }
    }

    val focusEscalationExtraDelaySeconds
        get() = launcherDataStore.data.map { it.focusEscalationExtraDelaySeconds }.distinctUntilChanged()

    fun setFocusEscalationExtraDelaySeconds(seconds: Int) {
        launcherDataStore.update { it.copy(focusEscalationExtraDelaySeconds = seconds) }
    }

    val focusDistractingSessionCapMinutes
        get() = launcherDataStore.data.map { it.focusDistractingSessionCapMinutes }.distinctUntilChanged()

    fun setFocusDistractingSessionCapMinutes(minutes: Int) {
        launcherDataStore.update { it.copy(focusDistractingSessionCapMinutes = minutes) }
    }

    val focusResetButtonEnabled
        get() = launcherDataStore.data.map { it.focusResetButtonEnabled }.distinctUntilChanged()

    fun setFocusResetButtonEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusResetButtonEnabled = enabled) }
    }

    val focusLastResumeContext
        get() = launcherDataStore.data.map { it.focusLastResumeContext }.distinctUntilChanged()

    fun setFocusLastResumeContext(context: FocusResumeContext?) {
        launcherDataStore.update { it.copy(focusLastResumeContext = context) }
    }

    val focusBlockPlans
        get() = launcherDataStore.data.map { it.focusBlockPlans }.distinctUntilChanged()

    fun setFocusBlockPlans(plans: List<FocusBlockPlan>) {
        launcherDataStore.update { it.copy(focusBlockPlans = plans) }
    }

    fun upsertFocusBlockPlan(plan: FocusBlockPlan) {
        launcherDataStore.update { data ->
            data.copy(
                focusBlockPlans = data.focusBlockPlans
                    .filterNot { it.date == plan.date && it.normalizedBlockLabel == plan.normalizedBlockLabel } +
                    plan
            )
        }
    }

    fun removeFocusBlockPlan(date: String, normalizedBlockLabel: String) {
        launcherDataStore.update { data ->
            data.copy(
                focusBlockPlans = data.focusBlockPlans.filterNot {
                    it.date == date && it.normalizedBlockLabel == normalizedBlockLabel
                }
            )
        }
    }

    val focusAdaptiveFrictionMode
        get() = launcherDataStore.data.map { it.focusAdaptiveFrictionMode }.distinctUntilChanged()

    fun setFocusAdaptiveFrictionMode(mode: FocusAdaptiveFrictionMode) {
        launcherDataStore.update { it.copy(focusAdaptiveFrictionMode = mode) }
    }

    val focusEnvironmentContextEnabled
        get() = launcherDataStore.data.map { it.focusEnvironmentContextEnabled }.distinctUntilChanged()

    fun setFocusEnvironmentContextEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusEnvironmentContextEnabled = enabled) }
    }

    val focusEnvironmentChargingContextEnabled
        get() = launcherDataStore.data.map { it.focusEnvironmentChargingContextEnabled }.distinctUntilChanged()

    fun setFocusEnvironmentChargingContextEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusEnvironmentChargingContextEnabled = enabled) }
    }

    val focusEnvironmentExplainabilityEnabled
        get() = launcherDataStore.data.map { it.focusEnvironmentExplainabilityEnabled }.distinctUntilChanged()

    fun setFocusEnvironmentExplainabilityEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusEnvironmentExplainabilityEnabled = enabled) }
    }

    val focusReviewSuggestionsEnabled
        get() = launcherDataStore.data.map { it.focusReviewSuggestionsEnabled }.distinctUntilChanged()

    fun setFocusReviewSuggestionsEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusReviewSuggestionsEnabled = enabled) }
    }

    val focusDismissedRecommendationKeys
        get() = launcherDataStore.data.map { it.focusDismissedRecommendationKeys }.distinctUntilChanged()

    fun dismissFocusRecommendation(key: String) {
        launcherDataStore.update {
            it.copy(focusDismissedRecommendationKeys = it.focusDismissedRecommendationKeys + key)
        }
    }

    fun restoreFocusRecommendation(key: String) {
        launcherDataStore.update {
            it.copy(focusDismissedRecommendationKeys = it.focusDismissedRecommendationKeys - key)
        }
    }

    val focusActiveExperiment
        get() = launcherDataStore.data.map { it.focusActiveExperiment }.distinctUntilChanged()

    fun setFocusActiveExperiment(experiment: FocusExperiment?) {
        launcherDataStore.update { it.copy(focusActiveExperiment = experiment) }
    }

    val focusCompletedExperiments
        get() = launcherDataStore.data.map { it.focusCompletedExperiments }.distinctUntilChanged()

    fun addCompletedFocusExperiment(experiment: FocusExperiment) {
        launcherDataStore.update {
            it.copy(focusCompletedExperiments = it.focusCompletedExperiments + experiment)
        }
    }

}
