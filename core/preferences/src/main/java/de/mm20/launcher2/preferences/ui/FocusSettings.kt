package de.mm20.launcher2.preferences.ui

import de.mm20.launcher2.preferences.LauncherDataStore
import de.mm20.launcher2.preferences.FocusHabit
import de.mm20.launcher2.preferences.ScheduleDockMapping
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

data class FocusSettingsData(
    val enabled: Boolean = true,
    val strictSearch: Boolean = true,
    val hideDistractingApps: Boolean = true,
    val defaultDelaySeconds: Int = 10,
    val defaultSessionMinutes: Int = 15,
    val quietHoursEnabled: Boolean = true,
    val quietHoursStartMinutes: Int = 20 * 60,
    val quietHoursEndMinutes: Int = 8 * 60,
    val desaturateDistractingApps: Boolean = true,
    val noIconsMode: Boolean = false,
    val enableDnd: Boolean = false,
    val productivityTimeEnabled: Boolean = false,
    val productivityWindows: List<de.mm20.launcher2.preferences.FocusProductivityWindow> = emptyList(),
    val dailyScheduleEnabled: Boolean = false,
    val dailyScheduleCalendarId: String? = null,
    val scheduleDockMappings: List<ScheduleDockMapping> = emptyList(),
    val habitsEnabled: Boolean = false,
    val habits: List<FocusHabit> = emptyList(),
    val applyToPersonalProfile: Boolean = true,
    val applyToWorkProfile: Boolean = true,
    val applyToPrivateProfile: Boolean = true,
    val commuteModeEnabled: Boolean = false,
    val atAGlanceEnabled: Boolean = true,
    val adaptiveFrictionMode: de.mm20.launcher2.preferences.FocusAdaptiveFrictionMode =
        de.mm20.launcher2.preferences.FocusAdaptiveFrictionMode.Auto,
    val environmentContextEnabled: Boolean = true,
    val environmentExplainabilityEnabled: Boolean = true,
    val reviewSuggestionsEnabled: Boolean = true,
)

class FocusSettings internal constructor(
    private val launcherDataStore: LauncherDataStore,
) {
    val data = launcherDataStore.data.map {
        FocusSettingsData(
            enabled = it.focusModeEnabled,
            strictSearch = it.focusStrictSearch,
            hideDistractingApps = it.focusHideDistractingApps,
            defaultDelaySeconds = it.focusDefaultDelaySeconds,
            defaultSessionMinutes = it.focusDefaultSessionMinutes,
            quietHoursEnabled = it.focusQuietHoursEnabled,
            quietHoursStartMinutes = it.focusQuietHoursStartMinutes,
            quietHoursEndMinutes = it.focusQuietHoursEndMinutes,
            desaturateDistractingApps = it.focusDesaturateDistractingApps,
            noIconsMode = it.focusNoIconsMode,
            enableDnd = it.focusEnableDnd,
            productivityTimeEnabled = it.focusProductivityTimeEnabled,
            productivityWindows = it.focusProductivityWindows,
            dailyScheduleEnabled = it.focusDailyScheduleEnabled,
            dailyScheduleCalendarId = it.focusDailyScheduleCalendarId,
            scheduleDockMappings = it.focusScheduleDockMappings,
            habitsEnabled = it.focusHabitsEnabled,
            habits = it.focusHabits,
            applyToPersonalProfile = it.focusApplyToPersonalProfile,
            applyToWorkProfile = it.focusApplyToWorkProfile,
            applyToPrivateProfile = it.focusApplyToPrivateProfile,
            commuteModeEnabled = it.focusCommuteModeEnabled,
            atAGlanceEnabled = it.focusAtAGlanceEnabled,
            adaptiveFrictionMode = it.focusAdaptiveFrictionMode,
            environmentContextEnabled = it.focusEnvironmentContextEnabled,
            environmentExplainabilityEnabled = it.focusEnvironmentExplainabilityEnabled,
            reviewSuggestionsEnabled = it.focusReviewSuggestionsEnabled,
        )
    }.distinctUntilChanged()

    fun setEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusModeEnabled = enabled) }
    }

    fun setStrictSearch(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusStrictSearch = enabled) }
    }

    fun setHideDistractingApps(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusHideDistractingApps = enabled) }
    }

    fun setDefaultDelaySeconds(seconds: Int) {
        launcherDataStore.update { it.copy(focusDefaultDelaySeconds = seconds) }
    }

    fun setDefaultSessionMinutes(minutes: Int) {
        launcherDataStore.update { it.copy(focusDefaultSessionMinutes = minutes) }
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusQuietHoursEnabled = enabled) }
    }

    fun setQuietHoursStartMinutes(minutes: Int) {
        launcherDataStore.update { it.copy(focusQuietHoursStartMinutes = minutes) }
    }

    fun setQuietHoursEndMinutes(minutes: Int) {
        launcherDataStore.update { it.copy(focusQuietHoursEndMinutes = minutes) }
    }

    fun setDesaturateDistractingApps(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusDesaturateDistractingApps = enabled) }
    }

    fun setNoIconsMode(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusNoIconsMode = enabled) }
    }

    fun setEnableDnd(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusEnableDnd = enabled) }
    }

    fun setProductivityTimeEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusProductivityTimeEnabled = enabled) }
    }

    fun setApplyToPersonalProfile(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusApplyToPersonalProfile = enabled) }
    }

    fun setApplyToWorkProfile(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusApplyToWorkProfile = enabled) }
    }

    fun setApplyToPrivateProfile(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusApplyToPrivateProfile = enabled) }
    }

    fun setCommuteModeEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusCommuteModeEnabled = enabled) }
    }

    fun setAtAGlanceEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusAtAGlanceEnabled = enabled) }
    }

    fun setAdaptiveFrictionMode(mode: de.mm20.launcher2.preferences.FocusAdaptiveFrictionMode) {
        launcherDataStore.update { it.copy(focusAdaptiveFrictionMode = mode) }
    }

    fun setEnvironmentContextEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusEnvironmentContextEnabled = enabled) }
    }

    fun setEnvironmentExplainabilityEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusEnvironmentExplainabilityEnabled = enabled) }
    }

    fun setReviewSuggestionsEnabled(enabled: Boolean) {
        launcherDataStore.update { it.copy(focusReviewSuggestionsEnabled = enabled) }
    }
}
