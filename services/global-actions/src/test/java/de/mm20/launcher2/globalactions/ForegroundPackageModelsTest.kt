package de.mm20.launcher2.globalactions

import org.junit.Assert.assertEquals
import org.junit.Test

class ForegroundPackageModelsTest {
    @Test
    fun `long-running app remains foreground without a recent resume`() {
        val foreground = reduceForegroundPackage(
            currentPackage = null,
            eventPackage = "com.example.video",
            movedToForeground = true,
            movedToBackground = false,
        )

        assertEquals("com.example.video", foreground)
    }

    @Test
    fun `home replaces app and stale background event cannot clear home`() {
        val app = reduceForegroundPackage(null, "com.example.video", true, false)
        val home = reduceForegroundPackage(app, "com.example.launcher", true, false)
        val afterStalePause = reduceForegroundPackage(home, "com.example.video", false, true)

        assertEquals("com.example.launcher", afterStalePause)
    }

    @Test
    fun `background event clears the matching foreground package idempotently`() {
        val cleared = reduceForegroundPackage("com.example.video", "com.example.video", false, true)
        val clearedAgain = reduceForegroundPackage(cleared, "com.example.video", false, true)

        assertEquals(null, cleared)
        assertEquals(null, clearedAgain)
    }

    @Test
    fun `same package activity stop cannot clear a newer resumed activity`() {
        var state = ForegroundActivityState()
        state = reduceForegroundActivityState(
            state = state,
            eventPackage = "com.example.video",
            activityId = "com.example.video/ChildActivity",
            eventTimestampMillis = 1_000L,
            activityResumed = true,
            activityPaused = false,
            activityStopped = false,
            legacyMovedToForeground = false,
            legacyMovedToBackground = false,
            supportsActivityLifecycleEvents = true,
        )
        state = reduceForegroundActivityState(
            state = state,
            eventPackage = "com.example.video",
            activityId = "com.example.video/ChildActivity",
            eventTimestampMillis = 2_000L,
            activityResumed = false,
            activityPaused = true,
            activityStopped = false,
            legacyMovedToForeground = false,
            legacyMovedToBackground = false,
            supportsActivityLifecycleEvents = true,
        )
        state = reduceForegroundActivityState(
            state = state,
            eventPackage = "com.example.video",
            activityId = "com.example.video/ParentActivity",
            eventTimestampMillis = 3_000L,
            activityResumed = true,
            activityPaused = false,
            activityStopped = false,
            legacyMovedToForeground = false,
            legacyMovedToBackground = false,
            supportsActivityLifecycleEvents = true,
        )
        state = reduceForegroundActivityState(
            state = state,
            eventPackage = "com.example.video",
            activityId = "com.example.video/ChildActivity",
            eventTimestampMillis = 4_000L,
            activityResumed = false,
            activityPaused = false,
            activityStopped = true,
            legacyMovedToForeground = false,
            legacyMovedToBackground = false,
            supportsActivityLifecycleEvents = true,
        )

        assertEquals("com.example.video", state.currentPackage())
        assertEquals(setOf("com.example.video/ParentActivity"), state.resumedActivities.keys)
    }

    @Test
    fun `newest resumed activity determines current package`() {
        var state = ForegroundActivityState()
        state = reduceForegroundActivityState(
            state, "com.example.first", "com.example.first/MainActivity", 1_000L,
            activityResumed = true, activityPaused = false, activityStopped = false,
            legacyMovedToForeground = false, legacyMovedToBackground = false,
            supportsActivityLifecycleEvents = true,
        )
        state = reduceForegroundActivityState(
            state, "com.example.second", "com.example.second/MainActivity", 2_000L,
            activityResumed = true, activityPaused = false, activityStopped = false,
            legacyMovedToForeground = false, legacyMovedToBackground = false,
            supportsActivityLifecycleEvents = true,
        )

        assertEquals("com.example.second", state.currentPackage())
    }

    @Test
    fun `late stop of old same-class generation keeps newer generation foreground`() {
        val activityId = "com.example.video/MainActivity"
        var state = ForegroundActivityState()
        state = reduceForegroundActivityState(
            state, "com.example.video", activityId, 1_000L,
            activityResumed = true, activityPaused = false, activityStopped = false,
            legacyMovedToForeground = false, legacyMovedToBackground = false,
            supportsActivityLifecycleEvents = true,
        )
        state = reduceForegroundActivityState(
            state, "com.example.video", activityId, 2_000L,
            activityResumed = false, activityPaused = true, activityStopped = false,
            legacyMovedToForeground = false, legacyMovedToBackground = false,
            supportsActivityLifecycleEvents = true,
        )
        state = reduceForegroundActivityState(
            state, "com.example.video", activityId, 3_000L,
            activityResumed = true, activityPaused = false, activityStopped = false,
            legacyMovedToForeground = false, legacyMovedToBackground = false,
            supportsActivityLifecycleEvents = true,
        )
        state = reduceForegroundActivityState(
            state, "com.example.video", activityId, 4_000L,
            activityResumed = false, activityPaused = false, activityStopped = true,
            legacyMovedToForeground = false, legacyMovedToBackground = false,
            supportsActivityLifecycleEvents = true,
        )

        assertEquals("com.example.video", state.currentPackage())
        assertEquals(2L, state.resumedActivities.getValue(activityId).generation)
        assertEquals(emptyList<ForegroundActivity>(), state.retiredActivities[activityId].orEmpty())
    }
}
