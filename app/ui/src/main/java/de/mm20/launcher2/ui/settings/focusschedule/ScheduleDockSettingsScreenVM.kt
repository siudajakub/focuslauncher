package de.mm20.launcher2.ui.settings.focusschedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.mm20.launcher2.applications.AppRepository
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import de.mm20.launcher2.search.Application
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ScheduleDockSettingsScreenVM : ViewModel(), KoinComponent {
    private val searchUiSettings: SearchUiSettings by inject()
    private val appRepository: AppRepository by inject()

    val allApps = appRepository.findMany()
        .map { apps -> apps.sortedBy { (it.labelOverride ?: it.label).lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val scheduleDockMappings = searchUiSettings.focusScheduleDockMappings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun upsertScheduleDockMapping(eventName: String, appKeys: List<String>) {
        searchUiSettings.upsertFocusScheduleDockMapping(eventName, appKeys)
    }

    fun removeScheduleDockMapping(eventName: String) {
        searchUiSettings.removeFocusScheduleDockMapping(eventName)
    }

    fun appLabelForKey(appKey: String): String? {
        return allApps.value.firstOrNull { it.key == appKey }?.labelOverride
            ?: allApps.value.firstOrNull { it.key == appKey }?.label
    }
}
