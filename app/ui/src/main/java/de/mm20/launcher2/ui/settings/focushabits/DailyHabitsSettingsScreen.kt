package de.mm20.launcher2.ui.settings.focushabits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import de.mm20.launcher2.preferences.FocusHabit
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.SmallMessage
import de.mm20.launcher2.ui.component.preferences.Preference
import de.mm20.launcher2.ui.component.preferences.PreferenceCategory
import de.mm20.launcher2.ui.component.preferences.PreferenceScreen
import de.mm20.launcher2.ui.component.preferences.SwitchPreference
import kotlinx.serialization.Serializable

@Serializable
data object DailyHabitsSettingsRoute : NavKey

private fun formatHabitTime(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "%02d:%02d".format(hours, minutes)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitEditorDialog(
    initialHabit: FocusHabit?,
    onDismiss: () -> Unit,
    onSave: (title: String, deadlineMinutes: Int) -> Unit,
) {
    var title by remember(initialHabit?.id) { mutableStateOf(initialHabit?.title.orEmpty()) }
    val pickerState = rememberTimePickerState(
        initialHour = (initialHabit?.deadlineMinutes ?: 9 * 60) / 60,
        initialMinute = (initialHabit?.deadlineMinutes ?: 9 * 60) % 60,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (initialHabit == null) R.string.focus_settings_daily_habit_dialog_title_add
                    else R.string.focus_settings_daily_habit_dialog_title_edit
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.focus_settings_daily_habit_title_hint)) },
                    singleLine = true,
                )
                TimeInput(state = pickerState)
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    onSave(title, pickerState.hour * 60 + pickerState.minute)
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
fun DailyHabitsSettingsScreen() {
    val viewModel: DailyHabitsSettingsScreenVM = viewModel()
    val habitsEnabled by viewModel.habitsEnabled.collectAsStateWithLifecycle(null)
    val habits by viewModel.habits.collectAsStateWithLifecycle(emptyList())
    var editingHabit by remember { mutableStateOf<FocusHabit?>(null) }
    var addingHabit by remember { mutableStateOf(false) }

    PreferenceScreen(title = stringResource(R.string.focus_settings_daily_habits_title)) {
        item {
            PreferenceCategory(title = stringResource(R.string.focus_settings_daily_habits_title)) {
                SmallMessage(
                    modifier = Modifier.padding(bottom = 12.dp),
                    icon = R.drawable.check_24px,
                    text = stringResource(R.string.focus_settings_daily_habits_summary),
                )
                SwitchPreference(
                    title = stringResource(R.string.focus_settings_daily_habits_enabled),
                    summary = stringResource(R.string.focus_settings_daily_habits_enabled_summary),
                    icon = R.drawable.check_24px,
                    value = habitsEnabled == true,
                    onValueChanged = viewModel::setHabitsEnabled,
                )
                Preference(
                    title = stringResource(R.string.focus_settings_daily_habit_add),
                    summary = stringResource(R.string.focus_settings_daily_habit_add_summary),
                    icon = R.drawable.add_24px,
                    onClick = { addingHabit = true },
                )
                if (habits.isEmpty()) {
                    Text(
                        text = stringResource(R.string.focus_settings_daily_habit_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    habits.forEach { habit ->
                        HabitRow(
                            habit = habit,
                            onEdit = { editingHabit = habit },
                            onRemove = { viewModel.removeHabit(habit.id) },
                        )
                    }
                }
            }
        }
    }

    if (addingHabit) {
        HabitEditorDialog(
            initialHabit = null,
            onDismiss = { addingHabit = false },
            onSave = { title, deadlineMinutes ->
                viewModel.saveHabit(null, title, deadlineMinutes)
                addingHabit = false
            },
        )
    }

    editingHabit?.let { habit ->
        HabitEditorDialog(
            initialHabit = habit,
            onDismiss = { editingHabit = null },
            onSave = { title, deadlineMinutes ->
                viewModel.saveHabit(
                    id = habit.id,
                    title = title,
                    deadlineMinutes = deadlineMinutes,
                    completedDates = habit.completedDates,
                )
                editingHabit = null
            },
        )
    }
}

@Composable
private fun HabitRow(
    habit: FocusHabit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Preference(
            title = habit.title,
            summary = stringResource(
                R.string.focus_settings_daily_habit_time,
                formatHabitTime(habit.deadlineMinutes),
            ),
            icon = R.drawable.check_24px,
            onClick = onEdit,
            controls = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            painter = painterResource(R.drawable.edit_24px),
                            contentDescription = stringResource(R.string.edit),
                        )
                    }
                    IconButton(onClick = onRemove) {
                        Icon(
                            painter = painterResource(R.drawable.delete_24px),
                            contentDescription = stringResource(R.string.focus_settings_daily_habit_remove),
                        )
                    }
                }
            },
        )
    }
}
