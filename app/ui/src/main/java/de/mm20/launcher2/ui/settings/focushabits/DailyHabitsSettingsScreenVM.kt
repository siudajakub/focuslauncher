package de.mm20.launcher2.ui.settings.focushabits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.mm20.launcher2.preferences.FocusHabit
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

class DailyHabitsSettingsScreenVM : ViewModel(), KoinComponent {
    private val searchUiSettings: SearchUiSettings by inject()

    val habitsEnabled = searchUiSettings.focusHabitsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val habits = searchUiSettings.focusHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun setHabitsEnabled(enabled: Boolean) {
        searchUiSettings.setFocusHabitsEnabled(enabled)
    }

    fun saveHabit(
        id: String?,
        title: String,
        deadlineMinutes: Int,
        completedDates: Set<String> = emptySet(),
    ) {
        searchUiSettings.upsertFocusHabit(
            FocusHabit(
                id = id ?: UUID.randomUUID().toString(),
                title = title.trim(),
                deadlineMinutes = deadlineMinutes,
                completedDates = completedDates,
            )
        )
    }

    fun removeHabit(id: String) {
        searchUiSettings.removeFocusHabit(id)
    }
}
