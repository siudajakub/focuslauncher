package de.mm20.launcher2.ui.launcher.focus

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import de.mm20.launcher2.applications.AppRepository
import de.mm20.launcher2.calendar.CalendarRepository
import de.mm20.launcher2.data.customattrs.CustomAttributesRepository
import de.mm20.launcher2.data.customattrs.FocusProfile
import de.mm20.launcher2.preferences.FocusResumeContext
import de.mm20.launcher2.icons.IconService
import de.mm20.launcher2.search.Application
import de.mm20.launcher2.searchable.SavableSearchableRepository
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.base.BaseActivity
import de.mm20.launcher2.ui.base.ProvideSettings
import de.mm20.launcher2.ui.launcher.LauncherActivity
import de.mm20.launcher2.ui.component.ShapedLauncherIcon
import de.mm20.launcher2.ui.ktx.toPixels
import de.mm20.launcher2.ui.overlays.OverlayHost
import de.mm20.launcher2.ui.theme.LauncherTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject

class FocusGateActivity : BaseActivity() {
    private val searchableRepository: SavableSearchableRepository by inject()
    private val customAttributesRepository: CustomAttributesRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
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
                            onGoBack = { returnToLauncherHome() },
                            onFinish = { finish() },
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_SEARCHABLE_KEY = "searchable_key"
        internal const val EXTRA_LAUNCH_LEFT = "launch_left"
        internal const val EXTRA_LAUNCH_TOP = "launch_top"
        internal const val EXTRA_LAUNCH_RIGHT = "launch_right"
        internal const val EXTRA_LAUNCH_BOTTOM = "launch_bottom"

        fun intent(
            context: android.content.Context,
            searchable: Application,
            launchBounds: IntRect? = null,
        ): Intent {
            return Intent(context, FocusGateActivity::class.java)
                .putExtra(EXTRA_SEARCHABLE_KEY, searchable.key)
                .apply {
                    if (launchBounds != null) {
                        putExtra(EXTRA_LAUNCH_LEFT, launchBounds.left)
                        putExtra(EXTRA_LAUNCH_TOP, launchBounds.top)
                        putExtra(EXTRA_LAUNCH_RIGHT, launchBounds.right)
                        putExtra(EXTRA_LAUNCH_BOTTOM, launchBounds.bottom)
                    }
                }
        }
    }

    private fun returnToLauncherHome() {
        val intent = Intent(this, LauncherActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_HOME)
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        finish()
    }
}

@Composable
private fun FocusGateRoute(
    searchableKey: String,
    searchableRepository: SavableSearchableRepository,
    customAttributesRepository: CustomAttributesRepository,
    onGoBack: () -> Unit,
    onFinish: () -> Unit,
) {
    val activity = LocalContext.current as? FocusGateActivity
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
            onFinish()
        }
        return
    }

    val profile by remember(app.key) {
        customAttributesRepository.getFocusProfile(app)
    }.collectAsState(initial = FocusProfile())

    val launchBounds = remember(activity) {
        val intent = activity?.intent
        val left = intent?.getIntExtra(FocusGateActivity.EXTRA_LAUNCH_LEFT, Int.MIN_VALUE) ?: Int.MIN_VALUE
        val top = intent?.getIntExtra(FocusGateActivity.EXTRA_LAUNCH_TOP, Int.MIN_VALUE) ?: Int.MIN_VALUE
        val right = intent?.getIntExtra(FocusGateActivity.EXTRA_LAUNCH_RIGHT, Int.MIN_VALUE) ?: Int.MIN_VALUE
        val bottom = intent?.getIntExtra(FocusGateActivity.EXTRA_LAUNCH_BOTTOM, Int.MIN_VALUE) ?: Int.MIN_VALUE
        if (left == Int.MIN_VALUE || top == Int.MIN_VALUE || right == Int.MIN_VALUE || bottom == Int.MIN_VALUE) {
            null
        } else {
            IntRect(left, top, right, bottom)
        }
    }

    FocusGateScreen(
        app = app,
        profile = profile,
        customAttributesRepository = customAttributesRepository,
        launchBounds = launchBounds,
        onGoBack = onGoBack,
        onFinish = onFinish,
    )
}

private sealed interface FocusGateRouteState {
    data object Loading : FocusGateRouteState
    data class Ready(val app: Application?) : FocusGateRouteState
}

private enum class FocusGateStage {
    Entry,
    Animation,
    Intent,
    Blocked,
}

@Composable
private fun FocusGateScreen(
    app: Application,
    profile: FocusProfile,
    customAttributesRepository: CustomAttributesRepository,
    launchBounds: IntRect?,
    onGoBack: () -> Unit,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val launchCoordinator = remember { FocusLaunchCoordinator() }
    val focusPolicyService = remember { FocusPolicyService() }
    val historyRepository = remember { FocusHistoryRepository() }
    val calendarRepository: CalendarRepository = koinInject()
    val appRepository: AppRepository = koinInject()
    val iconService: IconService = koinInject()
    val searchUiSettings: SearchUiSettings = koinInject()
    val iconSizePx = 64.dp.toPixels().toInt()
    val icon by remember(app.key, iconSizePx) {
        iconService.getIcon(app, iconSizePx)
    }.collectAsState(initial = null)
    val allApps by remember {
        appRepository.findMany()
    }.collectAsState(initial = emptyList())
    val focusSessionEndsAt by searchUiSettings.focusSessionEndsAt.collectAsState(initial = 0L)
    val defaultSessionMinutes by searchUiSettings.focusDefaultSessionMinutes.collectAsState(initial = 15)
    val capMinutes by searchUiSettings.focusDistractingSessionCapMinutes.collectAsState(initial = 15)
    val startRitualEnabled by searchUiSettings.focusStartRitualEnabled.collectAsState(initial = true)
    val microStepPromptEnabled by searchUiSettings.focusMicroStepPromptEnabled.collectAsState(initial = true)
    val focusLastResumeContext by searchUiSettings.focusLastResumeContext.collectAsState(initial = null)
    val focusRecoveryFollowsCurrentBlockEnabled by searchUiSettings.focusRecoveryFollowsCurrentBlockEnabled.collectAsState(initial = true)
    val focusBlockAwareSessionSizingEnabled by searchUiSettings.focusBlockAwareSessionSizingEnabled.collectAsState(initial = true)
    val focusPrepLeadTimeMinutes by searchUiSettings.focusPrepLeadTimeMinutes.collectAsState(initial = 10)
    val focusBlockPlans by searchUiSettings.focusBlockPlans.collectAsState(initial = emptyList())
    val focusScheduleDockMappings by searchUiSettings.focusScheduleDockMappings.collectAsState(initial = emptyList())
    val focusSessionActive = focusSessionEndsAt > System.currentTimeMillis()
    val currentScheduleSnapshot by remember {
        kotlinx.coroutines.flow.combine(
            searchUiSettings.focusDailyScheduleEnabled,
            searchUiSettings.focusDailyScheduleCalendarId,
            calendarRepository.getCalendars(),
        ) { enabled, calendarId, calendars ->
            Triple(enabled, calendarId, calendars)
        }.flatMapLatest { (enabled, calendarId, calendars) ->
            if (!enabled || calendarId == null) {
                flowOf(DailyScheduleSnapshot())
            } else {
                val selectedCalendar = calendars.firstOrNull { it.id == calendarId }
                if (selectedCalendar == null) {
                    flowOf(DailyScheduleSnapshot())
                } else {
                    val zone = ZoneId.systemDefault()
                    val today = LocalDate.now(zone)
                    val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
                    val dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                    val excludedCalendars = calendars.filterNot { it.id == selectedCalendar.id }.map { it.id }
                    calendarRepository.findMany(
                        from = dayStart,
                        to = dayEnd,
                        excludeCalendars = excludedCalendars,
                        excludeAllDayEvents = true,
                    ).map { events ->
                        resolveDailyScheduleSnapshot(
                            events = events.map { it.toDailyScheduleBlock() },
                            nowMillis = System.currentTimeMillis(),
                        )
                    }
                }
            }
        }
    }.collectAsState(initial = DailyScheduleSnapshot())

    var sessionMinutes by remember(defaultSessionMinutes) {
        mutableIntStateOf(defaultSessionMinutes.coerceAtMost(capMinutes))
    }
    var reason by remember { mutableStateOf("") }
    var microStep by remember { mutableStateOf("") }
    var showBlockSetupSheet by remember { mutableStateOf(false) }
    var decision by remember(app.key, profile) { mutableStateOf<FocusPolicyDecision?>(null) }
    var hasLaunched by remember { mutableStateOf(false) }
    var stage by remember { mutableStateOf(if (launchBounds != null) FocusGateStage.Entry else FocusGateStage.Animation) }
    var countdownSeconds by remember { mutableIntStateOf(0) }
    val fillProgress = remember { Animatable(0f) }
    val entryProgress = remember { Animatable(if (launchBounds != null) 0f else 1f) }

    val backgroundColor = MaterialTheme.colorScheme.surfaceDim.copy(alpha = 0.98f)
    val primaryText = MaterialTheme.colorScheme.onPrimaryContainer
    val secondaryText = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
    val gradientTop = MaterialTheme.colorScheme.primaryContainer
    val gradientMiddle = MaterialTheme.colorScheme.secondaryContainer
    val gradientBottom = MaterialTheme.colorScheme.tertiaryContainer
    val panelColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f)
    val panelOutline = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val currentScheduleBlockLabel = currentScheduleSnapshot.currentBlock?.label
    val nextScheduleBlockLabel = currentScheduleSnapshot.nextBlock?.label
    val blockPlanTarget = currentScheduleSnapshot.currentBlock ?: currentScheduleSnapshot.upcomingBlock
    val currentBlockPlan = blockPlanTarget?.let {
        findBlockPlan(focusBlockPlans, LocalDate.now(ZoneId.systemDefault()), it.label)
    }
    val actionableBlockPlan = currentBlockPlan?.takeUnless { it.doneForBlock }
    val blockPlanSuggestedApps by remember(blockPlanTarget, focusScheduleDockMappings, allApps) {
        derivedStateOf {
            resolveActiveDockApps(
                currentBlock = blockPlanTarget,
                mappings = focusScheduleDockMappings,
                apps = allApps,
            )
        }
    }
    val prepState = resolvePrepCard(
        currentBlock = currentScheduleSnapshot.currentBlock,
        nextBlock = currentScheduleSnapshot.nextBlock,
        minutesUntilCurrentBlockEnds = currentScheduleSnapshot.minutesUntilCurrentBlockEnds,
        leadMinutes = focusPrepLeadTimeMinutes,
    )
    val isAlignedWithCurrentBlock = currentScheduleBlockLabel?.let { currentBlockLabel ->
        focusScheduleDockMappings.firstOrNull {
            normalizeScheduleEventName(it.eventName) == normalizeScheduleEventName(currentBlockLabel)
        }?.appKeys?.contains(app.key) == true
    } == true
    val relatedRecoveryMicroStep = focusLastResumeContext?.takeIf {
        val contextBlockLabel = it.scheduleBlockLabel
        focusRecoveryFollowsCurrentBlockEnabled &&
            currentScheduleBlockLabel != null &&
            contextBlockLabel != null &&
            normalizeScheduleEventName(contextBlockLabel) == normalizeScheduleEventName(currentScheduleBlockLabel)
    }?.microStep
    val resolvedReasonPrefill = actionableBlockPlan?.intention
        ?.takeIf { it.isNotBlank() }
        .orEmpty()
    val resolvedMicroStepPrefill = actionableBlockPlan?.tinyStep
        ?.takeIf { it.isNotBlank() }
        ?: relatedRecoveryMicroStep.orEmpty()

    LaunchedEffect(currentBlockPlan?.tinyStep, currentBlockPlan?.intention, relatedRecoveryMicroStep) {
        if (microStep.isBlank()) {
            microStep = resolvedMicroStepPrefill
        }
        if (reason.isBlank()) {
            reason = resolvedReasonPrefill
        }
    }
    val panelGradient = remember(
        gradientTop,
        gradientMiddle,
        gradientBottom,
    ) {
        Brush.verticalGradient(
            colors = listOf(
                gradientTop,
                gradientMiddle,
                gradientBottom,
            )
        )
    }

    LaunchedEffect(app.key, profile, focusSessionActive) {
        decision = focusPolicyService.evaluate(app)
        sessionMinutes = resolveBlockAwareSessionMinutes(
            defaultMinutes = defaultSessionMinutes,
            capMinutes = capMinutes,
            minutesUntilCurrentBlockEnds = currentScheduleSnapshot.currentBlock?.let {
                currentScheduleSnapshot.minutesUntilCurrentBlockEnds
            },
            enabled = focusBlockAwareSessionSizingEnabled,
        )
        val policy = decision ?: return@LaunchedEffect
        if (!policy.requiresGate && !hasLaunched) {
            hasLaunched = true
            launchCoordinator.launchDirect(app, context)
            onFinish()
        }
    }

    BackHandler {
        onGoBack()
    }

    LaunchedEffect(decision?.effectiveDelaySeconds, decision?.hardBlocked) {
        val policy = decision ?: return@LaunchedEffect
        if (!policy.requiresGate) return@LaunchedEffect

        if (launchBounds != null) {
            stage = FocusGateStage.Entry
            entryProgress.snapTo(0f)
            entryProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            )
        } else {
            entryProgress.snapTo(1f)
        }

        stage = FocusGateStage.Animation
        reason = resolvedReasonPrefill
        microStep = resolvedMicroStepPrefill
        countdownSeconds = policy.effectiveDelaySeconds
        fillProgress.snapTo(0f)

        val totalDurationMs = (policy.effectiveDelaySeconds * 1000L).coerceAtLeast(900L)
        val expandDuration = (totalDurationMs * 0.65f).toInt()
        val settleDuration = (totalDurationMs - expandDuration).toInt()
        val startTime = System.currentTimeMillis()

        coroutineScope {
            launch {
                while (true) {
                    val elapsed = (System.currentTimeMillis() - startTime).coerceAtLeast(0L)
                    val remainingMs = (totalDurationMs - elapsed).coerceAtLeast(0L)
                    countdownSeconds = ((remainingMs + 999L) / 1000L).toInt()
                    if (remainingMs <= 0L) break
                    delay(100)
                }
            }
            launch {
                fillProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(expandDuration),
                )
                fillProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(settleDuration),
                )
            }
        }

        countdownSeconds = 0
        stage = if (policy.hardBlocked) FocusGateStage.Blocked else FocusGateStage.Intent
    }

    val message = when {
        decision?.appType == FocusAppType.Distracting &&
            decision?.hardBlocked == false &&
            decision?.habitBlocked == false &&
            currentScheduleBlockLabel != null &&
            isAlignedWithCurrentBlock ->
            stringResource(R.string.focus_gate_message_block_aligned, currentScheduleBlockLabel)
        decision?.appType == FocusAppType.Distracting &&
            decision?.hardBlocked == false &&
            decision?.habitBlocked == false &&
            currentScheduleBlockLabel != null ->
            stringResource(R.string.focus_gate_message_block_mismatch, currentScheduleBlockLabel)
        decision?.blockReason == FocusBlockReason.HabitDeadline ->
            decision?.blockingHabitTitle?.let {
                context.getString(R.string.focus_gate_message_habit_deadline_named, it)
            } ?: stringResource(R.string.focus_gate_message_habit_deadline)
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
    val scheduleSupportMessage = if (
        stage == FocusGateStage.Intent &&
        prepState.show &&
        nextScheduleBlockLabel != null
    ) {
        stringResource(
            R.string.focus_gate_message_transition_soon,
            nextScheduleBlockLabel,
            prepState.minutesUntilNextBlock ?: 0,
        )
    } else {
        null
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val startBounds = launchBounds

        if (stage == FocusGateStage.Entry && startBounds != null) {
            val startWidth = (startBounds.width.toFloat() / maxWidthPx).coerceAtLeast(0.05f)
            val startHeight = (startBounds.height.toFloat() / maxHeightPx).coerceAtLeast(0.05f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = startWidth + (1f - startWidth) * entryProgress.value
                        scaleY = startHeight + (1f - startHeight) * entryProgress.value
                        translationX = startBounds.left * (1f - entryProgress.value)
                        translationY = startBounds.top * (1f - entryProgress.value)
                    }
                    .clip(RoundedCornerShape(36.dp))
                    .background(panelGradient),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(fillProgress.value)
                .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                .background(panelGradient),
        )

        AnimatedContent(
            targetState = stage,
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.safeDrawing.asPaddingValues())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            transitionSpec = {
                (fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 8 } + scaleIn(tween(240), initialScale = 0.96f))
                    .togetherWith(fadeOut(tween(180)) + slideOutVertically(tween(180)) { -it / 12 } + scaleOut(tween(180), targetScale = 1.02f))
            }
        ) {
            stageValue ->
            when (stageValue) {
                FocusGateStage.Entry -> {
                    Spacer(modifier = Modifier.fillMaxSize())
                }
                FocusGateStage.Animation -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            Text(
                                text = app.labelOverride ?: app.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = secondaryText,
                            )
                            Text(
                                text = stringResource(R.string.focus_gate_breath_title),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryText,
                            )
                            Text(
                                text = if (countdownSeconds > 0) {
                                    stringResource(R.string.focus_gate_animation_countdown, countdownSeconds)
                                } else {
                                    stringResource(R.string.focus_gate_breath_subtitle)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = secondaryText,
                            )
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = panelColor.copy(alpha = 0.22f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, panelOutline),
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.focus_gate_breath_subtitle),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = primaryText,
                                )
                                OutlinedButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = onGoBack,
                                ) {
                                    Text(stringResource(R.string.focus_gate_go_back))
                                }
                            }
                        }
                    }
                }

                FocusGateStage.Intent,
                FocusGateStage.Blocked,
                -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Spacer(modifier = Modifier.height(1.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = panelColor,
                            border = androidx.compose.foundation.BorderStroke(1.dp, panelOutline),
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp),
                            ) {
                                ShapedLauncherIcon(
                                    size = 72.dp,
                                    icon = { icon },
                                )

                                Text(
                                    text = app.labelOverride ?: app.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = if (stageValue == FocusGateStage.Blocked) {
                                        stringResource(R.string.focus_gate_blocked_title)
                                    } else {
                                        stringResource(R.string.focus_gate_intentional_title)
                                    },
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                scheduleSupportMessage?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                }

                                if (stageValue == FocusGateStage.Intent) {
                                    if (startRitualEnabled) {
                                        OutlinedTextField(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(140.dp),
                                            value = reason,
                                            onValueChange = { reason = it },
                                            label = {
                                                Text(stringResource(R.string.focus_gate_reason_label))
                                            },
                                            placeholder = {
                                                Text(
                                                    if (currentScheduleBlockLabel != null) {
                                                        stringResource(
                                                            R.string.focus_gate_reason_placeholder_block,
                                                            currentScheduleBlockLabel,
                                                        )
                                                    } else if (blockPlanTarget != null) {
                                                        stringResource(
                                                            R.string.focus_gate_reason_placeholder_block,
                                                            blockPlanTarget.label,
                                                        )
                                                    } else {
                                                        stringResource(R.string.focus_gate_reason_placeholder)
                                                    }
                                                )
                                            },
                                            textStyle = MaterialTheme.typography.bodyLarge,
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                            minLines = 2,
                                            maxLines = 3,
                                        )

                                        if (microStepPromptEnabled) {
                                            OutlinedTextField(
                                                modifier = Modifier.fillMaxWidth(),
                                                value = microStep,
                                                onValueChange = { microStep = it },
                                                label = {
                                                    Text(stringResource(R.string.focus_gate_micro_step_label))
                                                },
                                                placeholder = {
                                                    Text(
                                                        relatedRecoveryMicroStep
                                                            ?: stringResource(R.string.focus_gate_micro_step_placeholder)
                                                    )
                                                },
                                                textStyle = MaterialTheme.typography.bodyLarge,
                                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                                minLines = 1,
                                                maxLines = 2,
                                            )
                                        }
                                    }

                                    FilledTonalButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            val maxSessionMinutes = if (focusBlockAwareSessionSizingEnabled) {
                                                capMinutes.coerceAtMost(
                                                    currentScheduleSnapshot.currentBlock?.let {
                                                        currentScheduleSnapshot.minutesUntilCurrentBlockEnds
                                                    } ?: capMinutes
                                                )
                                            } else {
                                                capMinutes
                                            }.coerceAtLeast(1)
                                            val durationOptions = listOf(5, 10, 15, 30).filter { it <= maxSessionMinutes }
                                                .ifEmpty { listOf(maxSessionMinutes) }
                                            val currentIndex = durationOptions.indexOf(sessionMinutes)
                                            sessionMinutes = durationOptions.getOrElse(currentIndex + 1) {
                                                durationOptions.first()
                                            }
                                        },
                                    ) {
                                        Text(stringResource(R.string.focus_gate_session_static_value, sessionMinutes))
                                    }

                                    if (blockPlanTarget != null) {
                                        OutlinedButton(
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = { showBlockSetupSheet = true },
                                        ) {
                                            Text(stringResource(R.string.focus_home_daily_schedule_setup))
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    if (stageValue == FocusGateStage.Intent) {
                                        Button(
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = !startRitualEnabled || (
                                                reason.isNotBlank() &&
                                                    (!microStepPromptEnabled || microStep.isNotBlank())
                                                ),
                                            onClick = {
                                                customAttributesRepository.setFocusProfile(
                                                    app,
                                                    profile.withTemporaryUnlockUntil(
                                                        System.currentTimeMillis() + sessionMinutes * 60_000L
                                                    ),
                                                )
                                                searchUiSettings.setFocusLastResumeContext(
                                                    FocusResumeContext(
                                                        taskLabel = app.labelOverride ?: app.label,
                                                        scheduleBlockLabel = currentScheduleBlockLabel,
                                                        microStep = microStep.takeIf { it.isNotBlank() },
                                                        appKey = app.key,
                                                        interruptedAtMillis = System.currentTimeMillis(),
                                                    )
                                                )
                                                kotlinx.coroutines.runBlocking {
                                                    historyRepository.logEvent(
                                                        FocusLogEvent(
                                                            appKey = app.key,
                                                            appLabel = app.labelOverride ?: app.label,
                                                            reason = reason,
                                                            scheduleBlockLabel = currentScheduleBlockLabel,
                                                            microStep = microStep.takeIf { it.isNotBlank() },
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
                                                onFinish()
                                            },
                                        ) {
                                            Text(stringResource(R.string.focus_gate_continue))
                                        }
                                    }

                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = onGoBack,
                                    ) {
                                        Text(stringResource(R.string.focus_gate_go_back))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBlockSetupSheet && blockPlanTarget != null) {
        FocusBlockSetupSheet(
            expanded = true,
            block = blockPlanTarget,
            date = LocalDate.now(ZoneId.systemDefault()),
            existingPlan = currentBlockPlan,
            suggestedApps = blockPlanSuggestedApps,
            onDismissRequest = { showBlockSetupSheet = false },
            onSave = {
                reason = it.intention
                microStep = it.tinyStep.takeIf { step -> step.isNotBlank() } ?: relatedRecoveryMicroStep.orEmpty()
                searchUiSettings.upsertFocusBlockPlan(it)
            },
        )
    }
}
