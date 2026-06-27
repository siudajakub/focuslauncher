package de.mm20.launcher2.ui.launcher.focus

import androidx.annotation.StringRes
import de.mm20.launcher2.ui.R

typealias FocusExperiment = de.mm20.launcher2.preferences.FocusExperiment
typealias FocusExperimentKind = de.mm20.launcher2.preferences.FocusExperimentKind

enum class FocusRecommendationKind {
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
    @StringRes val titleRes: Int,
    val titleArgs: List<Any> = emptyList(),
    @StringRes val summaryRes: Int? = null,
    val summaryArgs: List<Any> = emptyList(),
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
                    titleRes = R.string.focus_recommendation_enable_prep_title,
                    summaryRes = R.string.focus_recommendation_enable_prep_summary,
                    priority = 100,
                )
            )
        }
        if (inputs.prepPromptEnabled && inputs.prepLeadMinutes < 30 && inputs.repeatedMismatchCount >= 5) {
            add(
                FocusRecommendation(
                    key = "adjust-prep-lead:${inputs.prepLeadMinutes}",
                    kind = FocusRecommendationKind.AdjustPrepLeadTime,
                    titleRes = R.string.focus_recommendation_adjust_lead_title,
                    summaryRes = R.string.focus_recommendation_adjust_lead_summary,
                    priority = 80,
                )
            )
        }
        inputs.overdueHabitTitle?.let { title ->
            if (inputs.overdueHabitCount >= 3) {
                add(
                    FocusRecommendation(
                        key = "move-habit-earlier:$title",
                        kind = FocusRecommendationKind.MoveHabitEarlier,
                        titleRes = R.string.focus_recommendation_move_habit_title,
                        titleArgs = listOf(title),
                        summaryRes = R.string.focus_recommendation_move_habit_summary,
                        priority = 90,
                    )
                )
            }
        }
    }
    return recommendations
        .filter { it.key !in dismissedKeys }
        .sortedByDescending { it.priority }
        .take(limit)
}
