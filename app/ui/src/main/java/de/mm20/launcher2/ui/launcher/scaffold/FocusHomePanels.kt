package de.mm20.launcher2.ui.launcher.scaffold

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.mm20.launcher2.search.Application
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.launcher.focus.DailyScheduleBlock
import de.mm20.launcher2.ui.launcher.focus.DailyScheduleSnapshot
import de.mm20.launcher2.ui.launcher.focus.FocusGuidanceState
import de.mm20.launcher2.ui.launcher.focus.FocusGuidanceType
import de.mm20.launcher2.ui.launcher.focus.HabitGateState
import de.mm20.launcher2.ui.launcher.focus.HabitStatus
import de.mm20.launcher2.ui.launcher.focus.TransitionWarningState
import de.mm20.launcher2.ui.launcher.search.common.grid.SearchResultGrid

data class DailySchedulePanelState(
    val enabled: Boolean = false,
    val calendarSelected: Boolean = false,
    val snapshot: DailyScheduleSnapshot = DailyScheduleSnapshot(),
)

data class HabitPanelState(
    val enabled: Boolean = false,
    val habits: List<HabitStatus> = emptyList(),
    val gate: HabitGateState = HabitGateState(blocked = false, overdueCount = 0),
)

data class FocusInsightsPanelState(
    val streakDays: Int = 0,
    val show: Boolean = true,
)

@Composable
internal fun FocusInsightsCard(
    state: FocusInsightsPanelState,
    onOpenInsights: () -> Unit,
) {
    if (!state.show) return

    FocusSection(
        title = stringResource(R.string.focus_insights_title),
        supportingText = if (state.streakDays > 0) {
            stringResource(R.string.focus_insights_streak) + ": ${state.streakDays}"
        } else null,
        emphasis = false,
    ) {
        OutlinedButton(
            onClick = onOpenInsights,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.focus_insights_title))
        }
    }
}

@Composable
internal fun FocusGuidanceCard(
    state: FocusGuidanceState,
    hasBlockPlan: Boolean = false,
    onRecoverAccepted: () -> Unit = {},
    onRecoverDismissed: () -> Unit = {},
    onOpenBlockSetup: () -> Unit = {},
) {
    if (state.type == FocusGuidanceType.None) {
        return
    }

    val title = when (state.type) {
        FocusGuidanceType.Recover -> stringResource(R.string.focus_home_guidance_recover_title)
        FocusGuidanceType.Prep -> stringResource(R.string.focus_home_guidance_prep_title)
        FocusGuidanceType.Ready -> stringResource(R.string.focus_home_guidance_ready_title)
        FocusGuidanceType.Now -> stringResource(R.string.focus_home_guidance_now_title)
        FocusGuidanceType.None -> return
    }

    val supportingText = when (state.type) {
        FocusGuidanceType.Recover -> when {
            !state.taskLabel.isNullOrBlank() && !state.blockLabel.isNullOrBlank() -> stringResource(
                R.string.focus_home_guidance_recover_body_block,
                state.taskLabel,
                state.blockLabel,
            )
            !state.taskLabel.isNullOrBlank() -> stringResource(
                R.string.focus_home_guidance_recover_body_task,
                state.taskLabel,
            )
            else -> null
        }
        FocusGuidanceType.Prep -> state.nextBlockLabel?.let {
            stringResource(
                R.string.focus_home_guidance_prep_body,
                it,
                state.minutesRemaining ?: 0,
            )
        }
        FocusGuidanceType.Ready -> state.blockLabel?.let {
            stringResource(R.string.focus_home_guidance_ready_body, it)
        }
        FocusGuidanceType.Now -> state.blockLabel?.let {
            if (state.completedForBlock) {
                stringResource(R.string.focus_home_guidance_complete_body, it)
            } else {
                stringResource(
                    R.string.focus_home_guidance_now_body,
                    it,
                    state.minutesRemaining ?: 0,
                )
            }
        }
        FocusGuidanceType.None -> null
    }

    FocusSection(
        title = title,
        supportingText = supportingText,
        emphasis = true,
    ) {
        FocusGuidanceDetails(state = state)
        when (state.type) {
            FocusGuidanceType.Recover -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onRecoverAccepted,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(
                                if (state.resumeMatchesCurrentBlock) {
                                    R.string.focus_home_resume_current_block_action
                                } else {
                                    R.string.focus_home_resume_action
                                }
                            )
                        )
                    }
                    TextButton(
                        onClick = onRecoverDismissed,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.focus_home_resume_dismiss))
                    }
                }
            }
            FocusGuidanceType.Prep -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onOpenBlockSetup,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(
                                if (hasBlockPlan) {
                                    R.string.focus_home_guidance_edit_action
                                } else {
                                    R.string.focus_home_guidance_prep_action
                                }
                            )
                        )
                    }
                    TextButton(
                        onClick = onOpenBlockSetup,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(
                                if (hasBlockPlan) {
                                    R.string.focus_home_daily_schedule_edit
                                } else {
                                    R.string.focus_home_daily_schedule_setup
                                }
                            )
                        )
                    }
                }
            }
            FocusGuidanceType.Ready -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onOpenBlockSetup,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(
                                if (hasBlockPlan) {
                                    R.string.focus_home_guidance_edit_action
                                } else {
                                    R.string.focus_home_guidance_ready_action
                                }
                            )
                        )
                    }
                    TextButton(
                        onClick = onOpenBlockSetup,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(
                                if (hasBlockPlan) {
                                    R.string.focus_home_daily_schedule_edit
                                } else {
                                    R.string.focus_home_daily_schedule_setup
                                }
                            )
                        )
                    }
                }
            }
            FocusGuidanceType.Now,
            -> {
                when {
                    state.requiresSetup -> {
                        OutlinedButton(
                            onClick = onOpenBlockSetup,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.focus_home_guidance_setup_action))
                        }
                    }

                    state.completedForBlock -> {
                        OutlinedButton(
                            onClick = onOpenBlockSetup,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.focus_home_guidance_continue_action))
                        }
                    }

                    hasBlockPlan -> {
                        TextButton(
                            onClick = onOpenBlockSetup,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.focus_home_guidance_edit_action))
                        }
                    }
                }
            }
            FocusGuidanceType.None -> Unit
        }
    }
}

@Composable
internal fun FocusSection(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    supportingText: String? = null,
    emphasis: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val backgroundColor = if (emphasis) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = backgroundColor,
        tonalElevation = if (emphasis) 4.dp else 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            eyebrow?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                supportingText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            content()
        }
    }
}

@Composable
internal fun FocusDailyScheduleCard(
    state: DailySchedulePanelState,
    hasBlockPlan: Boolean = false,
    onOpenBlockSetup: () -> Unit = {},
    onOpenConfiguration: () -> Unit = {},
) {
    FocusSection(
        title = stringResource(R.string.focus_home_daily_schedule_title),
    ) {
        when {
            !state.enabled -> {
                EmptyStateWithAction(
                    text = stringResource(R.string.focus_home_daily_schedule_disabled),
                    actionLabel = stringResource(R.string.focus_home_quick_start_schedule_action),
                    onAction = onOpenConfiguration,
                )
            }

            !state.calendarSelected -> {
                EmptyStateWithAction(
                    text = stringResource(R.string.focus_home_daily_schedule_no_calendar),
                    actionLabel = stringResource(R.string.focus_home_quick_start_schedule_action),
                    onAction = onOpenConfiguration,
                )
            }

            state.snapshot.currentBlock != null -> {
                ScheduleStateBlock(
                    label = stringResource(R.string.focus_home_daily_schedule_now),
                    block = state.snapshot.currentBlock,
                    countdownText = stringResource(
                        R.string.focus_home_daily_schedule_ends_in,
                        state.snapshot.minutesUntilCurrentBlockEnds,
                    ),
                    nextBlock = state.snapshot.nextBlock,
                    hasBlockPlan = hasBlockPlan,
                    onOpenBlockSetup = onOpenBlockSetup,
                )
            }

            state.snapshot.upcomingBlock != null -> {
                ScheduleStateBlock(
                    label = stringResource(R.string.focus_home_daily_schedule_upcoming),
                    block = state.snapshot.upcomingBlock,
                    countdownText = stringResource(
                        R.string.focus_home_daily_schedule_starts_in,
                        state.snapshot.minutesUntilUpcomingBlockStarts,
                    ),
                    hasBlockPlan = hasBlockPlan,
                    onOpenBlockSetup = onOpenBlockSetup,
                )
            }

            else -> {
                Text(
                    text = stringResource(R.string.focus_home_daily_schedule_between_blocks),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun FocusQuickStartDayCard(
    show: Boolean,
    onOpenFocusApps: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenHabits: () -> Unit,
) {
    if (!show) return

    FocusSection(
        title = stringResource(R.string.focus_home_quick_start_title),
        supportingText = stringResource(R.string.focus_home_quick_start_summary),
        emphasis = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onOpenFocusApps,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.focus_home_quick_start_apps_action))
            }
            OutlinedButton(
                onClick = onOpenSchedule,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.focus_home_quick_start_schedule_action))
            }
            TextButton(
                onClick = onOpenHabits,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.focus_home_quick_start_habits_action))
            }
        }
    }
}

@Composable
private fun ScheduleStateBlock(
    label: String,
    block: DailyScheduleBlock,
    countdownText: String,
    nextBlock: DailyScheduleBlock? = null,
    hasBlockPlan: Boolean = false,
    onOpenBlockSetup: () -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = block.label,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = countdownText,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        TextButton(
            onClick = onOpenBlockSetup,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    if (hasBlockPlan) {
                        R.string.focus_home_daily_schedule_edit
                    } else {
                        R.string.focus_home_daily_schedule_setup
                    }
                )
            )
        }
        nextBlock?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.focus_home_daily_schedule_next),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = it.label,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
internal fun FocusTransitionWarningCard(
    state: TransitionWarningState,
) {
    if (!state.show || state.nextBlockLabel == null) {
        return
    }

    FocusSection(
        title = stringResource(R.string.focus_home_transition_title),
        supportingText = stringResource(
            R.string.focus_home_transition_body,
            state.nextBlockLabel,
        ),
    ) {}
}

@Composable
private fun FocusGuidanceDetails(
    state: FocusGuidanceState,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val showBlockLabel = state.type == FocusGuidanceType.Recover
        state.taskLabel?.takeIf { state.type == FocusGuidanceType.Recover && it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.blockLabel?.takeIf { showBlockLabel && it.isNotBlank() }?.let {
            Text(
                text = stringResource(R.string.focus_home_guidance_recover_block, it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.intention?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.suggestedMicroStep?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = stringResource(R.string.focus_home_guidance_micro_step, it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun FocusHabitCard(
    state: HabitPanelState,
    onHabitCheckedChanged: (habitId: String, completed: Boolean) -> Unit,
) {
    FocusSection(
        title = stringResource(R.string.focus_home_daily_habits_title),
    ) {
        when {
            !state.enabled -> {
                Text(
                    text = stringResource(R.string.focus_home_daily_habits_disabled),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.habits.isEmpty() -> {
                Text(
                    text = stringResource(R.string.focus_home_daily_habits_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.gate.blocked) {
                        Text(
                            text = state.gate.primaryOverdueHabitTitle?.let {
                                stringResource(R.string.focus_home_habits_locked_named, it)
                            } ?: stringResource(R.string.focus_home_habits_locked),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    state.habits.forEach { habitStatus ->
                        HabitStatusRow(
                            habitStatus = habitStatus,
                            onHabitCheckedChanged = onHabitCheckedChanged,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitStatusRow(
    habitStatus: HabitStatus,
    onHabitCheckedChanged: (habitId: String, completed: Boolean) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Checkbox(
                checked = habitStatus.completed,
                onCheckedChange = { onHabitCheckedChanged(habitStatus.habit.id, it) },
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = habitStatus.habit.title,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(
                        R.string.focus_home_daily_habit_due_by,
                        formatHabitTime(habitStatus.habit.deadlineMinutes),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (habitStatus.overdue) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (habitStatus.completed) {
                    Text(
                        text = stringResource(R.string.focus_home_daily_habit_done),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else if (habitStatus.overdue) {
                    Text(
                        text = stringResource(R.string.focus_home_daily_habit_overdue),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
internal fun FocusScheduleDockCard(
    currentBlock: DailyScheduleBlock?,
    dockApps: List<Application>,
) {
    if (currentBlock == null) {
        return
    }

    FocusSection(
        title = stringResource(R.string.focus_home_daily_schedule_dock_title),
        supportingText = currentBlock.label,
    ) {
        if (dockApps.isEmpty()) {
            Text(
                text = stringResource(R.string.focus_home_daily_schedule_dock_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            SearchResultGrid(
                items = dockApps,
                showLabels = false,
                columns = dockApps.size.coerceAtMost(4).coerceAtLeast(1),
            )
        }
    }
}

@Composable
internal fun FocusEssentialAppsCard(
    apps: List<Application>,
) {
    if (apps.isEmpty()) return

    FocusSection(
        title = stringResource(R.string.focus_home_essentials_title),
        supportingText = stringResource(R.string.focus_home_essentials_summary),
    ) {
        SearchResultGrid(
            items = apps,
            showLabels = false,
            columns = apps.size.coerceAtMost(4).coerceAtLeast(1),
        )
    }
}

@Composable
private fun EmptyStateWithAction(
    text: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(
            onClick = onAction,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(actionLabel)
        }
    }
}

private fun formatHabitTime(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "%02d:%02d".format(hours, minutes)
}
