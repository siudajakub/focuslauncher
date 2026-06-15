package de.mm20.launcher2.ui.launcher.focus.plan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.launcher.focus.todoist.TodoistTask
import de.mm20.launcher2.search.CalendarEvent
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

sealed interface TimelineItem {
    data class Event(val event: CalendarEvent) : TimelineItem
    data class FreeSlot(val timeMillis: Long) : TimelineItem
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusPlanScreen(
    onNavigateUp: () -> Unit,
    onOpenTodoistSettings: () -> Unit,
    onOpenPlanSettings: () -> Unit,
) {
    val viewModel: FocusPlanVM = viewModel()
    val tasksState by viewModel.tasksState.collectAsStateWithLifecycle()
    val calendars by viewModel.calendars.collectAsStateWithLifecycle()
    val selectedCalendarId by viewModel.selectedCalendarId.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val startHour by viewModel.startHour.collectAsStateWithLifecycle()
    val endHour by viewModel.endHour.collectAsStateWithLifecycle()
    val defaultDurations by viewModel.durations.collectAsStateWithLifecycle()
    val tasks = (tasksState as? FocusPlanTasksState.Ready)?.tasks.orEmpty()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshTasks()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showTaskPickerForTimeMillis by remember { mutableStateOf<Long?>(null) }
    var durationMinutes by remember { mutableStateOf("60") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.focus_plan_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24px),
                            contentDescription = stringResource(R.string.focus_plan_navigate_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenPlanSettings) {
                        Icon(
                            painter = painterResource(R.drawable.settings_24px),
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                }
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (calendars.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                val selectedCalendar = calendars.find {
                    it.id.removePrefix("local:") == selectedCalendarId.toString()
                }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = selectedCalendar?.name.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text(stringResource(R.string.focus_plan_choose_calendar)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        calendars.forEach { calendar ->
                            DropdownMenuItem(
                                text = { Text(calendar.name) },
                                onClick = {
                                    calendar.id.removePrefix("local:").toLongOrNull()?.let(viewModel::selectCalendar)
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.focus_plan_to_plan),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            AnimatedVisibility(
                visible = tasksState !is FocusPlanTasksState.Ready,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                TasksStatusCard(
                    state = tasksState,
                    onRetry = viewModel::refreshTasks,
                    onOpenSettings = onOpenTodoistSettings,
                )
            }

            if (tasks.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(tasks, key = TodoistTask::id) { task ->
                        Card(modifier = Modifier.animateContentSize()) {
                            Box(modifier = Modifier.padding(16.dp)) {
                                Text(task.content)
                            }
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.focus_plan_timeline),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val startOfDay = LocalDate.now(ZoneId.systemDefault()).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val sortedEvents = events.sortedBy { it.startTime ?: 0L }.filter { it.startTime != null && it.endTime != null }

                var currentTimeMillis = startOfDay + startHour * 3600_000L
                val endTimeMillis = startOfDay + endHour * 3600_000L
                val currentSlots = mutableListOf<TimelineItem>()
                var eventIndex = 0

                while (currentTimeMillis < endTimeMillis) {
                    val nextEvent = sortedEvents.getOrNull(eventIndex)
                    if (nextEvent != null && nextEvent.startTime!! <= currentTimeMillis) {
                        currentSlots.add(TimelineItem.Event(nextEvent))
                        currentTimeMillis = maxOf(currentTimeMillis, nextEvent.endTime!!)
                        eventIndex++
                    } else if (nextEvent != null && nextEvent.startTime!! < currentTimeMillis + 30 * 60_000L) {
                        currentSlots.add(TimelineItem.FreeSlot(currentTimeMillis))
                        currentTimeMillis = nextEvent.startTime!!
                    } else {
                        currentSlots.add(TimelineItem.FreeSlot(currentTimeMillis))
                        val calendar = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
                        val minutes = calendar.get(Calendar.MINUTE)
                        val advanceMinutes = if (minutes % 30 == 0) 30 else (30 - (minutes % 30))
                        currentTimeMillis += advanceMinutes * 60_000L
                    }
                }

                items(currentSlots) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .clickable(enabled = tasks.isNotEmpty() && item is TimelineItem.FreeSlot) {
                                if (item is TimelineItem.FreeSlot) {
                                    showTaskPickerForTimeMillis = item.timeMillis
                                }
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val timeMillis = when (item) {
                                is TimelineItem.Event -> item.event.startTime!!
                                is TimelineItem.FreeSlot -> item.timeMillis
                            }
                            val calendar = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
                            Text(
                                String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE)),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(Modifier.width(16.dp))
                            when (item) {
                                is TimelineItem.Event -> {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.event.label, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                is TimelineItem.FreeSlot -> {
                                    Text(
                                        text = if (tasks.isEmpty()) {
                                            stringResource(R.string.focus_plan_tasks_required)
                                        } else {
                                            stringResource(R.string.focus_plan_click_to_plan)
                                        },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    showTaskPickerForTimeMillis?.let { timeMillis ->
        val calendar = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
        val timeLabel = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
        AlertDialog(
            onDismissRequest = { showTaskPickerForTimeMillis = null },
            title = { Text(stringResource(R.string.focus_plan_schedule_at, timeLabel)) },
            text = {
                Column {
                    Text(stringResource(R.string.focus_plan_duration))
                    // Use a layout that handles multiple rows dynamically.
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()
                    ) {
                        defaultDurations.forEach { preset ->
                            val isSelected = durationMinutes == preset.toString()
                            androidx.compose.material3.FilterChip(
                                selected = isSelected,
                                onClick = { durationMinutes = preset.toString() },
                                label = { Text("$preset") }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = durationMinutes,
                        onValueChange = { durationMinutes = it.filter(Char::isDigit) },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.focus_plan_select_task))
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(tasks, key = TodoistTask::id) { task ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.scheduleTask(
                                            task = task,
                                            startTime = timeMillis,
                                            durationMinutes = durationMinutes.toIntOrNull()?.coerceAtLeast(1) ?: 60,
                                        )
                                        showTaskPickerForTimeMillis = null
                                    }
                            ) {
                                Text(
                                    text = task.content,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTaskPickerForTimeMillis = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun TasksStatusCard(
    state: FocusPlanTasksState,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (state) {
                FocusPlanTasksState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.width(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.focus_plan_tasks_loading))
                    }
                }
                FocusPlanTasksState.TokenMissing -> {
                    Text(stringResource(R.string.focus_plan_tasks_token_missing_title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.focus_plan_tasks_token_missing_body))
                    OutlinedButton(onClick = onOpenSettings) {
                        Text(stringResource(R.string.focus_plan_open_todoist_settings))
                    }
                }
                FocusPlanTasksState.InvalidToken -> {
                    Text(stringResource(R.string.focus_plan_tasks_invalid_token_title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.focus_plan_tasks_invalid_token_body))
                    OutlinedButton(onClick = onOpenSettings) {
                        Text(stringResource(R.string.focus_plan_open_todoist_settings))
                    }
                }
                FocusPlanTasksState.ConnectionError -> {
                    Text(stringResource(R.string.focus_plan_tasks_connection_error_title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.focus_plan_tasks_connection_error_body))
                    OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.focus_plan_retry)) }
                }
                FocusPlanTasksState.ServiceError -> {
                    Text(stringResource(R.string.focus_plan_tasks_service_error_title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.focus_plan_tasks_service_error_body))
                    OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.focus_plan_retry)) }
                }
                FocusPlanTasksState.Empty -> {
                    Text(stringResource(R.string.focus_plan_tasks_empty_title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.focus_plan_tasks_empty_body))
                    OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.focus_plan_retry)) }
                }
                is FocusPlanTasksState.Ready -> Unit
            }
        }
    }
}
