package de.mm20.launcher2.preferences.ui

import de.mm20.launcher2.preferences.LauncherDataStore
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class SearchUiSettings internal constructor(
    private val launcherDataStore: LauncherDataStore,
) {
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

    val focusProductivityWindow1StartMinutes
        get() = launcherDataStore.data.map { it.focusProductivityWindow1StartMinutes }.distinctUntilChanged()

    fun setFocusProductivityWindow1StartMinutes(minutes: Int) {
        launcherDataStore.update { it.copy(focusProductivityWindow1StartMinutes = minutes) }
    }

    val focusProductivityWindow1EndMinutes
        get() = launcherDataStore.data.map { it.focusProductivityWindow1EndMinutes }.distinctUntilChanged()

    fun setFocusProductivityWindow1EndMinutes(minutes: Int) {
        launcherDataStore.update { it.copy(focusProductivityWindow1EndMinutes = minutes) }
    }

    val focusProductivityWindow2StartMinutes
        get() = launcherDataStore.data.map { it.focusProductivityWindow2StartMinutes }.distinctUntilChanged()

    fun setFocusProductivityWindow2StartMinutes(minutes: Int) {
        launcherDataStore.update { it.copy(focusProductivityWindow2StartMinutes = minutes) }
    }

    val focusProductivityWindow2EndMinutes
        get() = launcherDataStore.data.map { it.focusProductivityWindow2EndMinutes }.distinctUntilChanged()

    fun setFocusProductivityWindow2EndMinutes(minutes: Int) {
        launcherDataStore.update { it.copy(focusProductivityWindow2EndMinutes = minutes) }
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

}
