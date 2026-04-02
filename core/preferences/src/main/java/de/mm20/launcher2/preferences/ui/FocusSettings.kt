package de.mm20.launcher2.preferences.ui

import de.mm20.launcher2.preferences.LauncherDataStore
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
    val productivityWindow1StartMinutes: Int = 5 * 60,
    val productivityWindow1EndMinutes: Int = 9 * 60,
    val productivityWindow2StartMinutes: Int = 22 * 60,
    val productivityWindow2EndMinutes: Int = 7 * 60,
    val applyToPersonalProfile: Boolean = true,
    val applyToWorkProfile: Boolean = true,
    val applyToPrivateProfile: Boolean = true,
    val commuteModeEnabled: Boolean = false,
    val atAGlanceEnabled: Boolean = true,
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
            productivityWindow1StartMinutes = it.focusProductivityWindow1StartMinutes,
            productivityWindow1EndMinutes = it.focusProductivityWindow1EndMinutes,
            productivityWindow2StartMinutes = it.focusProductivityWindow2StartMinutes,
            productivityWindow2EndMinutes = it.focusProductivityWindow2EndMinutes,
            applyToPersonalProfile = it.focusApplyToPersonalProfile,
            applyToWorkProfile = it.focusApplyToWorkProfile,
            applyToPrivateProfile = it.focusApplyToPrivateProfile,
            commuteModeEnabled = it.focusCommuteModeEnabled,
            atAGlanceEnabled = it.focusAtAGlanceEnabled,
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
}
