package de.mm20.launcher2.ui.settings.focusreport

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.preferences.PreferenceCategory
import de.mm20.launcher2.ui.component.preferences.PreferenceScreen
import de.mm20.launcher2.ui.launcher.focus.FocusHistoryRepository
import de.mm20.launcher2.ui.launcher.focus.WeeklyFocusReport
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
                Text(
                    text = stringResource(R.string.focus_report_total_unlocks, report.totalUnlocks),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
                Text(
                    text = stringResource(R.string.focus_report_total_unlock_minutes, report.totalUnlockMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Text(
                    text = stringResource(R.string.focus_report_average_delay, report.averageDelaySeconds),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Text(
                    text = stringResource(R.string.focus_report_streak_days, report.streakDays),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Text(
                    text = stringResource(R.string.focus_report_bypass_unlocks, report.bypassUnlocks),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Text(
                    text = stringResource(R.string.focus_report_in_session_unlocks, report.inSessionUnlocks),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        item {
            PreferenceCategory(title = stringResource(R.string.focus_report_sessions_title)) {
                Text(
                    text = stringResource(R.string.focus_report_total_sessions, report.totalSessions),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
                Text(
                    text = stringResource(R.string.focus_report_total_session_minutes, report.totalSessionMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Text(
                    text = stringResource(R.string.focus_report_session_days, report.sessionDays),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        item {
            PreferenceCategory(title = stringResource(R.string.focus_report_top_breakers)) {
                if (report.topFocusBreakers.isEmpty()) {
                    Text(
                        text = stringResource(R.string.focus_report_empty_breakers),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                } else {
                    Column {
                        report.topFocusBreakers.forEach { (label, count) ->
                            Text(
                                text = stringResource(R.string.focus_report_breaker_item, label, count),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }
        item {
            PreferenceCategory(title = stringResource(R.string.focus_report_top_reasons)) {
                if (report.topUnlockReasons.isEmpty()) {
                    Text(
                        text = stringResource(R.string.focus_report_empty_reasons),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                } else {
                    Column {
                        report.topUnlockReasons.forEach { (reason, count) ->
                            Text(
                                text = stringResource(R.string.focus_report_reason_item, reason, count),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

class FocusReportSettingsScreenVM : androidx.lifecycle.ViewModel() {
    private val repository = FocusHistoryRepository()
    val report = repository.getWeeklyReport()
}
