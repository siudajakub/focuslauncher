package de.mm20.launcher2.ui.launcher.scaffold

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.app.AlarmManager
import android.provider.MediaStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.getSystemService
import de.mm20.launcher2.data.customattrs.CustomAttributesRepository
import de.mm20.launcher2.calendar.CalendarRepository
import de.mm20.launcher2.ktx.tryStartActivity
import de.mm20.launcher2.permissions.PermissionGroup
import de.mm20.launcher2.permissions.PermissionsManager
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import de.mm20.launcher2.search.CalendarEvent
import de.mm20.launcher2.searchable.PinnedLevel
import de.mm20.launcher2.services.favorites.FavoritesService
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.launcher.focus.FocusHistoryRepository
import de.mm20.launcher2.ui.launcher.focus.FocusAppClassifier
import de.mm20.launcher2.ui.launcher.focus.FocusAppType
import de.mm20.launcher2.ui.launcher.focus.FocusPolicyService
import de.mm20.launcher2.ui.launcher.focus.FocusSessionRepository
import de.mm20.launcher2.weather.WeatherRepository
import de.mm20.launcher2.ui.launcher.search.common.list.SearchResultList
import de.mm20.launcher2.ui.launcher.widgets.clock.ClockWidget
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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
        val viewModel: FocusHomeVM = viewModel()
        val essentials by viewModel.essentials.collectAsStateWithLifecycle(initialValue = emptyList())
        val nextEvent by viewModel.nextEvent.collectAsStateWithLifecycle(initialValue = null)
        val focusSessionEndsAt by viewModel.focusSessionEndsAt.collectAsStateWithLifecycle(initialValue = 0L)
        val minutesRemaining by viewModel.minutesRemaining.collectAsStateWithLifecycle(initialValue = 0)
        val emergencyBypassActive by viewModel.emergencyBypassActive.collectAsStateWithLifecycle(initialValue = false)
        val commuteModeEnabled by viewModel.commuteModeEnabled.collectAsStateWithLifecycle(initialValue = false)
        val atAGlanceEnabled by viewModel.atAGlanceEnabled.collectAsStateWithLifecycle(initialValue = true)
        val atAGlance by viewModel.atAGlance.collectAsStateWithLifecycle(initialValue = FocusAtAGlance())
        val sessionSummary by viewModel.sessionSummary.collectAsStateWithLifecycle(initialValue = FocusSessionUiSummary())

        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .padding(insets)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ClockWidget(
                modifier = Modifier.fillMaxWidth(),
                fillScreenHeight = false,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.focus_home_session_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = if (focusSessionEndsAt > System.currentTimeMillis()) {
                            stringResource(R.string.focus_home_session_active, minutesRemaining)
                        } else {
                            stringResource(R.string.focus_home_session_idle)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (emergencyBypassActive) {
                        Text(
                            text = stringResource(R.string.focus_home_bypass_active),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (atAGlanceEnabled && (atAGlance.nextAlarm != null || atAGlance.weatherSummary != null)) {
                        Text(
                            text = buildString {
                                atAGlance.nextAlarm?.let {
                                    append(stringResource(R.string.focus_home_next_alarm, it))
                                }
                                if (atAGlance.nextAlarm != null && atAGlance.weatherSummary != null) append(" • ")
                                atAGlance.weatherSummary?.let { append(it) }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (focusSessionEndsAt > System.currentTimeMillis()) {
                            OutlinedButton(
                                onClick = { viewModel.endFocusSession() },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.focus_home_end_session))
                            }
                        } else {
                            listOf(10, 25).forEach { minutes ->
                                Button(
                                    onClick = { viewModel.startFocusSession(minutes) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(stringResource(R.string.focus_home_start_session, minutes))
                                }
                            }
                        }
                    }
                    if (commuteModeEnabled) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            stringResource(R.string.focus_quick_phone) to Intent(Intent.ACTION_DIAL),
                            stringResource(R.string.focus_quick_sms) to Intent(Intent.ACTION_MAIN).apply { type = "vnd.android-dir/mms-sms" },
                            stringResource(R.string.focus_quick_camera) to Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
                            stringResource(R.string.focus_quick_maps) to Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")),
                            stringResource(R.string.focus_quick_calendar) to Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_APP_CALENDAR)
                            },
                        ).forEach { (label, intent) ->
                            OutlinedButton(
                                onClick = { viewModel.launchQuickAction(intent) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(label)
                            }
                        }
                    }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.focus_home_essentials_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (essentials.isEmpty()) {
                        Text(
                            text = stringResource(R.string.focus_home_essentials_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        SearchResultList(essentials)
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.focus_home_agenda_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (nextEvent == null) {
                        Text(
                            text = stringResource(R.string.focus_home_agenda_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        SearchResultList(listOf(nextEvent!!))
                    }
                }
            }
        }
    }
}

internal class FocusHomeVM : ViewModel(), KoinComponent {
    private val favoritesService: FavoritesService by inject()
    private val calendarRepository: CalendarRepository by inject()
    private val customAttributesRepository: CustomAttributesRepository by inject()
    private val permissionsManager: PermissionsManager by inject()
    private val searchUiSettings: SearchUiSettings by inject()
    private val weatherRepository: WeatherRepository by inject()
    private val context: Context by inject()
    private val historyRepository = FocusHistoryRepository()
    private val sessionRepository = FocusSessionRepository()
    private val focusPolicyService = FocusPolicyService()
    private val focusAppClassifier = FocusAppClassifier()

    private val baseEssentials = favoritesService.getFavorites(
        excludeTypes = listOf("calendar", "tasks.org", "plugin.calendar", "tag"),
        minPinnedLevel = PinnedLevel.AutomaticallySorted,
        maxPinnedLevel = PinnedLevel.ManuallySorted,
        limit = 6,
    )

    val essentials = combine(
        baseEssentials,
        searchUiSettings.focusSessionEndsAt,
    ) { apps, sessionEndsAt -> apps to (sessionEndsAt > System.currentTimeMillis()) }
        .flatMapLatest { (apps, sessionActive) ->
            focusAppClassifier.classify(apps.map { it.key }).map { appTypes ->
                if (!sessionActive) apps
                else apps.filter { appTypes[it.key] == FocusAppType.Essential }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private val hasCalendarPermission = permissionsManager.hasPermission(PermissionGroup.Calendar)

    val nextEvent = hasCalendarPermission.combine(
        calendarRepository.findMany(
            from = System.currentTimeMillis(),
            to = System.currentTimeMillis() + 24 * 60 * 60 * 1000L,
        ),
    ) { hasPermission, events ->
        if (!hasPermission) null
        else events.firstOrNull { it.endTime >= System.currentTimeMillis() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val focusSessionEndsAt = searchUiSettings.focusSessionEndsAt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0L)

    val minutesRemaining = searchUiSettings.focusSessionEndsAt
        .map { endsAt ->
            ((endsAt - System.currentTimeMillis()).coerceAtLeast(0L) / 60_000L).toInt()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    val emergencyBypassActive = searchUiSettings.focusEmergencyBypassEndsAt
        .map { it > System.currentTimeMillis() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val commuteModeEnabled = searchUiSettings.focusCommuteModeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val atAGlanceEnabled = searchUiSettings.focusAtAGlanceEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    val atAGlance = combine(
        searchUiSettings.focusAtAGlanceEnabled,
        weatherRepository.getForecasts(limit = 1),
    ) { enabled, forecasts ->
        if (!enabled) {
            FocusAtAGlance()
        } else {
            val alarmManager = context.getSystemService<AlarmManager>()
            FocusAtAGlance(
                nextAlarm = alarmManager?.nextAlarmClock?.triggerTime?.let { java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(it)) },
                weatherSummary = forecasts.firstOrNull()?.temperature?.let { "${it.toInt()}°" },
            )
        }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), FocusAtAGlance())

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

    fun startFocusSession(minutes: Int) {
        viewModelScope.launch {
            focusPolicyService.beginFocusSession(context, minutes)
        }
    }

    fun endFocusSession() {
        viewModelScope.launch {
            focusPolicyService.endFocusSession(context)
        }
    }

    fun launchQuickAction(intent: Intent) {
        context.tryStartActivity(intent)
    }
}

data class FocusAtAGlance(
    val nextAlarm: String? = null,
    val weatherSummary: String? = null,
)

data class FocusSessionUiSummary(
    val currentSessionUnlocks: Int = 0,
    val lastSessionDurationMinutes: Int? = null,
    val lastSessionUnlocks: Int = 0,
)
