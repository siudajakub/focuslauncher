package de.mm20.launcher2.ui.launcher.focus

import java.time.LocalDate

typealias FocusBlockPlan = de.mm20.launcher2.preferences.FocusBlockPlan
typealias FocusReadinessCheck = de.mm20.launcher2.preferences.FocusReadinessCheck
typealias FocusReadinessSource = de.mm20.launcher2.preferences.FocusReadinessSource

enum class FocusBlockReadiness {
    MissingPlan,
    NeedsPrep,
    MissingNextStep,
    Ready,
    DoneForBlock,
}

fun resolveBlockPlanKey(
    date: LocalDate,
    blockLabel: String,
): String {
    return "${date}::${normalizeScheduleEventName(blockLabel)}"
}

fun findBlockPlan(
    plans: List<FocusBlockPlan>,
    date: LocalDate,
    blockLabel: String,
): FocusBlockPlan? {
    val normalizedLabel = normalizeScheduleEventName(blockLabel)
    return plans.lastOrNull {
        it.date == date.toString() && it.normalizedBlockLabel == normalizedLabel
    }
}

fun isBlockPlanStale(
    plan: FocusBlockPlan,
    date: LocalDate,
    blockLabel: String,
): Boolean {
    return plan.date != date.toString() || plan.normalizedBlockLabel != normalizeScheduleEventName(blockLabel)
}

fun resolveBlockReadiness(
    plan: FocusBlockPlan?,
    habitsSatisfied: Boolean,
    prepSatisfied: Boolean,
): FocusBlockReadiness {
    if (plan == null) return FocusBlockReadiness.MissingPlan
    if (plan.doneForBlock) return FocusBlockReadiness.DoneForBlock
    if (plan.tinyStep.isBlank()) return FocusBlockReadiness.MissingNextStep
    if (!habitsSatisfied || !prepSatisfied) return FocusBlockReadiness.NeedsPrep
    return FocusBlockReadiness.Ready
}

fun upsertBlockPlan(
    plans: List<FocusBlockPlan>,
    plan: FocusBlockPlan,
): List<FocusBlockPlan> {
    return plans.filterNot {
        it.date == plan.date && it.normalizedBlockLabel == plan.normalizedBlockLabel
    } + plan
}
