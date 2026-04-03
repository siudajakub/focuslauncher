package de.mm20.launcher2.ui.launcher.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.mm20.launcher2.search.Application
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.DismissableBottomSheet
import java.time.LocalDate

@Composable
fun FocusBlockSetupSheet(
    expanded: Boolean,
    block: DailyScheduleBlock,
    date: LocalDate,
    existingPlan: FocusBlockPlan?,
    suggestedApps: List<Application>,
    onDismissRequest: () -> Unit,
    onSave: (FocusBlockPlan) -> Unit,
) {
    if (!expanded) return

    var intention by remember(block.label, existingPlan) { mutableStateOf(existingPlan?.intention.orEmpty()) }
    var tinyStep by remember(block.label, existingPlan) { mutableStateOf(existingPlan?.tinyStep.orEmpty()) }
    var note by remember(block.label, existingPlan) { mutableStateOf(existingPlan?.note.orEmpty()) }
    var includeSuggestedApps by remember(block.label, existingPlan, suggestedApps) {
        mutableStateOf(
            existingPlan?.recommendedAppKeys?.isNotEmpty() == true ||
                (existingPlan == null && suggestedApps.isNotEmpty())
        )
    }
    var requireHabits by remember(block.label, existingPlan) {
        mutableStateOf(existingPlan?.readinessChecks?.any { it.source == FocusReadinessSource.Habit } == true)
    }
    var requirePrep by remember(block.label, existingPlan) {
        mutableStateOf(existingPlan?.readinessChecks?.any { it.source == FocusReadinessSource.Prep } == true)
    }

    LaunchedEffect(existingPlan, block.label) {
        if (existingPlan != null) {
            intention = existingPlan.intention
            tinyStep = existingPlan.tinyStep
            note = existingPlan.note.orEmpty()
        }
    }

    DismissableBottomSheet(expanded = expanded, onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.focus_block_setup_title, block.label),
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = intention,
                onValueChange = { intention = it },
                label = { Text(stringResource(R.string.focus_block_setup_intention_label)) },
                placeholder = { Text(stringResource(R.string.focus_block_setup_intention_placeholder)) },
                minLines = 1,
                maxLines = 2,
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = tinyStep,
                onValueChange = { tinyStep = it },
                label = { Text(stringResource(R.string.focus_block_setup_tiny_step_label)) },
                placeholder = { Text(stringResource(R.string.focus_block_setup_tiny_step_placeholder)) },
                minLines = 1,
                maxLines = 2,
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.focus_block_setup_note_label)) },
                minLines = 1,
                maxLines = 3,
            )
            FocusReadinessToggleRow(
                checked = requireHabits,
                label = stringResource(R.string.focus_block_setup_require_habits),
                onCheckedChange = { requireHabits = it },
            )
            FocusReadinessToggleRow(
                checked = requirePrep,
                label = stringResource(R.string.focus_block_setup_require_prep),
                onCheckedChange = { requirePrep = it },
            )
            if (suggestedApps.isNotEmpty()) {
                FocusReadinessToggleRow(
                    checked = includeSuggestedApps,
                    label = stringResource(R.string.focus_block_setup_use_assigned_apps),
                    onCheckedChange = { includeSuggestedApps = it },
                )
                if (includeSuggestedApps) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(suggestedApps, key = { it.key }) { app ->
                            Text(
                                text = app.labelOverride ?: app.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = tinyStep.isNotBlank(),
                onClick = {
                    val readinessChecks = buildList {
                        if (requireHabits) {
                            add(
                                FocusReadinessCheck(
                                    id = "habits",
                                    label = "Habits",
                                    source = FocusReadinessSource.Habit,
                                )
                            )
                        }
                        if (requirePrep) {
                            add(
                                FocusReadinessCheck(
                                    id = "prep",
                                    label = "Prep",
                                    source = FocusReadinessSource.Prep,
                                )
                            )
                        }
                    }
                    onSave(
                        FocusBlockPlan(
                            date = date.toString(),
                            normalizedBlockLabel = normalizeScheduleEventName(block.label),
                            blockLabel = block.label,
                            intention = intention.trim(),
                            tinyStep = tinyStep.trim(),
                            note = note.trim().takeIf { it.isNotBlank() },
                            recommendedAppKeys = if (includeSuggestedApps) {
                                suggestedApps.map { it.key }
                            } else {
                                emptyList()
                            },
                            readinessChecks = readinessChecks,
                            lastUpdatedAtMillis = System.currentTimeMillis(),
                        )
                    )
                    onDismissRequest()
                },
            ) {
                Text(stringResource(R.string.focus_block_setup_save))
            }
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onDismissRequest,
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    }
}

@Composable
private fun FocusReadinessToggleRow(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}
