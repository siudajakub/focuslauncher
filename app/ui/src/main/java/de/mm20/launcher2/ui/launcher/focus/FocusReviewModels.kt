package de.mm20.launcher2.ui.launcher.focus

typealias FocusExperiment = de.mm20.launcher2.preferences.FocusExperiment
typealias FocusExperimentKind = de.mm20.launcher2.preferences.FocusExperimentKind

enum class FocusRecommendationKind {
    IncreaseFriction,
    ShorterUnlockCap,
    EnablePrepPrompts,
    AdjustPrepLeadTime,
    MoveHabitEarlier,
}

data class FocusReviewInputs(
    val topBreakingAppKey: String? = null,
    val topBreakingAppLabel: String? = null,
    val repeatedMismatchCount: Int = 0,
    val prepPromptEnabled: Boolean = true,
    val prepLeadMinutes: Int = 10,
    val overdueHabitTitle: String? = null,
    val overdueHabitCount: Int = 0,
)

data class FocusRecommendation(
    val key: String,
    val kind: FocusRecommendationKind,
    val title: String,
    val summary: String,
    val priority: Int,
)

fun resolveRecommendations(
    inputs: FocusReviewInputs,
    dismissedKeys: Set<String>,
    limit: Int = 2,
): List<FocusRecommendation> {
    val recommendations = buildList {
        if (!inputs.prepPromptEnabled && inputs.repeatedMismatchCount >= 3) {
            add(
                FocusRecommendation(
                    key = "enable-prep-prompts",
                    kind = FocusRecommendationKind.EnablePrepPrompts,
                    title = "Enable prep prompts",
                    summary = "Prep prompts can reduce rough starts before demanding blocks.",
                    priority = 100,
                )
            )
        }
        inputs.topBreakingAppKey?.let { appKey ->
            if (inputs.repeatedMismatchCount >= 4) {
                add(
                    FocusRecommendation(
                        key = "increase-friction:$appKey",
                        kind = FocusRecommendationKind.IncreaseFriction,
                        title = "Increase friction for ${inputs.topBreakingAppLabel ?: "this app"}",
                        summary = "This app breaks focus often during mismatched launches.",
                        priority = 90,
                    )
                )
            }
            if (inputs.repeatedMismatchCount >= 6) {
                add(
                    FocusRecommendation(
                        key = "shorter-unlock-cap:$appKey",
                        kind = FocusRecommendationKind.ShorterUnlockCap,
                        title = "Shorten unlock cap for ${inputs.topBreakingAppLabel ?: "this app"}",
                        summary = "A smaller default unlock can make detours less sticky.",
                        priority = 70,
                    )
                )
            }
        }
        if (inputs.prepPromptEnabled && inputs.prepLeadMinutes < 10 && inputs.repeatedMismatchCount >= 5) {
            add(
                FocusRecommendation(
                    key = "adjust-prep-lead:${inputs.prepLeadMinutes}",
                    kind = FocusRecommendationKind.AdjustPrepLeadTime,
                    title = "Increase prep lead time",
                    summary = "A slightly earlier prep window may make transitions smoother.",
                    priority = 80,
                )
            )
        }
        inputs.overdueHabitTitle?.let { title ->
            if (inputs.overdueHabitCount > 0) {
                add(
                    FocusRecommendation(
                        key = "move-habit-earlier:$title",
                        kind = FocusRecommendationKind.MoveHabitEarlier,
                        title = "Move $title earlier",
                        summary = "This habit is often late before focus blocks begin.",
                        priority = 75,
                    )
                )
            }
        }
    }
    return recommendations
        .filterNot { it.key in dismissedKeys }
        .sortedByDescending { it.priority }
        .take(limit.coerceAtLeast(0))
}
