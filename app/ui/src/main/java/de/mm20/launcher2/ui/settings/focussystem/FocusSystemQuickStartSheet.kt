package de.mm20.launcher2.ui.settings.focussystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.DismissableBottomSheet
import de.mm20.launcher2.ui.component.SmallMessage
import de.mm20.launcher2.ui.component.preferences.Preference
import de.mm20.launcher2.ui.component.preferences.PreferenceCategory

@Composable
fun FocusSystemQuickStartSheet(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onBalancedPreset: () -> Unit,
    onHardFocusPreset: () -> Unit,
    onMinimalPreset: () -> Unit,
    onOpenFocusApps: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenReport: () -> Unit,
) {
    DismissableBottomSheet(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.focus_system_quick_start_title),
                style = MaterialTheme.typography.titleLarge,
            )
            SmallMessage(
                icon = R.drawable.emoji_objects_24px,
                text = stringResource(R.string.focus_system_onboarding_summary),
            )

            FocusSystemQuickStartStep(
                step = stringResource(R.string.focus_system_quick_start_step_one_title),
                summary = stringResource(R.string.focus_system_quick_start_step_one_summary),
            )
            Preference(
                title = stringResource(R.string.focus_system_preset_balanced_title),
                summary = stringResource(R.string.focus_system_preset_balanced_summary),
                icon = R.drawable.tune_24px,
                onClick = onBalancedPreset,
            )
            Preference(
                title = stringResource(R.string.focus_system_preset_hard_title),
                summary = stringResource(R.string.focus_system_preset_hard_summary),
                icon = R.drawable.lock_24px,
                onClick = onHardFocusPreset,
            )
            Preference(
                title = stringResource(R.string.focus_system_preset_minimal_title),
                summary = stringResource(R.string.focus_system_preset_minimal_summary),
                icon = R.drawable.visibility_off_24px,
                onClick = onMinimalPreset,
            )

            PreferenceCategory(title = stringResource(R.string.focus_system_quick_start_next_title)) {
                Preference(
                    title = stringResource(R.string.focus_system_quick_start_step_two_title),
                    summary = stringResource(R.string.focus_system_quick_start_step_two_summary),
                    icon = R.drawable.apps_24px,
                    onClick = onOpenFocusApps,
                )
                Preference(
                    title = stringResource(R.string.focus_system_quick_start_step_three_title),
                    summary = stringResource(R.string.focus_system_quick_start_step_three_summary),
                    icon = R.drawable.schedule_24px,
                    onClick = onOpenSchedule,
                )
                Preference(
                    title = stringResource(R.string.focus_settings_daily_habits_title),
                    summary = stringResource(R.string.focus_system_quick_start_habits_summary),
                    icon = R.drawable.check_24px,
                    onClick = onOpenHabits,
                )
                Preference(
                    title = stringResource(R.string.focus_report_title),
                    summary = stringResource(R.string.focus_system_quick_start_report_summary),
                    icon = R.drawable.query_stats_24px,
                    onClick = onOpenReport,
                )
            }
        }
    }
}

@Composable
private fun FocusSystemQuickStartStep(
    step: String,
    summary: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = step,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
