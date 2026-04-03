package de.mm20.launcher2.ui.settings.focusreport

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.preferences.Preference
import de.mm20.launcher2.ui.component.preferences.PreferenceCategory
import de.mm20.launcher2.ui.component.preferences.PreferenceScreen
import de.mm20.launcher2.ui.launcher.focus.FocusHistoryRepository
import de.mm20.launcher2.ui.launcher.focus.FocusRecommendation
import de.mm20.launcher2.ui.launcher.focus.FocusReviewInputs
import de.mm20.launcher2.ui.launcher.focus.WeeklyFocusReport
import de.mm20.launcher2.ui.launcher.focus.resolveRecommendations
import kotlinx.serialization.Serializable

@Serializable
data object FocusReportSettingsRoute : NavKey

@Composable
fun FocusReportSettingsScreen() {
    val viewModel: FocusReportSettingsScreenVM = viewModel()
    val report by viewModel.report.collectAsStateWithLifecycle(WeeklyFocusReport())

    PreferenceScreen(title = stringResource(R.string.focus_report_title)) {
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
            PreferenceCategory(title = "Long-horizon review") {
                val recommendations = resolveRecommendations(
                    inputs = report.toReviewInputs(),
                    dismissedKeys = emptySet(),
                    limit = 2,
                )
                if (recommendations.isEmpty()) {
                    ReportStatPreference(title = "No long-horizon signals yet.")
                } else {
                    recommendations.forEach { recommendation ->
                        ReviewRecommendationPreference(recommendation)
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

@Composable
private fun ReviewRecommendationPreference(recommendation: FocusRecommendation) {
    Preference(
        title = recommendation.title,
        summary = recommendation.summary,
        onClick = {},
    )
}

private fun WeeklyFocusReport.toReviewInputs(): FocusReviewInputs {
    val topBreaker = topFocusBreakers.firstOrNull()
    return FocusReviewInputs(
        topBreakingAppKey = topBreaker?.first
            ?.lowercase()
            ?.replace(Regex("[^a-z0-9]+"), "-")
            ?.trim('-')
            ?.takeIf { it.isNotBlank() },
        topBreakingAppLabel = topBreaker?.first,
        repeatedMismatchCount = scheduledBlockUnlocks + inSessionUnlocks + recoveryDismissedCount,
        prepPromptEnabled = true,
        prepLeadMinutes = 10,
    )
}

class FocusReportSettingsScreenVM : androidx.lifecycle.ViewModel() {
    private val repository = FocusHistoryRepository()
    val report = repository.getWeeklyReport()
}
