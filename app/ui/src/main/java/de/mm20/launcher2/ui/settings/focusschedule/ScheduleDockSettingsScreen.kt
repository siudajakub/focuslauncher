package de.mm20.launcher2.ui.settings.focusschedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import de.mm20.launcher2.search.Application
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.DismissableBottomSheet
import de.mm20.launcher2.ui.component.SmallMessage
import de.mm20.launcher2.ui.component.preferences.CheckboxPreference
import de.mm20.launcher2.ui.component.preferences.Preference
import de.mm20.launcher2.ui.component.preferences.PreferenceCategory
import de.mm20.launcher2.ui.component.preferences.PreferenceScreen
import kotlinx.serialization.Serializable

@Serializable
data object ScheduleDockSettingsRoute : NavKey

@Composable
fun ScheduleDockSettingsScreen() {
    val viewModel: ScheduleDockSettingsScreenVM = viewModel()
    val mappings by viewModel.scheduleDockMappings.collectAsStateWithLifecycle(emptyList())
    val allApps by viewModel.allApps.collectAsStateWithLifecycle(emptyList())
    var editingEventName by remember { mutableStateOf<String?>(null) }

    PreferenceScreen(title = stringResource(R.string.focus_schedule_dock_title)) {
        item {
            PreferenceCategory(title = stringResource(R.string.focus_schedule_dock_title)) {
                SmallMessage(
                    modifier = Modifier.padding(bottom = 12.dp),
                    icon = R.drawable.apps_24px,
                    text = stringResource(R.string.focus_schedule_dock_summary),
                )
                Preference(
                    title = stringResource(R.string.focus_schedule_dock_add_mapping),
                    summary = stringResource(R.string.focus_schedule_dock_add_mapping_summary),
                    icon = R.drawable.apps_24px,
                    onClick = { editingEventName = "" },
                )
                if (mappings.isEmpty()) {
                    Text(
                        text = stringResource(R.string.focus_schedule_dock_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                } else {
                    mappings.forEach { mapping ->
                        val apps = mapping.appKeys.mapNotNull { key ->
                            allApps.firstOrNull { it.key == key }
                        }
                        ScheduleDockMappingRow(
                            eventName = mapping.eventName,
                            appSummary = apps.takeIf { it.isNotEmpty() }?.let {
                                formatScheduleDockAppsSummary(it.map { app -> app.labelOverride ?: app.label })
                            } ?: stringResource(R.string.focus_schedule_dock_picker_empty),
                            onEdit = { editingEventName = mapping.eventName },
                            onRemove = { viewModel.removeScheduleDockMapping(mapping.eventName) },
                        )
                    }
                }
            }
        }
    }

    if (editingEventName != null) {
        val existing = findScheduleDockMapping(editingEventName!!, mappings)
        ScheduleDockMappingEditorSheet(
            initialEventName = existing?.eventName ?: editingEventName!!,
            initialAppKeys = existing?.appKeys ?: emptyList(),
            apps = allApps,
            onDismissRequest = { editingEventName = null },
            onSave = { eventName, appKeys ->
                viewModel.upsertScheduleDockMapping(eventName, appKeys)
                editingEventName = null
            },
        )
    }
}

@Composable
private fun ScheduleDockMappingRow(
    eventName: String,
    appSummary: String,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    Preference(
        title = eventName,
        summary = appSummary,
        icon = R.drawable.apps_24px,
        onClick = onEdit,
        controls = {
            IconButton(onClick = onRemove) {
                Icon(
                    painter = painterResource(R.drawable.delete_24px),
                    contentDescription = stringResource(R.string.focus_schedule_dock_mapping_remove),
                )
            }
        },
    )
}

@Composable
private fun PickerSheetTitle(
    title: String,
    summary: String,
) {
    Column(
        modifier = Modifier.padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ScheduleDockMappingEditorSheet(
    initialEventName: String,
    initialAppKeys: List<String>,
    apps: List<Application>,
    onDismissRequest: () -> Unit,
    onSave: (eventName: String, appKeys: List<String>) -> Unit,
) {
    var eventName by remember(initialEventName) { mutableStateOf(initialEventName) }
    var selectedAppKeys by remember(initialAppKeys) { mutableStateOf(initialAppKeys.toSet()) }
    var pickerOpen by remember { mutableStateOf(false) }

    val selectedApps = remember(apps, selectedAppKeys) {
        apps.filter { it.key in selectedAppKeys }
    }

    DismissableBottomSheet(expanded = true, onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(16.dp)) {
            PickerSheetTitle(
                title = stringResource(R.string.focus_schedule_dock_picker_title),
                summary = stringResource(R.string.focus_schedule_dock_summary),
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = eventName,
                onValueChange = { eventName = it },
                singleLine = true,
                label = { Text(stringResource(R.string.focus_schedule_dock_mapping_event_name)) },
            )
            Preference(
                title = stringResource(R.string.focus_schedule_dock_mapping_apps),
                summary = selectedApps.takeIf { it.isNotEmpty() }?.let {
                    formatScheduleDockAppsSummary(it.map { app -> app.labelOverride ?: app.label })
                } ?: stringResource(R.string.focus_schedule_dock_picker_empty),
                icon = R.drawable.apps_24px,
                onClick = { pickerOpen = true },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onSave(eventName, selectedAppKeys.toList())
                    },
                    enabled = eventName.isNotBlank() && selectedAppKeys.isNotEmpty(),
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        }
    }

    if (pickerOpen) {
        AppPickerSheet(
            apps = apps,
            selectedKeys = selectedAppKeys,
            onDismissRequest = { pickerOpen = false },
            onSelectionChanged = { selectedAppKeys = it },
        )
    }
}

@Composable
private fun AppPickerSheet(
    apps: List<Application>,
    selectedKeys: Set<String>,
    onDismissRequest: () -> Unit,
    onSelectionChanged: (Set<String>) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filteredApps = remember(apps, query) {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) {
            apps
        } else {
            apps.filter {
                val label = (it.labelOverride ?: it.label).lowercase()
                val packageName = it.componentName.packageName.lowercase()
                normalized in label || normalized in packageName
            }
        }
    }

    DismissableBottomSheet(expanded = true, onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(16.dp)) {
            PickerSheetTitle(
                title = stringResource(R.string.focus_schedule_dock_picker_title),
                summary = stringResource(R.string.focus_schedule_dock_add_mapping_summary),
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.focus_schedule_dock_picker_search)) },
                singleLine = true,
            )
            LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
                items(filteredApps, key = { it.key }) { app ->
                    val label = app.labelOverride ?: app.label
                    CheckboxPreference(
                        title = label,
                        summary = app.componentName.packageName,
                        value = selectedKeys.contains(app.key),
                        onValueChanged = { checked ->
                            onSelectionChanged(
                                if (checked) selectedKeys + app.key else selectedKeys - app.key
                            )
                        },
                    )
                }
                if (filteredApps.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.focus_schedule_dock_picker_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                }
            }
        }
    }
}
