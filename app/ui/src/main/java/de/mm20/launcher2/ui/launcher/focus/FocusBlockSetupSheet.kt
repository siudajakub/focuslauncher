package de.mm20.launcher2.ui.launcher.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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

private data class FocusBlockSetupDefaults(
    val intention: String,
    val tinyStep: String,
    val note: String,
    val doneForBlock: Boolean,
    val selectedRecommendedAppKeys: Set<String>,
    val requireHabits: Boolean,
    val requirePrep: Boolean,
)

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

    val normalizedBlockLabel = remember(block.label) { normalizeScheduleEventName(block.label) }
    val sameDayExistingPlan = existingPlan?.takeIf {
        it.date == date.toString() && it.normalizedBlockLabel == normalizedBlockLabel
    }
    val defaults = remember(date, block.label, sameDayExistingPlan, suggestedApps) {
        resolveFocusBlockSetupDefaults(
            existingPlan = sameDayExistingPlan,
            suggestedApps = suggestedApps,
        )
    }

    var intention by remember(date, block.label, sameDayExistingPlan?.lastUpdatedAtMillis) {
        mutableStateOf(defaults.intention)
    }
    var tinyStep by remember(date, block.label, sameDayExistingPlan?.lastUpdatedAtMillis) {
        mutableStateOf(defaults.tinyStep)
    }
    var note by remember(date, block.label, sameDayExistingPlan?.lastUpdatedAtMillis) {
        mutableStateOf(defaults.note)
    }
    var doneForBlock by remember(date, block.label, sameDayExistingPlan?.lastUpdatedAtMillis) {
        mutableStateOf(defaults.doneForBlock)
    }
    var requireHabits by remember(date, block.label, sameDayExistingPlan?.lastUpdatedAtMillis) {
        mutableStateOf(defaults.requireHabits)
    }
    var requirePrep by remember(date, block.label, sameDayExistingPlan?.lastUpdatedAtMillis) {
        mutableStateOf(defaults.requirePrep)
    }
    var selectedRecommendedAppKeys by remember(date, block.label, sameDayExistingPlan?.lastUpdatedAtMillis) {
        mutableStateOf(defaults.selectedRecommendedAppKeys)
    }

    val canSave = tinyStep.trim().isNotBlank() || doneForBlock

    LaunchedEffect(sameDayExistingPlan) {
        if (sameDayExistingPlan == null) return@LaunchedEffect
        intention = sameDayExistingPlan.intention
        tinyStep = sameDayExistingPlan.tinyStep
        note = sameDayExistingPlan.note.orEmpty()
        doneForBlock = sameDayExistingPlan.doneForBlock
        requireHabits = sameDayExistingPlan.readinessChecks.any { it.source == FocusReadinessSource.Habit }
        requirePrep = sameDayExistingPlan.readinessChecks.any { it.source == FocusReadinessSource.Prep }
        selectedRecommendedAppKeys = sameDayExistingPlan.recommendedAppKeys.toSet()
    }

    val modeLabel = stringResource(
        if (sameDayExistingPlan != null) {
            R.string.focus_block_setup_mode_edit
        } else {
            R.string.focus_block_setup_mode_create
        }
    )
    val modeDescription = stringResource(
        if (sameDayExistingPlan != null) {
            R.string.focus_block_setup_mode_edit_summary
        } else {
            R.string.focus_block_setup_mode_create_summary
        }
    )
    val habitsReadinessLabel = stringResource(R.string.focus_block_setup_require_habits)
    val prepReadinessLabel = stringResource(R.string.focus_block_setup_require_prep)
    val saveLabel = stringResource(
        if (sameDayExistingPlan != null) {
            R.string.focus_block_setup_update
        } else {
            R.string.focus_block_setup_save
        }
    )

    DismissableBottomSheet(expanded = expanded, onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = modeLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = modeDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.focus_block_setup_recommended_apps_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
                when {
                    sameDayExistingPlan?.recommendedAppKeys?.isNotEmpty() == true && suggestedApps.isEmpty() -> {
                        Text(
                            text = stringResource(R.string.focus_block_setup_recommended_apps_saved),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    suggestedApps.isEmpty() -> {
                        Text(
                            text = stringResource(R.string.focus_block_setup_recommended_apps_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(suggestedApps, key = { it.key }) { app ->
                                val selected = selectedRecommendedAppKeys.contains(app.key)
                                FocusBlockSetupCheckboxRow(
                                    checked = selected,
                                    label = app.labelOverride ?: app.label,
                                    supportingText = app.componentName.packageName,
                                    onCheckedChange = { checked ->
                                        selectedRecommendedAppKeys =
                                            if (checked) {
                                                selectedRecommendedAppKeys + app.key
                                            } else {
                                                selectedRecommendedAppKeys - app.key
                                            }
                                    },
                                )
                            }
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.focus_block_setup_readiness_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
                FocusBlockSetupCheckboxRow(
                    checked = requireHabits,
                    label = habitsReadinessLabel,
                    onCheckedChange = { requireHabits = it },
                )
                FocusBlockSetupCheckboxRow(
                    checked = requirePrep,
                    label = prepReadinessLabel,
                    onCheckedChange = { requirePrep = it },
                )
                FocusBlockSetupCheckboxRow(
                    checked = doneForBlock,
                    label = stringResource(R.string.focus_block_setup_done_label),
                    supportingText = stringResource(R.string.focus_block_setup_done_summary),
                    onCheckedChange = { doneForBlock = it },
                )
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
                onClick = {
                    val readinessChecks = buildList {
                        if (requireHabits) {
                            add(
                                FocusReadinessCheck(
                                    id = "habits",
                                    label = habitsReadinessLabel,
                                    source = FocusReadinessSource.Habit,
                                )
                            )
                        }
                        if (requirePrep) {
                            add(
                                FocusReadinessCheck(
                                    id = "prep",
                                    label = prepReadinessLabel,
                                    source = FocusReadinessSource.Prep,
                                )
                            )
                        }
                    }
                    onSave(
                        FocusBlockPlan(
                            date = date.toString(),
                            normalizedBlockLabel = normalizedBlockLabel,
                            blockLabel = block.label,
                            intention = intention.trim(),
                            tinyStep = tinyStep.trim(),
                            note = note.trim().takeIf { it.isNotBlank() },
                            recommendedAppKeys = resolveRecommendedAppKeys(
                                activePlan = sameDayExistingPlan,
                                selectedRecommendedAppKeys = selectedRecommendedAppKeys,
                                suggestedApps = suggestedApps,
                            ),
                            readinessChecks = readinessChecks,
                            doneForBlock = doneForBlock,
                            lastUpdatedAtMillis = System.currentTimeMillis(),
                        )
                    )
                    onDismissRequest()
                },
            ) {
                Text(saveLabel)
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
private fun FocusBlockSetupCheckboxRow(
    checked: Boolean,
    label: String,
    supportingText: String? = null,
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
        Column(modifier = Modifier.padding(top = 2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
            )
            supportingText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun resolveFocusBlockSetupDefaults(
    existingPlan: FocusBlockPlan?,
    suggestedApps: List<Application>,
): FocusBlockSetupDefaults {
    val selectedSuggestedAppKeys = when {
        existingPlan?.recommendedAppKeys?.isNotEmpty() == true -> existingPlan.recommendedAppKeys.toSet()
        suggestedApps.isNotEmpty() -> suggestedApps.map { it.key }.toSet()
        else -> emptySet()
    }
    return FocusBlockSetupDefaults(
        intention = existingPlan?.intention.orEmpty(),
        tinyStep = existingPlan?.tinyStep.orEmpty(),
        note = existingPlan?.note.orEmpty(),
        doneForBlock = existingPlan?.doneForBlock == true,
        selectedRecommendedAppKeys = selectedSuggestedAppKeys,
        requireHabits = existingPlan?.readinessChecks?.any { it.source == FocusReadinessSource.Habit } == true,
        requirePrep = existingPlan?.readinessChecks?.any { it.source == FocusReadinessSource.Prep } == true,
    )
}

private fun resolveRecommendedAppKeys(
    activePlan: FocusBlockPlan?,
    selectedRecommendedAppKeys: Set<String>,
    suggestedApps: List<Application>,
): List<String> {
    return when {
        suggestedApps.isNotEmpty() -> suggestedApps
            .filter { it.key in selectedRecommendedAppKeys }
            .map { it.key }
            .distinct()

        activePlan?.recommendedAppKeys?.isNotEmpty() == true -> activePlan.recommendedAppKeys

        else -> emptyList()
    }
}
