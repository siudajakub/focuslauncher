package de.mm20.launcher2.services.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusUsageSummaryTest {

    private fun min(n: Int) = n * 60_000L

    @Test
    fun `aggregates only distracting apps with time, ranked`() {
        val usage = mapOf(
            "com.social" to min(45),
            "com.video" to min(80),
            "com.essential" to min(30),   // not distracting → ignored
            "com.idle" to 0L,             // distracting but unused → dropped
        )
        val distracting = listOf(
            "com.social" to "Social",
            "com.video" to "Video",
            "com.idle" to "Idle",
        )

        val summary = summarizeDistractingUsage(usage, distracting)

        assertTrue(summary.hasData)
        assertEquals(125, summary.totalMinutes)   // 45 + 80, essential excluded
        assertEquals(2, summary.appCount)
        assertEquals(listOf("Video", "Social"), summary.topApps.map { it.label })
        assertEquals(80, summary.topApps.first().minutes)
    }

    @Test
    fun `same package under two profiles is not double-counted`() {
        val usage = mapOf("com.social" to min(50))
        val distracting = listOf("com.social" to "Social", "com.social" to "Social (work)")

        val summary = summarizeDistractingUsage(usage, distracting)

        assertEquals(50, summary.totalMinutes)
        assertEquals(1, summary.appCount)
    }

    @Test
    fun `no data when nothing was used`() {
        val summary = summarizeDistractingUsage(emptyMap(), listOf("com.social" to "Social"))
        assertFalse(summary.hasData)
        assertEquals(0, summary.totalMinutes)
        assertTrue(summary.topApps.isEmpty())
    }

    @Test
    fun `topN caps the list but total counts all`() {
        val usage = (1..6).associate { "app$it" to min(it * 10) }
        val distracting = (1..6).map { "app$it" to "App $it" }

        val summary = summarizeDistractingUsage(usage, distracting, topN = 3)

        assertEquals(3, summary.topApps.size)
        assertEquals("App 6", summary.topApps.first().label)   // 60 min, highest
        assertEquals(10 + 20 + 30 + 40 + 50 + 60, summary.totalMinutes)
        assertEquals(6, summary.appCount)
    }
}
