package de.mm20.launcher2.ui.settings.focussupport

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.preferences.ListPreference
import de.mm20.launcher2.ui.component.preferences.PreferenceCategory
import de.mm20.launcher2.ui.component.preferences.PreferenceScreen
import de.mm20.launcher2.ui.component.preferences.SwitchPreference
import kotlinx.serialization.Serializable

@Serializable
data object FocusSupportSettingsRoute : NavKey

@Composable
fun FocusSupportSettingsScreen() {
    val viewModel: FocusSupportSettingsScreenVM = viewModel()

    val startRitualEnabled = viewModel.startRitualEnabled.collectAsStateWithLifecycle(null).value
    val microStepPromptEnabled = viewModel.microStepPromptEnabled.collectAsStateWithLifecycle(null).value
    val recoveryEnabled = viewModel.recoveryEnabled.collectAsStateWithLifecycle(null).value
    val recoveryResumeTimeoutMinutes = viewModel.recoveryResumeTimeoutMinutes.collectAsStateWithLifecycle(null).value
    val recoveryFollowsCurrentBlockEnabled = viewModel.recoveryFollowsCurrentBlockEnabled.collectAsStateWithLifecycle(null).value
    val transitionWarningsEnabled = viewModel.transitionWarningsEnabled.collectAsStateWithLifecycle(null).value
    val transitionWarningLeadMinutes = viewModel.transitionWarningLeadMinutes.collectAsStateWithLifecycle(null).value
    val blockPrepPromptsEnabled = viewModel.blockPrepPromptsEnabled.collectAsStateWithLifecycle(null).value
    val prepLeadTimeMinutes = viewModel.prepLeadTimeMinutes.collectAsStateWithLifecycle(null).value
    val blockAwareSessionSizingEnabled = viewModel.blockAwareSessionSizingEnabled.collectAsStateWithLifecycle(null).value
    val escalatingFrictionEnabled = viewModel.escalatingFrictionEnabled.collectAsStateWithLifecycle(null).value
    val escalationWindowMinutes = viewModel.escalationWindowMinutes.collectAsStateWithLifecycle(null).value
    val escalationExtraDelaySeconds = viewModel.escalationExtraDelaySeconds.collectAsStateWithLifecycle(null).value
    val distractingSessionCapMinutes = viewModel.distractingSessionCapMinutes.collectAsStateWithLifecycle(null).value
    val resetButtonEnabled = viewModel.resetButtonEnabled.collectAsStateWithLifecycle(null).value

    PreferenceScreen(title = stringResource(R.string.focus_support_title)) {
        item {
            PreferenceCategory(title = stringResource(R.string.focus_support_start_title)) {
                SwitchPreference(
                    title = stringResource(R.string.focus_support_start_ritual_title),
                    summary = stringResource(R.string.focus_support_start_ritual_summary),
                    icon = R.drawable.play_arrow_24px,
                    value = startRitualEnabled == true,
                    onValueChanged = viewModel::setStartRitualEnabled,
                )
                SwitchPreference(
                    title = stringResource(R.string.focus_support_micro_step_title),
                    summary = stringResource(R.string.focus_support_micro_step_summary),
                    icon = R.drawable.check_24px,
                    value = microStepPromptEnabled == true,
                    onValueChanged = viewModel::setMicroStepPromptEnabled,
                )
            }
        }
        item {
            PreferenceCategory(title = stringResource(R.string.focus_support_recovery_title)) {
                SwitchPreference(
                    title = stringResource(R.string.focus_support_recovery_enabled_title),
                    summary = stringResource(R.string.focus_support_recovery_enabled_summary),
                    icon = R.drawable.autorenew_24px,
                    value = recoveryEnabled == true,
                    onValueChanged = viewModel::setRecoveryEnabled,
                )
                ListPreference(
                    title = stringResource(R.string.focus_support_recovery_timeout_title),
                    items = listOf(5, 10, 15, 20, 30).map { "$it min" to it },
                    value = recoveryResumeTimeoutMinutes,
                    onValueChanged = { if (it != null) viewModel.setRecoveryResumeTimeoutMinutes(it) },
                    icon = R.drawable.timer_24px,
                )
                SwitchPreference(
                    title = stringResource(R.string.focus_support_recovery_follows_block_title),
                    summary = stringResource(R.string.focus_support_recovery_follows_block_summary),
                    icon = R.drawable.autorenew_24px,
                    value = recoveryFollowsCurrentBlockEnabled == true,
                    onValueChanged = viewModel::setRecoveryFollowsCurrentBlockEnabled,
                )
                SwitchPreference(
                    title = stringResource(R.string.focus_support_reset_button_title),
                    summary = stringResource(R.string.focus_support_reset_button_summary),
                    icon = R.drawable.restart_alt_24px,
                    value = resetButtonEnabled == true,
                    onValueChanged = viewModel::setResetButtonEnabled,
                )
            }
        }
        item {
            PreferenceCategory(title = stringResource(R.string.focus_support_time_title)) {
                SwitchPreference(
                    title = stringResource(R.string.focus_support_transition_warnings_title),
                    summary = stringResource(R.string.focus_support_transition_warnings_summary),
                    icon = R.drawable.alarm_24px,
                    value = transitionWarningsEnabled == true,
                    onValueChanged = viewModel::setTransitionWarningsEnabled,
                )
                ListPreference(
                    title = stringResource(R.string.focus_support_transition_warning_lead_title),
                    items = listOf(5, 10, 15, 20, 30).map { "$it min" to it },
                    value = transitionWarningLeadMinutes,
                    onValueChanged = { if (it != null) viewModel.setTransitionWarningLeadMinutes(it) },
                    icon = R.drawable.schedule_24px,
                )
                SwitchPreference(
                    title = stringResource(R.string.focus_support_block_prep_title),
                    summary = stringResource(R.string.focus_support_block_prep_summary),
                    icon = R.drawable.event_24px,
                    value = blockPrepPromptsEnabled == true,
                    onValueChanged = viewModel::setBlockPrepPromptsEnabled,
                )
                ListPreference(
                    title = stringResource(R.string.focus_support_prep_lead_title),
                    items = listOf(5, 10, 15, 20, 30).map { "$it min" to it },
                    value = prepLeadTimeMinutes,
                    onValueChanged = { if (it != null) viewModel.setPrepLeadTimeMinutes(it) },
                    icon = R.drawable.schedule_24px,
                )
            }
        }
        item {
            PreferenceCategory(title = stringResource(R.string.focus_support_protection_title)) {
                SwitchPreference(
                    title = stringResource(R.string.focus_support_block_aware_session_sizing_title),
                    summary = stringResource(R.string.focus_support_block_aware_session_sizing_summary),
                    icon = R.drawable.schedule_24px,
                    value = blockAwareSessionSizingEnabled == true,
                    onValueChanged = viewModel::setBlockAwareSessionSizingEnabled,
                )
                SwitchPreference(
                    title = stringResource(R.string.focus_support_escalating_friction_title),
                    summary = stringResource(R.string.focus_support_escalating_friction_summary),
                    icon = R.drawable.warning_24px,
                    value = escalatingFrictionEnabled == true,
                    onValueChanged = viewModel::setEscalatingFrictionEnabled,
                )
                ListPreference(
                    title = stringResource(R.string.focus_support_escalation_window_title),
                    items = listOf(5, 10, 15, 20, 30).map { "$it min" to it },
                    value = escalationWindowMinutes,
                    onValueChanged = { if (it != null) viewModel.setEscalationWindowMinutes(it) },
                    icon = R.drawable.autorenew_24px,
                )
                ListPreference(
                    title = stringResource(R.string.focus_support_escalation_extra_delay_title),
                    items = listOf(5, 10, 15, 20).map { "${it}s" to it },
                    value = escalationExtraDelaySeconds,
                    onValueChanged = { if (it != null) viewModel.setEscalationExtraDelaySeconds(it) },
                    icon = R.drawable.timer_24px,
                )
                ListPreference(
                    title = stringResource(R.string.focus_support_session_cap_title),
                    items = listOf(5, 10, 15, 20, 30).map { "$it min" to it },
                    value = distractingSessionCapMinutes,
                    onValueChanged = { if (it != null) viewModel.setDistractingSessionCapMinutes(it) },
                    icon = R.drawable.schedule_24px,
                )
            }
        }
    }
}
