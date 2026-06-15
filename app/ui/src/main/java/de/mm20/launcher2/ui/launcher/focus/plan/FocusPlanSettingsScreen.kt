package de.mm20.launcher2.ui.launcher.focus.plan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.preferences.SliderPreference
import de.mm20.launcher2.ui.component.preferences.PreferenceCategory
import de.mm20.launcher2.ui.component.preferences.TextPreference
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusPlanSettingsScreen(
    onNavigateUp: () -> Unit,
) {
    val searchUiSettings = koinInject<SearchUiSettings>()

    val startHour by searchUiSettings.focusPlanTimelineStartHour.collectAsStateWithLifecycle(initialValue = 9)
    val endHour by searchUiSettings.focusPlanTimelineEndHour.collectAsStateWithLifecycle(initialValue = 22)
    val durations by searchUiSettings.focusPlanDurations.collectAsStateWithLifecycle(initialValue = listOf(15, 30, 45, 60, 90, 120, 180, 240))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24px),
                            contentDescription = stringResource(R.string.focus_plan_navigate_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            PreferenceCategory(title = "Timeline") {
                SliderPreference(
                    title = "Start hour",
                    value = startHour,
                    onValueChanged = { searchUiSettings.setFocusPlanTimelineStartHour(it) },
                    min = 0,
                    max = 23,
                    label = { Text(String.format("%02d:00", it)) }
                )

                SliderPreference(
                    title = "End hour",
                    value = endHour,
                    onValueChanged = { searchUiSettings.setFocusPlanTimelineEndHour(it) },
                    min = 1,
                    max = 24,
                    label = { Text(String.format("%02d:00", it)) }
                )
            }

            PreferenceCategory(title = "Event durations") {
                TextPreference(
                    title = "Default durations (minutes)",
                    value = durations.joinToString(", "),
                    onValueChanged = { textValue ->
                        val newDurations = textValue.split(",")
                            .mapNotNull { it.trim().toIntOrNull() }
                            .filter { it > 0 }
                            .distinct()
                            .sorted()
                        if (newDurations.isNotEmpty()) {
                            searchUiSettings.setFocusPlanDurations(newDurations)
                        }
                    }
                )
            }
        }
    }
}
