package de.mm20.launcher2.ui.launcher.focus

import de.mm20.launcher2.services.focus.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.mm20.launcher2.ui.R
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import de.mm20.launcher2.ui.locals.LocalBackStack

@Serializable
data object FocusInsightsRoute : NavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusInsightsScreen(
    viewModel: FocusInsightsVM = viewModel(),
) {
    val report by viewModel.weeklyReport.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    val rawActionSummary by viewModel.lastActionSummary.collectAsStateWithLifecycle()
    val backStack = LocalBackStack.current
    val snackbarHostState = remember { SnackbarHostState() }

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

    LaunchedEffect(lastActionSummary) {
        if (lastActionSummary != null) {
            snackbarHostState.showSnackbar(lastActionSummary)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.focus_insights_title)) },
                navigationIcon = {
                    IconButton(onClick = { backStack.removeLastOrNull() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.menu_back))
                    }
                }
            )
        }
    ) { padding ->
        val currentReport = report
        if (currentReport == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        InsightStatCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.focus_insights_streak),
                            value = currentReport.streakDays.toString(),
                            icon = Icons.Default.Star,
                        )
                        InsightStatCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.focus_insights_sessions),
                            value = currentReport.totalSessions.toString(),
                            icon = Icons.Default.DateRange,
                        )
                    }
                }

                item {
                    InsightStatCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.focus_insights_distractions),
                        value = currentReport.totalUnlocks.toString(),
                        subtitle = stringResource(R.string.focus_insights_distractions_time, currentReport.totalUnlockMinutes),
                        icon = Icons.Default.Warning,
                    )
                }

                if (currentReport.topFocusBreakers.isNotEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = stringResource(R.string.focus_insights_top_breakers),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }

                                currentReport.topFocusBreakers.forEach { (appName, count) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = appName,
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                        Text(
                                            text = count.toString(),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (currentReport.topUnlockReasons.isNotEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = stringResource(R.string.focus_report_top_reasons),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }

                                currentReport.topUnlockReasons.forEach { (reason, count) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = reason,
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                        Text(
                                            text = count.toString(),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (recommendations.isNotEmpty()) {
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.focus_report_long_horizon_title),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            recommendations.forEach { recommendation ->
                                InsightRecommendationCard(
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
    }
}

@Composable
private fun InsightStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightRecommendationCard(
    recommendation: FocusRecommendation,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text(
                    text = stringResource(
                        recommendation.titleRes,
                        *recommendation.titleArgs.toTypedArray()
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val summaryRes = recommendation.summaryRes
                if (summaryRes != null) {
                    Text(
                        text = stringResource(
                            summaryRes,
                            *recommendation.summaryArgs.toTypedArray()
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.focus_report_dismiss_action))
                }
                Spacer(modifier = Modifier.size(8.dp))
                TextButton(onClick = onApply) {
                    Text(stringResource(R.string.focus_report_apply_action))
                }
            }
        }
    }
}
