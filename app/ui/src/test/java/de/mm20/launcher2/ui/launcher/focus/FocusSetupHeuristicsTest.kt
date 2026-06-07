package de.mm20.launcher2.ui.launcher.focus

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusSetupHeuristicsTest {
    @Test
    fun `quick start is shown when no focus apps are configured`() {
        assertTrue(
            shouldShowFocusQuickStart(
                focusModeEnabled = true,
                essentialCount = 0,
                distractingCount = 0,
                dailyScheduleEnabled = false,
                habitsEnabled = false,
            )
        )
    }

    @Test
    fun `quick start is hidden when setup already has apps and at least one daily system`() {
        assertFalse(
            shouldShowFocusQuickStart(
                focusModeEnabled = true,
                essentialCount = 2,
                distractingCount = 3,
                dailyScheduleEnabled = true,
                habitsEnabled = false,
            )
        )
    }

    @Test
    fun `quick start is hidden when focus mode is disabled and apps exist`() {
        assertFalse(
            shouldShowFocusQuickStart(
                focusModeEnabled = false,
                essentialCount = 2,
                distractingCount = 0,
                dailyScheduleEnabled = false,
                habitsEnabled = false,
            )
        )
    }

    @Test
    fun `quick start is shown when focus is enabled but no daily system is set`() {
        assertTrue(
            shouldShowFocusQuickStart(
                focusModeEnabled = true,
                essentialCount = 2,
                distractingCount = 0,
                dailyScheduleEnabled = false,
                habitsEnabled = false,
            )
        )
    }

    @Test
    fun `quick start is hidden when focus is enabled and habits are enabled`() {
        assertFalse(
            shouldShowFocusQuickStart(
                focusModeEnabled = true,
                essentialCount = 2,
                distractingCount = 0,
                dailyScheduleEnabled = false,
                habitsEnabled = true,
            )
        )
    }
}
