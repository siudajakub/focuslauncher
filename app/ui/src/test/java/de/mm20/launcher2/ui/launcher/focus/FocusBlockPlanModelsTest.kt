package de.mm20.launcher2.ui.launcher.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FocusBlockPlanModelsTest {
    @Test
    fun `resolve block plan key normalizes block label`() {
        val key = resolveBlockPlanKey(
            date = LocalDate.of(2026, 4, 3),
            blockLabel = "  Deep   Work  ",
        )

        assertEquals("2026-04-03::deep work", key)
    }

    @Test
    fun `find block plan only matches same day and normalized label`() {
        val matchingPlan = FocusBlockPlan(
            date = "2026-04-03",
            normalizedBlockLabel = "deep work",
            blockLabel = "Deep Work",
            tinyStep = "Open the draft",
        )

        val otherDayPlan = matchingPlan.copy(date = "2026-04-04")

        val found = findBlockPlan(
            plans = listOf(otherDayPlan, matchingPlan),
            date = LocalDate.of(2026, 4, 3),
            blockLabel = " deep   work ",
        )

        assertEquals(matchingPlan, found)
    }

    @Test
    fun `find block plan returns null for renamed block`() {
        val found = findBlockPlan(
            plans = listOf(
                FocusBlockPlan(
                    date = "2026-04-03",
                    normalizedBlockLabel = "writing",
                    blockLabel = "Writing",
                    tinyStep = "Open doc",
                )
            ),
            date = LocalDate.of(2026, 4, 3),
            blockLabel = "Admin",
        )

        assertNull(found)
    }

    @Test
    fun `find block plan prefers the latest matching update for the same day`() {
        val olderPlan = FocusBlockPlan(
            date = "2026-04-03",
            normalizedBlockLabel = "writing",
            blockLabel = "Writing",
            tinyStep = "Open doc",
            lastUpdatedAtMillis = 100L,
        )
        val newerPlan = olderPlan.copy(
            tinyStep = "Write the first paragraph",
            lastUpdatedAtMillis = 200L,
        )

        val found = findBlockPlan(
            plans = listOf(olderPlan, newerPlan),
            date = LocalDate.of(2026, 4, 3),
            blockLabel = "Writing",
        )

        assertEquals(newerPlan, found)
    }

    @Test
    fun `upsert block plan replaces the existing same day same block entry`() {
        val original = FocusBlockPlan(
            date = "2026-04-03",
            normalizedBlockLabel = "writing",
            blockLabel = "Writing",
            tinyStep = "Open doc",
        )
        val updated = original.copy(
            tinyStep = "Draft intro",
            lastUpdatedAtMillis = 42L,
        )

        val plans = upsertBlockPlan(listOf(original), updated)

        assertEquals(listOf(updated), plans)
    }

    @Test
    fun `is block plan stale when date or normalized label changes`() {
        val plan = FocusBlockPlan(
            date = "2026-04-03",
            normalizedBlockLabel = "writing",
            blockLabel = "Writing",
        )

        assertTrue(isBlockPlanStale(plan, LocalDate.of(2026, 4, 4), "Writing"))
        assertTrue(isBlockPlanStale(plan, LocalDate.of(2026, 4, 3), "Admin"))
        assertFalse(isBlockPlanStale(plan, LocalDate.of(2026, 4, 3), " Writing "))
    }

    @Test
    fun `resolve block readiness returns ready only when prep is complete and tiny step exists`() {
        val readiness = resolveBlockReadiness(
            plan = FocusBlockPlan(
                date = "2026-04-03",
                normalizedBlockLabel = "writing",
                blockLabel = "Writing",
                tinyStep = "Write the first paragraph",
            ),
            habitsSatisfied = true,
            prepSatisfied = true,
        )

        assertEquals(FocusBlockReadiness.Ready, readiness)
    }

    @Test
    fun `resolve block readiness returns missing next step when plan has no tiny step`() {
        val readiness = resolveBlockReadiness(
            plan = FocusBlockPlan(
                date = "2026-04-03",
                normalizedBlockLabel = "writing",
                blockLabel = "Writing",
            ),
            habitsSatisfied = true,
            prepSatisfied = true,
        )

        assertEquals(FocusBlockReadiness.MissingNextStep, readiness)
    }

    @Test
    fun `resolve block readiness returns done for block before other checks`() {
        val readiness = resolveBlockReadiness(
            plan = FocusBlockPlan(
                date = "2026-04-03",
                normalizedBlockLabel = "writing",
                blockLabel = "Writing",
                tinyStep = "",
                doneForBlock = true,
            ),
            habitsSatisfied = false,
            prepSatisfied = false,
        )

        assertEquals(FocusBlockReadiness.DoneForBlock, readiness)
    }

    @Test
    fun `resolve block readiness returns needs prep when readiness conditions are not met`() {
        val readiness = resolveBlockReadiness(
            plan = FocusBlockPlan(
                date = "2026-04-03",
                normalizedBlockLabel = "writing",
                blockLabel = "Writing",
                tinyStep = "Open doc",
            ),
            habitsSatisfied = false,
            prepSatisfied = true,
        )

        assertEquals(FocusBlockReadiness.NeedsPrep, readiness)
    }
}
