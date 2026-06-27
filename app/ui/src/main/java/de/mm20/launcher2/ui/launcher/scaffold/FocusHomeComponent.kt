package de.mm20.launcher2.ui.launcher.scaffold

import android.content.Context
import android.app.AlarmManager
import android.content.Intent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.mm20.launcher2.ui.launcher.focus.FocusLaunchCoordinator
import androidx.lifecycle.viewModelScope
import androidx.core.content.getSystemService
import de.mm20.launcher2.applications.AppRepository
import de.mm20.launcher2.calendar.CalendarRepository
import de.mm20.launcher2.data.customattrs.CustomAttributesRepository
import de.mm20.launcher2.data.customattrs.utils.withCustomLabels
import de.mm20.launcher2.permissions.PermissionGroup
import de.mm20.launcher2.permissions.PermissionsManager
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import de.mm20.launcher2.searchable.PinnedLevel
import de.mm20.launcher2.services.favorites.FavoritesService
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.launcher.focus.FocusHistoryRepository
import de.mm20.launcher2.ui.launcher.focus.FocusBlockPlan
import de.mm20.launcher2.ui.launcher.focus.FocusBlockReadiness
import de.mm20.launcher2.ui.launcher.focus.FocusBlockSetupSheet
import de.mm20.launcher2.ui.launcher.focus.FocusEventKind
import de.mm20.launcher2.ui.launcher.focus.FocusLogEvent
import de.mm20.launcher2.ui.launcher.focus.FocusGuidanceState
import de.mm20.launcher2.ui.launcher.focus.FocusGuidanceType
import de.mm20.launcher2.ui.launcher.focus.FocusHomeCommandCenter
import de.mm20.launcher2.ui.launcher.focus.FocusHomeCommandType
import de.mm20.launcher2.ui.launcher.focus.FocusPolicyService
import de.mm20.launcher2.ui.launcher.focus.FocusSessionRepository
import de.mm20.launcher2.ui.launcher.focus.PrepCardState
import de.mm20.launcher2.ui.launcher.focus.ResumeCardState
import de.mm20.launcher2.ui.launcher.focus.TransitionWarningState
import de.mm20.launcher2.ui.launcher.focus.findBlockPlan
import de.mm20.launcher2.ui.launcher.focus.resolveActiveDockApps
import de.mm20.launcher2.ui.launcher.focus.resolveBlockReadiness
import de.mm20.launcher2.ui.launcher.focus.resolveDailyScheduleSnapshot
import de.mm20.launcher2.ui.launcher.focus.resolvePreferredDockApps
import de.mm20.launcher2.ui.launcher.focus.resolveFocusGuidance
import de.mm20.launcher2.ui.launcher.focus.FocusInsightsRoute
import de.mm20.launcher2.ui.launcher.focus.HabitGateState
import de.mm20.launcher2.ui.launcher.focus.HabitStatus
import de.mm20.launcher2.ui.launcher.focus.DailyScheduleSnapshot
import de.mm20.launcher2.ui.launcher.focus.resolveHabitGate
import de.mm20.launcher2.ui.launcher.focus.resolveHabitStatuses
import de.mm20.launcher2.ui.launcher.focus.resolvePrepCard
import de.mm20.launcher2.ui.launcher.focus.resolveScheduleAwareResumeCard
import de.mm20.launcher2.ui.launcher.focus.resolveTransitionWarning
import de.mm20.launcher2.ui.launcher.focus.normalizeScheduleEventName
import de.mm20.launcher2.ui.launcher.focus.shouldShowFocusQuickStart
import de.mm20.launcher2.ui.launcher.focus.toDailyScheduleBlock
import de.mm20.launcher2.ui.launcher.focus.toLocalDate
import de.mm20.launcher2.ui.launcher.search.SearchVM
import de.mm20.launcher2.ui.launcher.search.common.list.SearchResultList
import de.mm20.launcher2.ui.launcher.widgets.clock.ClockWidget
import de.mm20.launcher2.ui.settings.SettingsActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.compose.koinInject
import java.time.LocalDate
import java.time.ZoneId

private val FocusSessionDurationOptions = listOf(5, 15, 30, 45, 60, 75, 90)

private fun formatSessionDuration(minutes: Int): String {
    val hours = minutes / 60
    val remainder = minutes % 60
    return when {
        hours == 0 -> "${minutes} min"
        remainder == 0 -> "${hours} h"
        else -> "${hours} h ${remainder} min"
    }
}

internal object FocusHomeComponent : ScaffoldComponent() {
    override val drawBackground: Boolean = false
    override val isAtTop: State<Boolean?> = mutableStateOf(true)
    override val isAtBottom: State<Boolean?> = mutableStateOf(true)

    @Composable
    override fun Component(
        modifier: Modifier,
        insets: PaddingValues,
        state: LauncherScaffoldState,
    ) {
        val context = LocalContext.current
        val viewModel: FocusHomeVM = viewModel()
        val nextEvents by viewModel.nextEvents.collectAsStateWithLifecycle(initialValue = emptyList())
        val focusSessionEndsAt by viewModel.focusSessionEndsAt.collectAsStateWithLifecycle(initialValue = 0L)
        val minutesRemaining by viewModel.minutesRemaining.collectAsStateWithLifecycle(initialValue = 0)
        val defaultSessionMinutes by viewModel.defaultSessionMinutes.collectAsStateWithLifecycle(initialValue = 15)
        val nextAlarm by viewModel.nextAlarm.collectAsStateWithLifecycle(initialValue = null)
        val sessionSummary by viewModel.sessionSummary.collectAsStateWithLifecycle(initialValue = FocusSessionUiSummary())
        val dailyScheduleState by viewModel.dailyScheduleState.collectAsStateWithLifecycle(
            initialValue = DailySchedulePanelState(),
        )
        val transitionWarningState by viewModel.transitionWarningState.collectAsStateWithLifecycle(
            initialValue = TransitionWarningState(show = false),
        )
        val showResetButton by viewModel.showResetButton.collectAsStateWithLifecycle(initialValue = false)
        val essentialApps by viewModel.essentialApps.collectAsStateWithLifecycle(initialValue = emptyList())
        val webApps by viewModel.webApps.collectAsStateWithLifecycle(initialValue = emptyList())
        val shouldShowQuickStart by viewModel.shouldShowQuickStart.collectAsStateWithLifecycle(initialValue = false)
        val habitState by viewModel.habitState.collectAsStateWithLifecycle(
            initialValue = HabitPanelState(),
        )
        val insightsState by viewModel.insightsState.collectAsStateWithLifecycle(
            initialValue = FocusInsightsPanelState(),
        )
        val activeDockApps by viewModel.activeDockApps.collectAsStateWithLifecycle(initialValue = emptyList())
        val blockPlanTargetApps by viewModel.blockPlanTargetApps.collectAsStateWithLifecycle(initialValue = emptyList())
        val searchUiSettings: SearchUiSettings = koinInject()
        val focusRecoveryEnabled by searchUiSettings.focusRecoveryEnabled.collectAsStateWithLifecycle(initialValue = true)
        val focusLastResumeContext by searchUiSettings.focusLastResumeContext.collectAsStateWithLifecycle(initialValue = null)
        val focusRecoveryResumeTimeoutMinutes by searchUiSettings.focusRecoveryResumeTimeoutMinutes.collectAsStateWithLifecycle(
            initialValue = 30
        )
        val focusRecoveryFollowsCurrentBlockEnabled by searchUiSettings.focusRecoveryFollowsCurrentBlockEnabled.collectAsStateWithLifecycle(
            initialValue = true
        )
        val focusBlockPlans by searchUiSettings.focusBlockPlans.collectAsStateWithLifecycle(initialValue = emptyList())
        val focusBlockPrepPromptsEnabled by searchUiSettings.focusBlockPrepPromptsEnabled.collectAsStateWithLifecycle(
            initialValue = true
        )
        val focusPrepLeadTimeMinutes by searchUiSettings.focusPrepLeadTimeMinutes.collectAsStateWithLifecycle(initialValue = 10)
        var showCustomTimeDialog by remember { mutableStateOf(false) }
        var showBlockSetupSheet by remember { mutableStateOf(false) }
        val searchVM: SearchVM = viewModel()
        val scrollState = rememberScrollState()
        val scope = rememberCoroutineScope()
        val lastSessionMinutes = sessionSummary.lastSessionDurationMinutes ?: defaultSessionMinutes
        val today = remember { LocalDate.now(ZoneId.systemDefault()) }

        val dailyIntention by viewModel.dailyIntention.collectAsStateWithLifecycle()
        val dailyIntentionDate by viewModel.dailyIntentionDate.collectAsStateWithLifecycle()
        val pendingCommand by FocusHomeCommandCenter.pendingCommand.collectAsStateWithLifecycle(initialValue = null)
        val blockPlanTarget by remember(dailyScheduleState) {
            derivedStateOf {
                dailyScheduleState.snapshot.currentBlock ?: dailyScheduleState.snapshot.upcomingBlock
            }
        }
        val currentBlockPlan by remember(blockPlanTarget, focusBlockPlans, today) {
            derivedStateOf {
                blockPlanTarget?.let { findBlockPlan(focusBlockPlans, today, it.label) }
            }
        }
        val resumeCardState by remember(
            focusRecoveryEnabled,
            focusLastResumeContext,
            focusRecoveryResumeTimeoutMinutes,
            focusRecoveryFollowsCurrentBlockEnabled,
            dailyScheduleState,
        ) {
            derivedStateOf {
                if (!focusRecoveryEnabled) {
                    ResumeCardState(show = false)
                } else {
                    resolveScheduleAwareResumeCard(
                        lastContext = focusLastResumeContext,
                        currentBlockLabel = dailyScheduleState.snapshot.currentBlock?.label,
                        nowMillis = System.currentTimeMillis(),
                        expiryMillis = focusRecoveryResumeTimeoutMinutes * 60_000L,
                        followsCurrentBlock = focusRecoveryFollowsCurrentBlockEnabled,
                    )
                }
            }
        }
        val prepCardState by remember(
            dailyScheduleState,
            focusBlockPrepPromptsEnabled,
            focusPrepLeadTimeMinutes,
        ) {
            derivedStateOf {
                if (!focusBlockPrepPromptsEnabled) {
                    PrepCardState(show = false)
                } else {
                    resolvePrepCard(
                        currentBlock = dailyScheduleState.snapshot.currentBlock,
                        nextBlock = dailyScheduleState.snapshot.nextBlock,
                        minutesUntilCurrentBlockEnds = dailyScheduleState.snapshot.minutesUntilCurrentBlockEnds,
                        leadMinutes = focusPrepLeadTimeMinutes,
                    )
                }
            }
        }
        val blockReadiness by remember(
            currentBlockPlan,
            habitState,
            prepCardState,
            dailyScheduleState,
        ) {
            derivedStateOf {
                val requiresPrep = currentBlockPlan?.readinessChecks?.any {
                    it.source == de.mm20.launcher2.preferences.FocusReadinessSource.Prep
                } == true
                val prepSatisfied = !requiresPrep || dailyScheduleState.snapshot.currentBlock != null || prepCardState.show
                resolveBlockReadiness(
                    plan = currentBlockPlan,
                    habitsSatisfied = !habitState.gate.blocked,
                    prepSatisfied = prepSatisfied,
                )
            }
        }
        val guidanceState by remember(
            dailyScheduleState,
            prepCardState,
            resumeCardState,
            focusLastResumeContext,
            currentBlockPlan,
            blockReadiness,
            blockPlanTarget,
        ) {
            derivedStateOf {
                val base = resolveFocusGuidance(
                    currentBlock = dailyScheduleState.snapshot.currentBlock,
                    prepState = prepCardState,
                    resumeState = resumeCardState,
                    blockPlan = currentBlockPlan,
                    blockReadiness = blockReadiness,
                    guidanceBlock = blockPlanTarget,
                )
                when (base.type) {
                    FocusGuidanceType.Now -> base.copy(
                        minutesRemaining = dailyScheduleState.snapshot.minutesUntilCurrentBlockEnds,
                    )

                    FocusGuidanceType.Prep -> {
                        val prepMicroStep = focusLastResumeContext?.takeIf {
                            val contextBlock = it.scheduleBlockLabel
                            val nextBlock = prepCardState.nextBlockLabel
                            contextBlock != null &&
                                nextBlock != null &&
                                normalizeScheduleEventName(contextBlock) == normalizeScheduleEventName(nextBlock)
                        }?.microStep
                        base.copy(suggestedMicroStep = prepMicroStep)
                    }

                    FocusGuidanceType.Ready -> base

                    else -> base
                }
            }
        }

        LaunchedEffect(pendingCommand?.id, blockPlanTarget, resumeCardState.show, defaultSessionMinutes) {
            val command = pendingCommand ?: return@LaunchedEffect
            when (command.type) {
                FocusHomeCommandType.StartSession -> {
                    viewModel.startFocusSession(defaultSessionMinutes)
                    FocusHomeCommandCenter.consume(command.id)
                }
                FocusHomeCommandType.ResumeFocus -> {
                    if (resumeCardState.show) {
                        viewModel.acceptResumeContext()
                    } else if (blockPlanTarget != null) {
                        showBlockSetupSheet = true
                    } else {
                        context.startActivity(
                            Intent(context, SettingsActivity::class.java).apply {
                                putExtra(SettingsActivity.EXTRA_ROUTE, SettingsActivity.ROUTE_FOCUS_SETTINGS)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    }
                    FocusHomeCommandCenter.consume(command.id)
                }
                FocusHomeCommandType.OpenTodayPlan -> {
                    if (blockPlanTarget != null) {
                        showBlockSetupSheet = true
                    } else {
                        context.startActivity(
                            Intent(context, SettingsActivity::class.java).apply {
                                putExtra(SettingsActivity.EXTRA_ROUTE, SettingsActivity.ROUTE_FOCUS_SETTINGS)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    }
                    FocusHomeCommandCenter.consume(command.id)
                }
            }
        }

        Column(
            modifier = modifier
                .verticalScroll(scrollState)
                .padding(insets)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val todayStr = today.toString()
            DailyIntentionCard(
                intention = if (dailyIntentionDate == todayStr) dailyIntention else "",
                onSaveIntention = { intention ->
                    viewModel.setDailyIntention(intention, todayStr)
                },
                modifier = Modifier.fillMaxWidth()
            )

            ClockWidget(
                modifier = Modifier.fillMaxWidth(),
                fillScreenHeight = false,
            )

            FocusDailyScheduleCard(
                state = dailyScheduleState,
                hasBlockPlan = currentBlockPlan != null,
                onOpenBlockSetup = {
                    if (blockPlanTarget != null) {
                        showBlockSetupSheet = true
                    }
                },
                onOpenConfiguration = {
                    context.startActivity(
                        Intent(context, SettingsActivity::class.java).apply {
                            putExtra(SettingsActivity.EXTRA_ROUTE, SettingsActivity.ROUTE_FOCUS_SETTINGS)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                },
            )
            FocusQuickStartDayCard(
                show = shouldShowQuickStart && dailyScheduleState.snapshot.currentBlock == null,
                onOpenFocusApps = {
                    context.startActivity(
                        Intent(context, SettingsActivity::class.java).apply {
                            putExtra(SettingsActivity.EXTRA_ROUTE, SettingsActivity.ROUTE_FOCUS_APPS)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                },
                onOpenSchedule = {
                    context.startActivity(
                        Intent(context, SettingsActivity::class.java).apply {
                            putExtra(SettingsActivity.EXTRA_ROUTE, SettingsActivity.ROUTE_FOCUS_SETTINGS)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                },
                onOpenHabits = {
                    context.startActivity(
                        Intent(context, SettingsActivity::class.java).apply {
                            putExtra(SettingsActivity.EXTRA_ROUTE, SettingsActivity.ROUTE_FOCUS_SETTINGS)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                },
            )
            FocusGuidanceCard(
                state = guidanceState,
                hasBlockPlan = currentBlockPlan != null,
                onRecoverAccepted = { viewModel.acceptResumeContext() },
                onRecoverDismissed = { viewModel.dismissResumeContext() },
                onOpenBlockSetup = {
                    if (blockPlanTarget != null) {
                        showBlockSetupSheet = true
                    }
                },
            )
            FocusTransitionWarningCard(state = transitionWarningState)

            if (dailyScheduleState.snapshot.currentBlock != null) {
                FocusScheduleDockCard(
                    currentBlock = dailyScheduleState.snapshot.currentBlock,
                    dockApps = activeDockApps,
                )
            } else {
                if (habitState.enabled || habitState.habits.isNotEmpty()) {
                    FocusHabitCard(
                        state = habitState,
                        onHabitCheckedChanged = { habitId, completed ->
                            viewModel.setHabitCompleted(habitId, completed)
                        },
                    )
                }

                FocusInsightsCard(
                    state = insightsState,
                    onOpenInsights = {
                        context.startActivity(Intent(context, SettingsActivity::class.java).apply {
                            putExtra(SettingsActivity.EXTRA_ROUTE, FocusInsightsRoute::class.java.name)
                        })
                    }
                )

                FocusEssentialAppsCard(apps = essentialApps)

                FocusWebAppsCard(apps = webApps)
            }

            nextAlarm?.let {
                Text(
                    text = stringResource(R.string.focus_home_next_alarm, it),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }

            FocusSection(
                title = stringResource(R.string.focus_home_session_title),
                supportingText = if (focusSessionEndsAt > System.currentTimeMillis()) {
                    stringResource(R.string.focus_home_session_active, minutesRemaining)
                } else {
                    stringResource(R.string.focus_home_session_idle)
                },
            ) {
                if (focusSessionEndsAt > System.currentTimeMillis() && sessionSummary.currentSessionUnlocks > 0) {
                    Text(
                        text = stringResource(
                            R.string.focus_home_session_unlocks,
                            sessionSummary.currentSessionUnlocks,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (focusSessionEndsAt <= System.currentTimeMillis() && sessionSummary.lastSessionDurationMinutes != null) {
                    val lastSessionDurationMinutes = sessionSummary.lastSessionDurationMinutes ?: 0
                    Text(
                        text = stringResource(
                            R.string.focus_home_last_session_summary,
                            lastSessionDurationMinutes,
                            sessionSummary.lastSessionUnlocks,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (focusSessionEndsAt > System.currentTimeMillis()) {
                    FilledTonalButton(
                        onClick = { viewModel.endFocusSession() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.focus_home_end_session))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(
                            onClick = { viewModel.startFocusSession(lastSessionMinutes) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(
                                    R.string.focus_home_start_session_last,
                                    formatSessionDuration(lastSessionMinutes),
                                )
                            )
                        }
                        OutlinedButton(
                            onClick = { showCustomTimeDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.focus_home_custom_time))
                        }
                    }
                }
            }

            FocusSection(
                title = stringResource(R.string.focus_home_planning_title),
                supportingText = stringResource(R.string.focus_home_planning_subtitle)
            ) {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(context, de.mm20.launcher2.ui.launcher.focus.plan.FocusPlanActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.focus_home_plan_button))
                }
            }

            if (showResetButton) {
                OutlinedButton(
                        onClick = {
                            searchVM.reset()
                            viewModel.resetFocusHome()
                            scope.launch {
                                scrollState.scrollTo(0)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.focus_home_reset))
                    }
                }

            QuickCaptureCard(modifier = Modifier.fillMaxWidth())

            FocusSection(
                title = stringResource(R.string.focus_home_agenda_title),
            ) {
                if (nextEvents.isEmpty()) {
                    Text(
                        text = stringResource(R.string.focus_home_agenda_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    SearchResultList(nextEvents)
                }
            }
        }

        if (showCustomTimeDialog) {
            AlertDialog(
                onDismissRequest = { showCustomTimeDialog = false },
                title = { Text(stringResource(R.string.focus_home_custom_time_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (minutes in FocusSessionDurationOptions) {
                            TextButton(
                                onClick = {
                                    showCustomTimeDialog = false
                                    viewModel.startFocusSession(minutes)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(formatSessionDuration(minutes))
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showCustomTimeDialog = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
        }

        blockPlanTarget?.let { targetBlock ->
            if (showBlockSetupSheet) {
                FocusBlockSetupSheet(
                    expanded = true,
                    block = targetBlock,
                    date = today,
                    existingPlan = currentBlockPlan,
                    suggestedApps = blockPlanTargetApps,
                    onDismissRequest = { showBlockSetupSheet = false },
                    onSave = viewModel::saveBlockPlan,
                )
            }
        }
    }
}

internal class FocusHomeVM : ViewModel(), KoinComponent {
    private val appRepository: AppRepository by inject()
    private val favoritesService: FavoritesService by inject()
    private val customAttributesRepository: CustomAttributesRepository by inject()
    private val calendarRepository: CalendarRepository by inject()
    private val permissionsManager: PermissionsManager by inject()
    private val searchUiSettings: SearchUiSettings by inject()
    private val context: Context by inject()
    private val focusLaunchCoordinator = FocusLaunchCoordinator()
    private val historyRepository = FocusHistoryRepository()
    private val sessionRepository = FocusSessionRepository()
    private val focusPolicyService = FocusPolicyService()

    // Cold ticking clock: only runs while a derived flow is actually being collected
    // (i.e. the Focus Home UI is on screen). It stops when the launcher is backgrounded,
    // avoiding an always-on CPU wakeup while the home-screen VM stays alive. 60s resolution
    // is enough because the UI only renders minutes. `.value` still returns the last emit.
    private val currentTime: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            kotlinx.coroutines.delay(60_000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), System.currentTimeMillis())

    private val hasCalendarPermission = permissionsManager.hasPermission(PermissionGroup.Calendar)
    private val availableCalendars = calendarRepository.getCalendars()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val nextEvents = combine(
        hasCalendarPermission,
        calendarRepository.findMany(),
        searchUiSettings.focusUpcomingEventsCalendarIds,
        availableCalendars,
    ) { hasPermission, events, selectedCalendarIds, calendars ->
        if (!hasPermission) emptyList()
        else {
            val filteredEvents = if (selectedCalendarIds.isEmpty()) {
                events
            } else {
                val excludedIds = calendars.filterNot { it.id in selectedCalendarIds }.map { it.id }.toSet()
                events.filterNot { event ->
                    val calendarId = calendars.firstOrNull { it.name == event.calendarName }?.id
                    calendarId != null && calendarId in excludedIds
                }
            }
            filteredEvents
            .asSequence()
            .filter { it.endTime >= System.currentTimeMillis() }
            .sortedBy { it.startTime ?: it.endTime }
            .take(3)
            .toList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val focusSessionEndsAt = searchUiSettings.focusSessionEndsAt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0L)

    val defaultSessionMinutes = searchUiSettings.focusDefaultSessionMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 15)

    val minutesRemaining = searchUiSettings.focusSessionEndsAt
        .map { endsAt ->
            ((endsAt - System.currentTimeMillis()).coerceAtLeast(0L) / 60_000L).toInt()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    val nextAlarm = searchUiSettings.focusSessionEndsAt
        .map {
            val alarmManager = context.getSystemService<AlarmManager>()
            alarmManager?.nextAlarmClock?.triggerTime?.let { triggerTime ->
                java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT)
                    .format(java.util.Date(triggerTime))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val sessionSummary = combine(
        sessionRepository.getLatestSession(),
        historyRepository.getRecentEvents(100),
        searchUiSettings.focusSessionEndsAt,
    ) { latestSession, events, endsAt ->
        if (latestSession == null) {
            FocusSessionUiSummary()
        } else {
            val effectiveEnd = latestSession.endedAt ?: minOf(System.currentTimeMillis(), latestSession.plannedEndsAt)
            val durationMinutes = ((effectiveEnd - latestSession.startedAt).coerceAtLeast(0L) / 60_000L).toInt()
            val unlocks = events.count {
                it.timestamp >= latestSession.startedAt && it.timestamp <= effectiveEnd && it.duringFocusSession
            }
            if (endsAt > System.currentTimeMillis() && latestSession.plannedEndsAt >= System.currentTimeMillis()) {
                FocusSessionUiSummary(currentSessionUnlocks = unlocks)
            } else {
                FocusSessionUiSummary(
                    lastSessionDurationMinutes = durationMinutes,
                    lastSessionUnlocks = unlocks,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), FocusSessionUiSummary())

    private val currentDay = currentTime
        .map { it.toLocalDate() }
        .distinctUntilChanged()

    private val selectedDailyScheduleCalendar = combine(
        searchUiSettings.focusDailyScheduleCalendarId,
        availableCalendars,
    ) { calendarId, calendars ->
        if (calendarId == null) {
            null
        } else {
            calendars.firstOrNull { it.id == calendarId }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    private val dailyScheduleEvents = combine(
        searchUiSettings.focusDailyScheduleEnabled,
        currentDay,
        selectedDailyScheduleCalendar,
        availableCalendars,
    ) { enabled, day, selectedCalendar, calendars ->
        DailyScheduleRequest(enabled, day, selectedCalendar, calendars)
    }.flatMapLatest { request ->
        if (!request.enabled || request.selectedCalendar == null) {
            flowOf(emptyList())
        } else {
            val zone = ZoneId.systemDefault()
            val dayStart = request.day.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = request.day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val excludedCalendars = request.calendars
                .filterNot { it.id == request.selectedCalendar.id }
                .map { it.id }
            calendarRepository.findMany(
                from = dayStart,
                to = dayEnd,
                excludeCalendars = excludedCalendars,
                excludeAllDayEvents = true,
            ).map { events ->
                events.map { it.toDailyScheduleBlock() }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val dailyScheduleState = combine(
        searchUiSettings.focusDailyScheduleEnabled,
        selectedDailyScheduleCalendar,
        dailyScheduleEvents,
        currentTime,
    ) { enabled, selectedCalendar, events, nowMillis ->
        DailySchedulePanelState(
            enabled = enabled,
            calendarSelected = selectedCalendar != null,
            snapshot = resolveDailyScheduleSnapshot(events, nowMillis),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), DailySchedulePanelState())

    val transitionWarningState = combine(
        searchUiSettings.focusTransitionWarningsEnabled,
        searchUiSettings.focusTransitionWarningLeadMinutes,
        dailyScheduleState,
    ) { enabled, leadMinutes, dailySchedule ->
        if (!enabled) {
            TransitionWarningState(show = false)
        } else {
            resolveTransitionWarning(
                minutesUntilBlockEnd = dailySchedule.snapshot.minutesUntilCurrentBlockEnds,
                nextBlockLabel = dailySchedule.snapshot.nextBlock?.label,
                leadMinutes = leadMinutes,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), TransitionWarningState(show = false))

    val habitState = combine(
        searchUiSettings.focusHabitsEnabled,
        searchUiSettings.focusHabits,
        currentTime,
    ) { enabled, habits, nowMillis ->
        val now = java.time.Instant.ofEpochMilli(nowMillis).atZone(java.time.ZoneId.systemDefault())
        HabitPanelState(
            enabled = enabled,
            habits = if (!enabled) emptyList() else resolveHabitStatuses(
                habits = habits,
                today = now.toLocalDate(),
                now = now.toLocalDateTime(),
            ),
            gate = if (!enabled) HabitGateState(blocked = false, overdueCount = 0) else resolveHabitGate(
                habits = habits,
                today = now.toLocalDate(),
                now = now.toLocalDateTime(),
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), HabitPanelState())

    val insightsState = historyRepository.getWeeklyReport().map { report ->
        FocusInsightsPanelState(
            streakDays = report.streakDays,
            show = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), FocusInsightsPanelState())

    val activeDockApps = combine(
        dailyScheduleState,
        currentDay,
        searchUiSettings.focusBlockPlans,
        searchUiSettings.focusScheduleDockMappings,
        appRepository.findMany(),
    ) { scheduleState, today, blockPlans, mappings, apps ->
        val blockPlan = scheduleState.snapshot.currentBlock?.let {
            findBlockPlan(blockPlans, today, it.label)
        }
        resolvePreferredDockApps(
            currentBlock = scheduleState.snapshot.currentBlock,
            blockPlan = blockPlan,
            mappings = mappings,
            apps = apps,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val essentialApps = combine(
        searchUiSettings.focusEssentialAppKeys,
        appRepository.findMany(),
    ) { essentialKeys, apps ->
        apps.filter { it.key in essentialKeys }.sortedBy { it.labelOverride ?: it.label }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    // Pinned launcher shortcuts (e.g. PWAs added from a browser). The focus home has no other
    // surface for these, so they would otherwise be invisible after being pinned.
    val webApps = favoritesService.getFavorites(
        includeTypes = listOf("shortcut", "legacyshortcut"),
        minPinnedLevel = PinnedLevel.AutomaticallySorted,
        limit = 12,
    ).withCustomLabels(customAttributesRepository)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val shouldShowQuickStart = combine(
        searchUiSettings.focusModeEnabled,
        searchUiSettings.focusEssentialAppKeys,
        searchUiSettings.focusDistractingAppKeys,
        searchUiSettings.focusDailyScheduleEnabled,
        searchUiSettings.focusHabitsEnabled,
    ) { focusModeEnabled, essentialKeys, distractingKeys, dailyScheduleEnabled, habitsEnabled ->
        shouldShowFocusQuickStart(
            focusModeEnabled = focusModeEnabled,
            essentialCount = essentialKeys.size,
            distractingCount = distractingKeys.size,
            dailyScheduleEnabled = dailyScheduleEnabled,
            habitsEnabled = habitsEnabled,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val blockPlanTargetApps = combine(
        dailyScheduleState,
        currentDay,
        searchUiSettings.focusBlockPlans,
        searchUiSettings.focusScheduleDockMappings,
        appRepository.findMany(),
    ) { scheduleState, today, blockPlans, mappings, apps ->
        val targetBlock = scheduleState.snapshot.currentBlock ?: scheduleState.snapshot.upcomingBlock
        val blockPlan = targetBlock?.let {
            findBlockPlan(blockPlans, today, it.label)
        }
        resolvePreferredDockApps(
            currentBlock = targetBlock,
            blockPlan = blockPlan,
            mappings = mappings,
            apps = apps,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val showResetButton = searchUiSettings.focusResetButtonEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    fun startFocusSession(minutes: Int) {
        viewModelScope.launch {
            searchUiSettings.setFocusLastResumeContext(null)
            focusPolicyService.beginFocusSession(context, minutes)
        }
    }

    fun endFocusSession() {
        viewModelScope.launch {
            focusPolicyService.endFocusSession(context)
        }
    }

    fun setHabitCompleted(id: String, completed: Boolean) {
        val today = java.time.Instant.ofEpochMilli(currentTime.value)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .toString()
        searchUiSettings.setFocusHabitCompleted(id, today, completed)
    }

    fun clearResumeContext() {
        searchUiSettings.setFocusLastResumeContext(null)
    }

    fun resetFocusHome() {
        searchUiSettings.setFocusLastResumeContext(null)
    }

    fun acceptResumeContext() {
        viewModelScope.launch {
            val resumeContext = searchUiSettings.focusLastResumeContext.first() ?: return@launch
            val appKey = resumeContext.appKey ?: return@launch
            val app = appRepository.findMany().first().firstOrNull { it.key == appKey } ?: return@launch
            historyRepository.logEvent(
                FocusLogEvent(
                    appKey = appKey,
                    appLabel = resumeContext.taskLabel,
                    reason = "",
                    eventKind = FocusEventKind.ResumeAccepted.value,
                    scheduleBlockLabel = resumeContext.scheduleBlockLabel,
                    microStep = resumeContext.microStep,
                    unlockDurationMinutes = 0,
                    usedEmergencyBypass = false,
                    duringFocusSession = false,
                    budgetBlocked = false,
                    scheduleBlocked = false,
                    effectiveDelaySeconds = 0,
                )
            )
            val launched = focusLaunchCoordinator.launchDirect(app, context)
            if (launched) {
                searchUiSettings.setFocusLastResumeContext(null)
            }
        }
    }

    fun dismissResumeContext() {
        viewModelScope.launch {
            val context = searchUiSettings.focusLastResumeContext.first() ?: return@launch
            historyRepository.logEvent(
                FocusLogEvent(
                    appKey = context.appKey ?: return@launch,
                    appLabel = context.taskLabel,
                    reason = "",
                    eventKind = FocusEventKind.ResumeDismissed.value,
                    scheduleBlockLabel = context.scheduleBlockLabel,
                    microStep = context.microStep,
                    unlockDurationMinutes = 0,
                    usedEmergencyBypass = false,
                    duringFocusSession = false,
                    budgetBlocked = false,
                    scheduleBlocked = false,
                    effectiveDelaySeconds = 0,
                )
            )
            searchUiSettings.setFocusLastResumeContext(null)
        }
    }

    fun saveBlockPlan(plan: FocusBlockPlan) {
        searchUiSettings.upsertFocusBlockPlan(plan)
    }

    val dailyIntention = searchUiSettings.focusDailyIntention
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    val dailyIntentionDate = searchUiSettings.focusDailyIntentionDate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    fun setDailyIntention(intention: String, date: String) {
        searchUiSettings.setFocusDailyIntention(intention, date)
    }
}

data class FocusSessionUiSummary(
    val currentSessionUnlocks: Int = 0,
    val lastSessionDurationMinutes: Int? = null,
    val lastSessionUnlocks: Int = 0,
)

private data class DailyScheduleRequest(
    val enabled: Boolean,
    val day: LocalDate,
    val selectedCalendar: de.mm20.launcher2.calendar.providers.CalendarList?,
    val calendars: List<de.mm20.launcher2.calendar.providers.CalendarList>,
)
