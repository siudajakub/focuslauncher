package de.mm20.launcher2.ui.settings.focussupport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FocusSupportSettingsScreenVM : ViewModel(), KoinComponent {
    private val searchUiSettings: SearchUiSettings by inject()

    val startRitualEnabled = searchUiSettings.focusStartRitualEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setStartRitualEnabled(enabled: Boolean) {
        searchUiSettings.setFocusStartRitualEnabled(enabled)
    }

    val microStepPromptEnabled = searchUiSettings.focusMicroStepPromptEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setMicroStepPromptEnabled(enabled: Boolean) {
        searchUiSettings.setFocusMicroStepPromptEnabled(enabled)
    }

    val recoveryEnabled = searchUiSettings.focusRecoveryEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setRecoveryEnabled(enabled: Boolean) {
        searchUiSettings.setFocusRecoveryEnabled(enabled)
    }

    val recoveryResumeTimeoutMinutes = searchUiSettings.focusRecoveryResumeTimeoutMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setRecoveryResumeTimeoutMinutes(minutes: Int) {
        searchUiSettings.setFocusRecoveryResumeTimeoutMinutes(minutes)
    }

    val recoveryFollowsCurrentBlockEnabled = searchUiSettings.focusRecoveryFollowsCurrentBlockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setRecoveryFollowsCurrentBlockEnabled(enabled: Boolean) {
        searchUiSettings.setFocusRecoveryFollowsCurrentBlockEnabled(enabled)
    }

    val transitionWarningsEnabled = searchUiSettings.focusTransitionWarningsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setTransitionWarningsEnabled(enabled: Boolean) {
        searchUiSettings.setFocusTransitionWarningsEnabled(enabled)
    }

    val transitionWarningLeadMinutes = searchUiSettings.focusTransitionWarningLeadMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setTransitionWarningLeadMinutes(minutes: Int) {
        searchUiSettings.setFocusTransitionWarningLeadMinutes(minutes)
    }

    val blockPrepPromptsEnabled = searchUiSettings.focusBlockPrepPromptsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setBlockPrepPromptsEnabled(enabled: Boolean) {
        searchUiSettings.setFocusBlockPrepPromptsEnabled(enabled)
    }

    val prepLeadTimeMinutes = searchUiSettings.focusPrepLeadTimeMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setPrepLeadTimeMinutes(minutes: Int) {
        searchUiSettings.setFocusPrepLeadTimeMinutes(minutes)
    }

    val blockAwareSessionSizingEnabled = searchUiSettings.focusBlockAwareSessionSizingEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setBlockAwareSessionSizingEnabled(enabled: Boolean) {
        searchUiSettings.setFocusBlockAwareSessionSizingEnabled(enabled)
    }

    val escalatingFrictionEnabled = searchUiSettings.focusEscalatingFrictionEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setEscalatingFrictionEnabled(enabled: Boolean) {
        searchUiSettings.setFocusEscalatingFrictionEnabled(enabled)
    }

    val escalationWindowMinutes = searchUiSettings.focusEscalationWindowMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setEscalationWindowMinutes(minutes: Int) {
        searchUiSettings.setFocusEscalationWindowMinutes(minutes)
    }

    val escalationExtraDelaySeconds = searchUiSettings.focusEscalationExtraDelaySeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setEscalationExtraDelaySeconds(seconds: Int) {
        searchUiSettings.setFocusEscalationExtraDelaySeconds(seconds)
    }

    val distractingSessionCapMinutes = searchUiSettings.focusDistractingSessionCapMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setDistractingSessionCapMinutes(minutes: Int) {
        searchUiSettings.setFocusDistractingSessionCapMinutes(minutes)
    }

    val resetButtonEnabled = searchUiSettings.focusResetButtonEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setResetButtonEnabled(enabled: Boolean) {
        searchUiSettings.setFocusResetButtonEnabled(enabled)
    }
}
