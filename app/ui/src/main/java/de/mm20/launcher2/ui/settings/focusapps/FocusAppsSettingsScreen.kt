package de.mm20.launcher2.ui.settings.focusapps

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import de.mm20.launcher2.search.Application
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.DismissableBottomSheet
import de.mm20.launcher2.ui.component.preferences.Preference
import de.mm20.launcher2.ui.component.preferences.PreferenceCategory
import de.mm20.launcher2.ui.component.preferences.PreferenceScreen
import kotlinx.serialization.Serializable

@Serializable
data object FocusAppsSettingsRoute : NavKey

private enum class FocusAppsPickerTarget {
    Essential,
    Distracting,
}

@Composable
fun FocusAppsSettingsScreen() {
    val viewModel: FocusAppsSettingsScreenVM = viewModel()
    val allApps by viewModel.allApps.collectAsStateWithLifecycle(emptyList())
    val essentialApps by viewModel.essentialApps.collectAsStateWithLifecycle(emptyList())
    val distractingApps by viewModel.distractingApps.collectAsStateWithLifecycle(emptyList())
    var pickerTarget by remember { mutableStateOf<FocusAppsPickerTarget?>(null) }

    PreferenceScreen(title = stringResource(R.string.focus_apps_title)) {
        item {
            PreferenceCategory(title = stringResource(R.string.focus_apps_essential_title)) {
                Preference(
                    title = stringResource(R.string.focus_apps_add_essential),
                    summary = stringResource(R.string.focus_apps_add_essential_summary),
                    onClick = { pickerTarget = FocusAppsPickerTarget.Essential },
                )
                if (essentialApps.isEmpty()) {
                    Text(
                        text = stringResource(R.string.focus_apps_essential_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    essentialApps.forEach { app ->
                        FocusAppRow(
                            app = app,
                            onRemove = { viewModel.removeEssential(app.key) },
                        )
                    }
                }
            }
        }
        item {
            PreferenceCategory(title = stringResource(R.string.focus_apps_distracting_title)) {
                Preference(
                    title = stringResource(R.string.focus_apps_add_distracting),
                    summary = stringResource(R.string.focus_apps_add_distracting_summary),
                    onClick = { pickerTarget = FocusAppsPickerTarget.Distracting },
                )
                if (distractingApps.isEmpty()) {
                    Text(
                        text = stringResource(R.string.focus_apps_distracting_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    distractingApps.forEach { app ->
                        FocusAppRow(
                            app = app,
                            onRemove = { viewModel.removeDistracting(app.key) },
                        )
                    }
                }
            }
        }
        item {
            PreferenceCategory {
                Text(
                    text = stringResource(R.string.focus_apps_normal_info),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (pickerTarget != null) {
        FocusAppsPickerSheet(
            title = if (pickerTarget == FocusAppsPickerTarget.Essential) {
                stringResource(R.string.focus_apps_picker_essential)
            } else {
                stringResource(R.string.focus_apps_picker_distracting)
            },
            apps = allApps,
            onDismissRequest = { pickerTarget = null },
            onSelect = { app ->
                if (pickerTarget == FocusAppsPickerTarget.Essential) {
                    viewModel.addEssential(app.key)
                } else {
                    viewModel.addDistracting(app.key)
                }
                pickerTarget = null
            },
        )
    }
}

@Composable
private fun FocusAppRow(
    app: Application,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.labelOverride ?: app.label,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = app.componentName.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(R.drawable.delete_24px),
                contentDescription = stringResource(R.string.focus_apps_remove),
            )
        }
    }
}

@Composable
private fun FocusAppsPickerSheet(
    title: String,
    apps: List<Application>,
    onDismissRequest: () -> Unit,
    onSelect: (Application) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filteredApps = remember(apps, query) {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isBlank()) {
            apps
        } else {
            apps.filter {
                val label = (it.labelOverride ?: it.label).lowercase()
                val packageName = it.componentName.packageName.lowercase()
                normalizedQuery in label || normalizedQuery in packageName
            }
        }
    }

    DismissableBottomSheet(expanded = true, onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.focus_apps_picker_search)) },
            )
            LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
                items(filteredApps, key = { it.key }) { app ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(app) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                    ) {
                        Text(
                            text = app.labelOverride ?: app.label,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = app.componentName.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
