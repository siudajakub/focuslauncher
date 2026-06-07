package de.mm20.launcher2.ui.settings.focusreport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.preferences.Preference
import de.mm20.launcher2.ui.component.preferences.PreferenceCategory
import de.mm20.launcher2.ui.component.preferences.PreferenceScreen
import de.mm20.launcher2.ui.launcher.focus.FocusRecommendation
import de.mm20.launcher2.ui.launcher.focus.WeeklyFocusReport
import kotlinx.serialization.Serializable

@Serializable
data object FocusReportSettingsRoute : NavKey

@Composable
fun FocusReportSettingsScreen() {
    val viewModel: FocusReportSettingsScreenVM = viewModel()
    val report by viewModel.report.collectAsStateWithLifecycle(WeeklyFocusReport())
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle(emptyList())
    val rawActionSummary by viewModel.lastActionSummary.collectAsStateWithLifecycle(null)
    val actionSummary = rawActionSummary
    val lastActionSummary = when {
        actionSummary == null -> null
        actionSummary == R.string.focus_report_action_dismissed.toString() ->
            stringResource(R.string.focus_report_action_dismissed)
        actionSummary == R.string.focus_report_action_prep_enabled.toString() ->
            stringResource(R.string.focus_report_action_prep_enabled)
        actionSummary == R.string.focus_report_action_missing_app.toString() ->
            stringResource(R.string.focus_report_action_missing_app)
        actionSummary == R.string.focus_report_action_missing_habit.toString() ->
            stringResource(R.string.focus_report_action_missing_habit)
        actionSummary.startsWith("prep:") ->
            stringResource(R.string.focus_report_action_prep_updated, actionSummary.removePrefix("prep:"))
        actionSummary.startsWith("friction:") ->
            stringResource(R.string.focus_report_action_friction_updated, actionSummary.removePrefix("friction:"))
        actionSummary.startsWith("cap:") ->
            stringResource(R.string.focus_report_action_cap_updated, actionSummary.removePrefix("cap:"))
        actionSummary.startsWith("habit:") ->
            stringResource(R.string.focus_report_action_habit_updated, actionSummary.removePrefix("habit:"))
        else -> actionSummary
    }

    PreferenceScreen(title = stringResource(R.string.focus_report_title)) {
        if (lastActionSummary != null) {
            item {
                PreferenceCategory {
                    ReportStatPreference(title = lastActionSummary)
                }
            }
        }
        item {
            PreferenceCategory(title = stringResource(R.string.focus_report_summary)) {
                ReportStatPreference(
                    title = stringResource(R.string.focus_report_total_unlocks, report.totalUnlocks),
                )
                ReportStatPreference(
                    title = stringResource(R.string.focus_report_total_unlock_minutes, report.totalUnlockMinutes),
                )
                ReportStatPreference(
                    title = stringResource(R.string.focus_report_average_delay, report.averageDelaySeconds),
                )
                ReportStatPreference(
                    title = stringResource(R.string.focus_report_streak_days, report.streakDays),
                )
                ReportStatPreference(
                    title = stringResource(R.string.focus_report_in_session_unlocks, report.inSessionUnlocks),
                )
            }
        }
        item {
            PreferenceCategory(title = stringResource(R.string.focus_report_sessions_title)) {
                ReportStatPreference(
                    title = stringResource(R.string.focus_report_total_sessions, report.totalSessions),
                )
                ReportStatPreference(
                    title = stringResource(R.string.focus_report_total_session_minutes, report.totalSessionMinutes),
                )
                ReportStatPreference(
                    title = stringResource(R.string.focus_report_session_days, report.sessionDays),
                )
            }
        }
        item {
            PreferenceCategory(title = stringResource(R.string.focus_report_delta_title)) {
                ReportStatPreference(
                    title = stringResource(
                        R.string.focus_report_delta_unlocks,
                        formatSignedDelta(report.delta.unlocksDelta),
                    ),
                )
                ReportStatPreference(
                    title = stringResource(
                        R.string.focus_report_delta_session_minutes,
                        formatSignedDelta(report.delta.sessionMinutesDelta),
                    ),
                )
                ReportStatPreference(
                    title = report.delta.topBreakerLabel?.let {
                        stringResource(
                            R.string.focus_report_delta_breaker_named,
                            it,
                            formatSignedDelta(report.delta.topBreakerDelta),
                        )
                    } ?: stringResource(R.string.focus_report_delta_breaker_empty),
                )
            }
        }
        item {
            PreferenceCategory(title = stringResource(R.string.focus_report_alignment_title)) {
                ReportStatPreference(
                    title = stringResource(R.string.focus_report_scheduled_unlocks, report.scheduledBlockUnlocks),
                )
                ReportStatPreference(
                    title = stringResource(R.string.focus_report_recovery_accepted, report.recoveryAcceptedCount),
                )
                ReportStatPreference(
                    title = stringResource(R.string.focus_report_recovery_dismissed, report.recoveryDismissedCount),
                )
                if (report.topInterruptedBlocks.isEmpty()) {
                    ReportStatPreference(title = stringResource(R.string.focus_report_empty_blocks))
                } else {
                    report.topInterruptedBlocks.forEach { (label, count) ->
                        ReportStatPreference(
                            title = label,
                            summary = count.toString(),
                        )
                    }
                }
            }
        }
        item {
            PreferenceCategory(title = stringResource(R.string.focus_report_top_breakers)) {
                if (report.topFocusBreakers.isEmpty()) {
                    ReportStatPreference(title = stringResource(R.string.focus_report_empty_breakers))
                } else {
                    report.topFocusBreakers.forEach { (label, count) ->
                        ReportStatPreference(
                            title = label,
                            summary = count.toString(),
                        )
                    }
                }
            }
        }
        item {
            PreferenceCategory(title = stringResource(R.string.focus_report_top_reasons)) {
                if (report.topUnlockReasons.isEmpty()) {
                    ReportStatPreference(title = stringResource(R.string.focus_report_empty_reasons))
                } else {
                    report.topUnlockReasons.forEach { (reason, count) ->
                        ReportStatPreference(
                            title = reason,
                            summary = count.toString(),
                        )
                    }
                }
            }
        }
        item {
            PreferenceCategory(title = stringResource(R.string.focus_report_long_horizon_title)) {
                if (recommendations.isEmpty()) {
                    ReportStatPreference(title = stringResource(R.string.focus_report_long_horizon_empty))
                } else {
                    recommendations.forEach { recommendation ->
                        ReviewRecommendationPreference(
                            recommendation = recommendation,
                            onApply = { viewModel.applyRecommendation(recommendation) },
                            onDismiss = { viewModel.dismissRecommendation(recommendation.key) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportStatPreference(
    title: String,
    summary: String? = null,
) {
    Preference(
        title = title,
        summary = summary,
        onClick = {},
    )
}

private fun formatSignedDelta(value: Int): String {
    return when {
        value > 0 -> "+$value"
        else -> value.toString()
    }
}

@Composable
private fun ReviewRecommendationPreference(
    recommendation: FocusRecommendation,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    Preference(
        title = recommendation.title,
        summary = recommendation.summary,
        onClick = {},
        controls = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.focus_report_dismiss_action))
                }
                TextButton(onClick = onApply) {
                    Text(stringResource(R.string.focus_report_apply_action))
                }
            }
        },
    )
}
