package de.mm20.launcher2.ui.settings.focusapps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.mm20.launcher2.applications.AppRepository
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import de.mm20.launcher2.search.Application
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FocusAppsSettingsScreenVM : ViewModel(), KoinComponent {
    private val appRepository: AppRepository by inject()
    private val searchUiSettings: SearchUiSettings by inject()

    val allApps = appRepository.findMany()
        .map { apps -> apps.sortedBy { (it.labelOverride ?: it.label).lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val essentialApps = combine(
        allApps,
        searchUiSettings.focusEssentialAppKeys,
    ) { apps, keys ->
        apps.filter { it.key in keys }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val distractingApps = combine(
        allApps,
        searchUiSettings.focusDistractingAppKeys,
    ) { apps, keys ->
        apps.filter { it.key in keys }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun addEssential(key: String) {
        searchUiSettings.addFocusEssentialAppKey(key)
    }

    fun removeEssential(key: String) {
        searchUiSettings.removeFocusEssentialAppKey(key)
    }

    fun addDistracting(key: String) {
        searchUiSettings.addFocusDistractingAppKey(key)
    }

    fun removeDistracting(key: String) {
        searchUiSettings.removeFocusDistractingAppKey(key)
    }
}
