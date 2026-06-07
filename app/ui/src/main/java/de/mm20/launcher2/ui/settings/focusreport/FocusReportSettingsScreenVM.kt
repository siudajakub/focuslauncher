package de.mm20.launcher2.ui.settings.focusreport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.mm20.launcher2.applications.AppRepository
import de.mm20.launcher2.data.customattrs.CustomAttributesRepository
import de.mm20.launcher2.preferences.FocusHabit
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import de.mm20.launcher2.search.Application
import de.mm20.launcher2.ui.launcher.focus.FocusHistoryRepository
import de.mm20.launcher2.ui.launcher.focus.FocusRecommendation
import de.mm20.launcher2.ui.launcher.focus.FocusRecommendationKind
import de.mm20.launcher2.ui.launcher.focus.FocusReviewInputs
import de.mm20.launcher2.ui.launcher.focus.WeeklyFocusReport
import de.mm20.launcher2.ui.launcher.focus.resolveRecommendations
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FocusReportSettingsScreenVM : ViewModel(), KoinComponent {
    private val repository = FocusHistoryRepository()
    private val searchUiSettings: SearchUiSettings by inject()
    private val customAttributesRepository: CustomAttributesRepository by inject()
    private val appRepository: AppRepository by inject()

    private val actionSummary = MutableStateFlow<String?>(null)
    val lastActionSummary = actionSummary

    val report = repository.getWeeklyReport()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), WeeklyFocusReport())

    private val apps = appRepository.findMany()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), persistentListOf())

    val recommendations = combine(
        report,
        searchUiSettings.focusDismissedRecommendationKeys,
        searchUiSettings.focusBlockPrepPromptsEnabled,
        searchUiSettings.focusPrepLeadTimeMinutes,
    ) { report, dismissedKeys, prepEnabled, prepLead ->
        resolveRecommendations(
            inputs = report.toReviewInputs(
                prepPromptEnabled = prepEnabled,
                prepLeadMinutes = prepLead,
            ),
            dismissedKeys = dismissedKeys,
            limit = 3,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun dismissRecommendation(key: String) {
        searchUiSettings.dismissFocusRecommendation(key)
        actionSummary.value = de.mm20.launcher2.ui.R.string.focus_report_action_dismissed.toString()
    }

    fun applyRecommendation(recommendation: FocusRecommendation) {
        viewModelScope.launch {
            when (recommendation.kind) {
                FocusRecommendationKind.EnablePrepPrompts -> {
                    searchUiSettings.setFocusBlockPrepPromptsEnabled(true)
                    searchUiSettings.restoreFocusRecommendation(recommendation.key)
                    actionSummary.value = de.mm20.launcher2.ui.R.string.focus_report_action_prep_enabled.toString()
                }

                FocusRecommendationKind.AdjustPrepLeadTime -> {
                    val current = searchUiSettings.focusPrepLeadTimeMinutes.first()
                    val updated = (current + 5).coerceAtMost(30)
                    searchUiSettings.setFocusPrepLeadTimeMinutes(updated)
                    searchUiSettings.restoreFocusRecommendation(recommendation.key)
                    actionSummary.value = "prep:$updated"
                }

                FocusRecommendationKind.MoveHabitEarlier -> {
                    val inputs = report.value.toReviewInputs(
                        prepPromptEnabled = searchUiSettings.focusBlockPrepPromptsEnabled.first(),
                        prepLeadMinutes = searchUiSettings.focusPrepLeadTimeMinutes.first(),
                    )
                    val overdueTitle = inputs.overdueHabitTitle
                    val habits = searchUiSettings.focusHabits.first()
                    val habit = habits.firstOrNull { it.title == overdueTitle }
                    if (habit == null) {
                        actionSummary.value = de.mm20.launcher2.ui.R.string.focus_report_action_missing_habit.toString()
                    } else {
                        searchUiSettings.upsertFocusHabit(habit.moveEarlier())
                        searchUiSettings.restoreFocusRecommendation(recommendation.key)
                        actionSummary.value = "habit:${habit.title}"
                    }
                }
            }
        }
    }

    private fun findTopBreakerApp(report: WeeklyFocusReport): Application? {
        val appKey = report.recentEvents
            .groupingBy { it.appKey }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?: return null
        return apps.value.firstOrNull { it.key == appKey }
    }
}

private fun WeeklyFocusReport.toReviewInputs(
    prepPromptEnabled: Boolean,
    prepLeadMinutes: Int,
): FocusReviewInputs {
    val topBreaker = topFocusBreakers.firstOrNull()
    val topOverdueHabit = topUnlockReasons
        .firstOrNull { (reason, _) -> reason.startsWith("habit:", ignoreCase = true) }
        ?.let { (reason, count) ->
            reason.removePrefix("habit:").trim().ifBlank { null }?.let { it to count }
        }
    val topBreakerAppKey = recentEvents
        .groupingBy { it.appKey }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
    val topBreakerLabel = topBreakerAppKey?.let { key ->
        recentEvents.firstOrNull { it.appKey == key }?.appLabel
    } ?: topFocusBreakers.firstOrNull()?.first
    return FocusReviewInputs(
        topBreakingAppKey = topBreakerAppKey,
        topBreakingAppLabel = topBreakerLabel,
        repeatedMismatchCount = scheduledBlockUnlocks + inSessionUnlocks + recoveryDismissedCount,
        prepPromptEnabled = prepPromptEnabled,
        prepLeadMinutes = prepLeadMinutes,
        overdueHabitTitle = topOverdueHabit?.first,
        overdueHabitCount = topOverdueHabit?.second ?: 0,
    )
}

private fun FocusHabit.moveEarlier(): FocusHabit {
    return copy(deadlineMinutes = (deadlineMinutes - 30).coerceAtLeast(0))
}
