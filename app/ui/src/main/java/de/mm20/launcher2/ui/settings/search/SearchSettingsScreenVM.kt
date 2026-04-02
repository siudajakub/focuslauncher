package de.mm20.launcher2.ui.settings.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SearchSettingsScreenVM : ViewModel(), KoinComponent {
    private val searchUiSettings: SearchUiSettings by inject()

    val favorites = searchUiSettings.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setFavorites(favorites: Boolean) {
        searchUiSettings.setFavorites(favorites)
    }

    val allApps = searchUiSettings.allApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setAllApps(allApps: Boolean) {
        searchUiSettings.setAllApps(allApps)
    }

    val autoFocus = searchUiSettings.openKeyboard

    fun setAutoFocus(autoFocus: Boolean) {
        searchUiSettings.setOpenKeyboard(autoFocus)
    }

    val launchOnEnter = searchUiSettings.launchOnEnter
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setLaunchOnEnter(launchOnEnter: Boolean) {
        searchUiSettings.setLaunchOnEnter(launchOnEnter)
    }

    val reverseSearchResults = searchUiSettings.reversedResults
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setReverseSearchResults(reverseSearchResults: Boolean) {
        searchUiSettings.setReversedResults(reverseSearchResults)
    }

    val focusModeEnabled = searchUiSettings.focusModeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setFocusModeEnabled(enabled: Boolean) {
        searchUiSettings.setFocusModeEnabled(enabled)
    }

    val focusHideDistractingApps = searchUiSettings.focusHideDistractingApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setFocusHideDistractingApps(enabled: Boolean) {
        searchUiSettings.setFocusHideDistractingApps(enabled)
    }

    val focusDefaultDelaySeconds = searchUiSettings.focusDefaultDelaySeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setFocusDefaultDelaySeconds(seconds: Int) {
        searchUiSettings.setFocusDefaultDelaySeconds(seconds)
    }

    val focusDefaultSessionMinutes = searchUiSettings.focusDefaultSessionMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setFocusDefaultSessionMinutes(minutes: Int) {
        searchUiSettings.setFocusDefaultSessionMinutes(minutes)
    }

    val focusFadeDistractingApps = searchUiSettings.focusFadeDistractingApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setFocusFadeDistractingApps(enabled: Boolean) {
        searchUiSettings.setFocusFadeDistractingApps(enabled)
    }

    val focusNoIconsMode = searchUiSettings.focusNoIconsMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setFocusNoIconsMode(enabled: Boolean) {
        searchUiSettings.setFocusNoIconsMode(enabled)
    }

    val focusEnableDnd = searchUiSettings.focusEnableDnd
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setFocusEnableDnd(enabled: Boolean) {
        searchUiSettings.setFocusEnableDnd(enabled)
    }

    val focusProductivityTimeEnabled = searchUiSettings.focusProductivityTimeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setFocusProductivityTimeEnabled(enabled: Boolean) {
        searchUiSettings.setFocusProductivityTimeEnabled(enabled)
    }

    val focusProductivityWindow1StartMinutes = searchUiSettings.focusProductivityWindow1StartMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setFocusProductivityWindow1StartMinutes(minutes: Int) {
        searchUiSettings.setFocusProductivityWindow1StartMinutes(minutes)
    }

    val focusProductivityWindow1EndMinutes = searchUiSettings.focusProductivityWindow1EndMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setFocusProductivityWindow1EndMinutes(minutes: Int) {
        searchUiSettings.setFocusProductivityWindow1EndMinutes(minutes)
    }

    val focusProductivityWindow2StartMinutes = searchUiSettings.focusProductivityWindow2StartMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setFocusProductivityWindow2StartMinutes(minutes: Int) {
        searchUiSettings.setFocusProductivityWindow2StartMinutes(minutes)
    }

    val focusProductivityWindow2EndMinutes = searchUiSettings.focusProductivityWindow2EndMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setFocusProductivityWindow2EndMinutes(minutes: Int) {
        searchUiSettings.setFocusProductivityWindow2EndMinutes(minutes)
    }
}
