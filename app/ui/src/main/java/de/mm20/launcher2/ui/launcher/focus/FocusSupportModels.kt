package de.mm20.launcher2.ui.launcher.focus

import de.mm20.launcher2.preferences.FocusHabit
import de.mm20.launcher2.preferences.FocusBlockPlan
import de.mm20.launcher2.preferences.FocusResumeContext
import de.mm20.launcher2.preferences.ScheduleDockMapping
import de.mm20.launcher2.search.Application
import de.mm20.launcher2.search.CalendarEvent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private val WHITESPACE_REGEX = Regex("\\s+")

data class DailyScheduleBlock(
    val label: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
)

data class DailyScheduleSnapshot(
    val currentBlock: DailyScheduleBlock? = null,
    val nextBlock: DailyScheduleBlock? = null,
    val upcomingBlock: DailyScheduleBlock? = null,
    val minutesUntilCurrentBlockEnds: Int = 0,
    val minutesUntilUpcomingBlockStarts: Int = 0,
)

data class HabitStatus(
    val habit: FocusHabit,
    val completed: Boolean,
    val overdue: Boolean,
)

data class HabitGateState(
    val blocked: Boolean,
    val overdueCount: Int,
    val primaryOverdueHabitTitle: String? = null,
)

data class PrepCardState(
    val show: Boolean,
    val nextBlockLabel: String? = null,
    val minutesUntilNextBlock: Int? = null,
)

enum class FocusGuidanceType {
    None,
    Recover,
    Prep,
    Ready,
    Now,
}

data class FocusGuidanceState(
    val type: FocusGuidanceType,
    val blockLabel: String? = null,
    val nextBlockLabel: String? = null,
    val minutesRemaining: Int? = null,
    val suggestedMicroStep: String? = null,
    val intention: String? = null,
    val taskLabel: String? = null,
    val completedForBlock: Boolean = false,
    val resumeMatchesCurrentBlock: Boolean = false,
    val requiresSetup: Boolean = false,
)

fun normalizeScheduleEventName(name: String): String {
    return name.trim().lowercase().replace(WHITESPACE_REGEX, " ")
}

fun resolveDailyScheduleSnapshot(
    events: List<DailyScheduleBlock>,
    nowMillis: Long,
): DailyScheduleSnapshot {
    val sorted = events.sortedBy { it.startTimeMillis }
    val current = sorted.firstOrNull { nowMillis >= it.startTimeMillis && nowMillis < it.endTimeMillis }
    val next = sorted.firstOrNull { it.startTimeMillis > (current?.endTimeMillis ?: nowMillis) }
    val upcoming = if (current == null) sorted.firstOrNull { it.startTimeMillis > nowMillis } else null
    return DailyScheduleSnapshot(
        currentBlock = current,
        nextBlock = if (current != null) next else null,
        upcomingBlock = upcoming,
        minutesUntilCurrentBlockEnds = current?.let {
            ((it.endTimeMillis - nowMillis).coerceAtLeast(0L) / 60_000L).toInt()
        } ?: 0,
        minutesUntilUpcomingBlockStarts = upcoming?.let {
            ((it.startTimeMillis - nowMillis).coerceAtLeast(0L) / 60_000L).toInt()
        } ?: 0,
    )
}

fun resolveHabitStatuses(
    habits: List<FocusHabit>,
    today: LocalDate,
    now: LocalDateTime,
): List<HabitStatus> {
    val currentMinutes = now.hour * 60 + now.minute
    return habits.map { habit ->
        val completed = habit.completedDates.contains(today.toString())
        HabitStatus(
            habit = habit,
            completed = completed,
            overdue = !completed && currentMinutes >= habit.deadlineMinutes,
        )
    }.sortedWith(compareByDescending<HabitStatus> { it.overdue }.thenBy { it.habit.deadlineMinutes })
}

fun resolveHabitGate(
    habits: List<FocusHabit>,
    today: LocalDate = LocalDate.now(),
    now: LocalDateTime = LocalDateTime.now(),
): HabitGateState {
    val overdue = resolveHabitStatuses(habits, today, now).filter { it.overdue }
    return HabitGateState(
        blocked = overdue.isNotEmpty(),
        overdueCount = overdue.size,
        primaryOverdueHabitTitle = overdue.firstOrNull()?.habit?.title,
    )
}

fun CalendarEvent.toDailyScheduleBlock(): DailyScheduleBlock {
    return DailyScheduleBlock(
        label = labelOverride ?: label,
        startTimeMillis = startTime ?: endTime,
        endTimeMillis = endTime,
    )
}

fun resolveActiveDockApps(
    currentBlock: DailyScheduleBlock?,
    mappings: List<ScheduleDockMapping>,
    apps: List<Application>,
): List<Application> {
    val blockName = currentBlock?.label?.let(::normalizeScheduleEventName) ?: return emptyList()
    val mapping = mappings.firstOrNull { normalizeScheduleEventName(it.eventName) == blockName } ?: return emptyList()
    val appMap = apps.associateBy { it.key }
    return mapping.appKeys.mapNotNull(appMap::get)
}

fun resolvePreferredDockApps(
    currentBlock: DailyScheduleBlock?,
    blockPlan: FocusBlockPlan?,
    mappings: List<ScheduleDockMapping>,
    apps: List<Application>,
): List<Application> {
    val appMap = apps.associateBy { it.key }
    val planApps = blockPlan?.recommendedAppKeys?.mapNotNull(appMap::get).orEmpty()
    if (planApps.isNotEmpty()) return planApps
    return resolveActiveDockApps(currentBlock, mappings, apps)
}

fun Long.toLocalDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate {
    return java.time.Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
}

fun resolvePrepCard(
    currentBlock: DailyScheduleBlock?,
    nextBlock: DailyScheduleBlock?,
    minutesUntilCurrentBlockEnds: Int,
    leadMinutes: Int,
): PrepCardState {
    if (currentBlock == null || nextBlock == null) {
        return PrepCardState(show = false)
    }
    val show = minutesUntilCurrentBlockEnds in 0..leadMinutes
    return PrepCardState(
        show = show,
        nextBlockLabel = nextBlock.label,
        minutesUntilNextBlock = minutesUntilCurrentBlockEnds.takeIf { show },
    )
}

fun resolveScheduleAwareResumeCard(
    lastContext: FocusResumeContext?,
    currentBlockLabel: String?,
    nowMillis: Long,
    expiryMillis: Long,
    followsCurrentBlock: Boolean,
): ResumeCardState {
    val base = resolveResumeCard(
        lastContext = lastContext,
        nowMillis = nowMillis,
        expiryMillis = expiryMillis,
    )
    if (!base.show || lastContext == null) {
        return base
    }
    val lastBlockLabel = lastContext.scheduleBlockLabel
    if (
        followsCurrentBlock &&
        lastBlockLabel != null &&
        currentBlockLabel != null &&
        normalizeScheduleEventName(lastBlockLabel) != normalizeScheduleEventName(currentBlockLabel)
    ) {
        return ResumeCardState(show = false)
    }
    return base.copy(
        taskLabel = if (followsCurrentBlock) {
            currentBlockLabel ?: lastContext.scheduleBlockLabel ?: lastContext.taskLabel
        } else {
            lastContext.taskLabel
        },
        matchesCurrentBlock = followsCurrentBlock &&
            lastBlockLabel != null &&
            currentBlockLabel != null &&
            normalizeScheduleEventName(lastBlockLabel) == normalizeScheduleEventName(currentBlockLabel),
    )
}

fun resolveFocusGuidance(
    currentBlock: DailyScheduleBlock?,
    prepState: PrepCardState,
    resumeState: ResumeCardState,
    blockPlan: FocusBlockPlan? = null,
    blockReadiness: FocusBlockReadiness = FocusBlockReadiness.MissingPlan,
    guidanceBlock: DailyScheduleBlock? = currentBlock,
): FocusGuidanceState {
    return when {
        resumeState.show -> FocusGuidanceState(
            type = FocusGuidanceType.Recover,
            blockLabel = resumeState.relatedBlockLabel,
            taskLabel = resumeState.taskLabel,
            suggestedMicroStep = resumeState.microStep,
            resumeMatchesCurrentBlock = resumeState.matchesCurrentBlock,
        )

        prepState.show -> FocusGuidanceState(
            type = FocusGuidanceType.Prep,
            nextBlockLabel = prepState.nextBlockLabel,
            minutesRemaining = prepState.minutesUntilNextBlock,
        )

        blockPlan != null && blockReadiness == FocusBlockReadiness.DoneForBlock -> FocusGuidanceState(
            type = FocusGuidanceType.Now,
            blockLabel = guidanceBlock?.label ?: blockPlan.blockLabel,
            intention = blockPlan.intention.takeIf { it.isNotBlank() },
            completedForBlock = true,
        )

        blockPlan != null && blockReadiness == FocusBlockReadiness.Ready -> FocusGuidanceState(
            type = FocusGuidanceType.Ready,
            blockLabel = guidanceBlock?.label ?: blockPlan.blockLabel,
            suggestedMicroStep = blockPlan.tinyStep.takeIf { it.isNotBlank() },
            intention = blockPlan.intention.takeIf { it.isNotBlank() },
        )

        currentBlock != null -> FocusGuidanceState(
            type = FocusGuidanceType.Now,
            blockLabel = currentBlock.label,
            requiresSetup = blockPlan == null,
        )

        else -> FocusGuidanceState(type = FocusGuidanceType.None)
    }
}

fun resolveBlockAwareSessionMinutes(
    defaultMinutes: Int,
    capMinutes: Int,
    minutesUntilCurrentBlockEnds: Int?,
    enabled: Boolean,
): Int {
    val cappedDefault = defaultMinutes.coerceAtMost(capMinutes)
    if (!enabled || minutesUntilCurrentBlockEnds == null) {
        return cappedDefault
    }
    return cappedDefault.coerceAtMost(minutesUntilCurrentBlockEnds.coerceAtLeast(1))
}
