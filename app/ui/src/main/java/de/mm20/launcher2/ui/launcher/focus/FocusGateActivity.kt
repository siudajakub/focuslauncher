package de.mm20.launcher2.ui.launcher.focus

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import de.mm20.launcher2.data.customattrs.CustomAttributesRepository
import de.mm20.launcher2.data.customattrs.FocusProfile
import de.mm20.launcher2.icons.IconService
import de.mm20.launcher2.search.Application
import de.mm20.launcher2.searchable.SavableSearchableRepository
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.base.BaseActivity
import de.mm20.launcher2.ui.base.ProvideSettings
import de.mm20.launcher2.ui.component.ShapedLauncherIcon
import de.mm20.launcher2.ui.ktx.toPixels
import de.mm20.launcher2.ui.overlays.OverlayHost
import de.mm20.launcher2.ui.theme.LauncherTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject

class FocusGateActivity : BaseActivity() {
    private val searchableRepository: SavableSearchableRepository by inject()
    private val customAttributesRepository: CustomAttributesRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val searchableKey = intent.getStringExtra(EXTRA_SEARCHABLE_KEY) ?: return finish()

        setContent {
            LauncherTheme {
                ProvideSettings {
                    OverlayHost {
                        FocusGateRoute(
                            searchableKey = searchableKey,
                            searchableRepository = searchableRepository,
                            customAttributesRepository = customAttributesRepository,
                            onDismiss = { finish() },
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_SEARCHABLE_KEY = "searchable_key"

        fun intent(context: android.content.Context, searchable: Application): Intent {
            return Intent(context, FocusGateActivity::class.java)
                .putExtra(EXTRA_SEARCHABLE_KEY, searchable.key)
        }
    }
}

@Composable
private fun FocusGateRoute(
    searchableKey: String,
    searchableRepository: SavableSearchableRepository,
    customAttributesRepository: CustomAttributesRepository,
    onDismiss: () -> Unit,
) {
    val state by remember(searchableKey) {
        searchableRepository.getByKeys(listOf(searchableKey))
            .map { FocusGateRouteState.Ready(it.firstOrNull() as? Application) as FocusGateRouteState }
    }.collectAsState(initial = FocusGateRouteState.Loading)

    when (state) {
        FocusGateRouteState.Loading -> return
        is FocusGateRouteState.Ready -> Unit
    }

    val app = (state as FocusGateRouteState.Ready).app

    if (app == null) {
        LaunchedEffect(searchableKey, state) {
            onDismiss()
        }
        return
    }

    val profile by remember(app.key) {
        customAttributesRepository.getFocusProfile(app)
    }.collectAsState(initial = FocusProfile())

    FocusGateScreen(
        app = app,
        profile = profile,
        customAttributesRepository = customAttributesRepository,
        onDismiss = onDismiss,
    )
}

private sealed interface FocusGateRouteState {
    data object Loading : FocusGateRouteState
    data class Ready(val app: Application?) : FocusGateRouteState
}

@Composable
private fun FocusGateScreen(
    app: Application,
    profile: FocusProfile,
    customAttributesRepository: CustomAttributesRepository,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val launchCoordinator = remember { FocusLaunchCoordinator() }
    val focusPolicyService = remember { FocusPolicyService() }
    val historyRepository = remember { FocusHistoryRepository() }
    val iconService: IconService = koinInject()
    val searchUiSettings: SearchUiSettings = koinInject()
    val iconSizePx = 64.dp.toPixels().toInt()
    val icon by remember(app.key, iconSizePx) {
        iconService.getIcon(app, iconSizePx)
    }.collectAsState(initial = null)
    val focusSessionEndsAt by searchUiSettings.focusSessionEndsAt.collectAsState(initial = 0L)
    val defaultSessionMinutes by searchUiSettings.focusDefaultSessionMinutes.collectAsState(initial = 15)
    val focusSessionActive = focusSessionEndsAt > System.currentTimeMillis()

    var sessionMinutes by remember(defaultSessionMinutes) {
        mutableIntStateOf(defaultSessionMinutes)
    }
    var reason by remember { mutableStateOf("") }
    var decision by remember(app.key, profile) { mutableStateOf<FocusPolicyDecision?>(null) }
    var secondsRemaining by remember { mutableIntStateOf(0) }
    var hasLaunched by remember { mutableStateOf(false) }

    LaunchedEffect(app.key, profile, focusSessionActive) {
        decision = focusPolicyService.evaluate(app)
        sessionMinutes = defaultSessionMinutes
        val policy = decision ?: return@LaunchedEffect
        if (!policy.requiresGate && !hasLaunched) {
            hasLaunched = true
            launchCoordinator.launchDirect(app, context)
            onDismiss()
        }
    }

    LaunchedEffect(decision?.effectiveDelaySeconds) {
        secondsRemaining = decision?.effectiveDelaySeconds ?: 0
        while (secondsRemaining > 0) {
            delay(1_000)
            secondsRemaining -= 1
        }
    }

    val message = when {
        decision?.blockReason == FocusBlockReason.DailyBudget ->
            stringResource(R.string.focus_gate_message_budget)
        decision?.blockReason == FocusBlockReason.FocusSessionLock ->
            stringResource(R.string.focus_gate_message_session_lock)
        decision?.blockReason == FocusBlockReason.HardBlockWindow ->
            stringResource(R.string.focus_gate_message_hard_block)
        focusSessionActive ->
            stringResource(R.string.focus_gate_message_session)
        else ->
            stringResource(R.string.focus_gate_message_distracting)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Center,
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ShapedLauncherIcon(
                        size = 56.dp,
                        icon = { icon },
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.labelOverride ?: app.label,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = app.componentName.packageName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                )

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(stringResource(R.string.focus_gate_reason_label)) },
                    placeholder = { Text(stringResource(R.string.focus_gate_reason_placeholder)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    minLines = 2,
                    maxLines = 3,
                )

                Text(
                    text = stringResource(R.string.focus_gate_session_static_value, sessionMinutes),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                    ) {
                        Text(stringResource(R.string.focus_gate_cancel))
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = !decision?.hardBlocked.orFalse() && secondsRemaining == 0 && reason.isNotBlank(),
                        onClick = {
                            customAttributesRepository.setFocusProfile(
                                app,
                                profile.withTemporaryUnlockUntil(
                                    System.currentTimeMillis() + sessionMinutes * 60_000L
                                ),
                            )
                            kotlinx.coroutines.runBlocking {
                                historyRepository.logEvent(
                                    FocusLogEvent(
                                        appKey = app.key,
                                        appLabel = app.labelOverride ?: app.label,
                                        reason = reason,
                                        unlockDurationMinutes = sessionMinutes,
                                        usedEmergencyBypass = false,
                                        duringFocusSession = focusSessionActive,
                                        budgetBlocked = decision?.budgetBlocked == true,
                                        scheduleBlocked = decision?.blockReason == FocusBlockReason.HardBlockWindow,
                                        effectiveDelaySeconds = decision?.effectiveDelaySeconds ?: 0,
                                    )
                                )
                            }
                            launchCoordinator.launchDirect(app, context)
                            onDismiss()
                        },
                    ) {
                        val label = if (secondsRemaining > 0) {
                            stringResource(R.string.focus_gate_continue_delay, secondsRemaining)
                        } else {
                            stringResource(R.string.focus_gate_continue)
                        }
                        Text(label)
                    }
                }
                if (decision?.bypassAllowed == true) {
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        enabled = reason.isNotBlank(),
                        onClick = {
                            kotlinx.coroutines.runBlocking {
                                focusPolicyService.activateEmergencyBypass(reason, sessionMinutes)
                                historyRepository.logEvent(
                                    FocusLogEvent(
                                        appKey = app.key,
                                        appLabel = app.labelOverride ?: app.label,
                                        reason = reason,
                                        unlockDurationMinutes = sessionMinutes,
                                        usedEmergencyBypass = true,
                                        duringFocusSession = focusSessionActive,
                                        budgetBlocked = decision?.budgetBlocked == true,
                                        scheduleBlocked = decision?.blockReason == FocusBlockReason.HardBlockWindow,
                                        effectiveDelaySeconds = 0,
                                    )
                                )
                            }
                            launchCoordinator.launchDirect(app, context)
                            onDismiss()
                        },
                    ) {
                        Text(stringResource(R.string.focus_gate_emergency_bypass))
                    }
                }
            }
        }
    }
}

private fun Boolean?.orFalse(): Boolean = this == true
