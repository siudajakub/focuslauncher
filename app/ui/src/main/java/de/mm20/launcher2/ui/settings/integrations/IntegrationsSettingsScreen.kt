package de.mm20.launcher2.ui.settings.integrations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.preferences.Preference
import de.mm20.launcher2.ui.component.preferences.PreferenceCategory
import de.mm20.launcher2.ui.component.preferences.PreferenceScreen
import de.mm20.launcher2.ui.component.preferences.TextPreference
import de.mm20.launcher2.ui.locals.LocalBackStack
import de.mm20.launcher2.ui.settings.tasks.TasksIntegrationSettingsRoute
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
data object IntegrationsSettingsRoute : NavKey

@Composable
fun IntegrationsSettingsScreen() {
    val backStack = LocalBackStack.current
    val searchUiSettings = koinInject<SearchUiSettings>()
    val todoistToken by searchUiSettings.focusTodoistApiToken.collectAsStateWithLifecycle(initialValue = "")

    PreferenceScreen(title = stringResource(R.string.preference_screen_integrations)) {
        item {
            PreferenceCategory {
                Preference(
                    title = stringResource(R.string.preference_tasks_integration),
                    icon = R.drawable.check_24px_sharp,
                    onClick = {
                        backStack.add(TasksIntegrationSettingsRoute)
                    }
                )
                TextPreference(
                    title = stringResource(R.string.focus_system_todoist_title),
                    value = todoistToken.orEmpty(),
                    summary = stringResource(
                        if (todoistToken.isNullOrBlank()) {
                            R.string.focus_system_todoist_summary_unlinked
                        } else {
                            R.string.focus_system_todoist_summary_linked
                        }
                    ),
                    placeholder = stringResource(R.string.focus_system_todoist_token_placeholder),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    onValueChanged = searchUiSettings::setFocusTodoistApiToken,
                )
            }
        }
    }
}
