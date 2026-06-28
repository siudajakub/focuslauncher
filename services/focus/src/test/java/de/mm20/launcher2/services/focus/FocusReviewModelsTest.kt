package de.mm20.launcher2.services.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusReviewModelsTest {
    @Test
    fun `recommendations stay sparse and deterministic`() {
        val recommendations = resolveRecommendations(
            inputs = FocusReviewInputs(
                topBreakingAppKey = "pkg.video/.Main",
                topBreakingAppLabel = "Video",
                repeatedMismatchCount = 8,
                prepPromptEnabled = false,
                prepLeadMinutes = 5,
                overdueHabitTitle = "Medication",
                overdueHabitCount = 3,
            ),
            dismissedKeys = emptySet(),
            limit = 2,
        )

        assertEquals(2, recommendations.size)
        assertEquals(FocusRecommendationKind.EnablePrepPrompts, recommendations.first().kind)
    }

    @Test
    fun `recommendations skip dismissed items`() {
        val recommendations = resolveRecommendations(
            inputs = FocusReviewInputs(
                topBreakingAppKey = "pkg.video/.Main",
                topBreakingAppLabel = "Video",
                repeatedMismatchCount = 8,
                prepPromptEnabled = true,
                prepLeadMinutes = 5,
            ),
            dismissedKeys = setOf("increase-friction:pkg.video/.Main"),
            limit = 2,
        )

        assertTrue(recommendations.none { it.key == "increase-friction:pkg.video/.Main" })
    }
}
