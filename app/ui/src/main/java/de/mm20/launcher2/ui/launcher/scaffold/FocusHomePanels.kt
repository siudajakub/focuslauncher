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

@Composable
internal fun FocusGuidanceCard(
    state: FocusGuidanceState,
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
        FocusGuidanceType.Recover -> state.blockLabel
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
            stringResource(
                R.string.focus_home_guidance_now_body,
                it,
                state.minutesRemaining ?: 0,
            )
        }
        FocusGuidanceType.None -> null
    }

    FocusSection(
        title = title,
        supportingText = supportingText,
    ) {
        state.intention?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.suggestedMicroStep?.let {
            Text(
                text = stringResource(R.string.focus_home_guidance_micro_step, it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when (state.type) {
            FocusGuidanceType.Recover -> {
                OutlinedButton(
                    onClick = onRecoverAccepted,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.focus_home_resume_action))
                }
                TextButton(
                    onClick = onRecoverDismissed,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.focus_home_resume_dismiss))
                }
            }
            FocusGuidanceType.Prep -> {
                OutlinedButton(
                    onClick = onOpenBlockSetup,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.focus_home_guidance_prep_action))
                }
            }
            FocusGuidanceType.Ready -> {
                OutlinedButton(
                    onClick = onOpenBlockSetup,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.focus_home_guidance_ready_action))
                }
            }
            FocusGuidanceType.Now,
            FocusGuidanceType.None,
            -> Unit
        }
    }
}

@Composable
internal fun FocusSection(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    supportingText: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
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
    onOpenBlockSetup: () -> Unit = {},
) {
    FocusSection(
        title = stringResource(R.string.focus_home_daily_schedule_title),
    ) {
        when {
            !state.enabled -> {
                Text(
                    text = stringResource(R.string.focus_home_daily_schedule_disabled),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            !state.calendarSelected -> {
                Text(
                    text = stringResource(R.string.focus_home_daily_schedule_no_calendar),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    onOpenBlockSetup = onOpenBlockSetup,
                )
            }

            else -> {
                Text(
                    text = stringResource(R.string.focus_home_daily_schedule_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
            Text(stringResource(R.string.focus_home_daily_schedule_setup))
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

private fun formatHabitTime(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "%02d:%02d".format(hours, minutes)
}
