package de.mm20.launcher2.services.focus

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

private fun FocusBlockPlan.matches(date: LocalDate, normalizedBlockLabel: String): Boolean {
    return this.date == date.toString() && this.normalizedBlockLabel == normalizedBlockLabel
}

private fun FocusBlockPlan.matches(date: String, normalizedBlockLabel: String): Boolean {
    return this.date == date && this.normalizedBlockLabel == normalizedBlockLabel
}

fun findBlockPlan(
    plans: List<FocusBlockPlan>,
    date: LocalDate,
    blockLabel: String,
): FocusBlockPlan? {
    val normalizedLabel = normalizeScheduleEventName(blockLabel)
    return plans
        .asSequence()
        .filter { it.matches(date, normalizedLabel) }
        .maxByOrNull { it.lastUpdatedAtMillis }
}

fun isBlockPlanStale(
    plan: FocusBlockPlan,
    date: LocalDate,
    blockLabel: String,
): Boolean {
    return !plan.matches(date, normalizeScheduleEventName(blockLabel))
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
    return plans.filterNot { it.matches(plan.date, plan.normalizedBlockLabel) } + plan
}
