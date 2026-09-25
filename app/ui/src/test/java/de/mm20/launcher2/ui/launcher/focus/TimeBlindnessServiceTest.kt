package de.mm20.launcher2.ui.launcher.focus

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeBlindnessServiceTest {
    @Test
    fun `same foreground package keeps its continuous start across checks`() {
        val started = observeForegroundPackage(ContinuousForegroundState(), "com.example.video", 1_000L)
        val unchanged = observeForegroundPackage(started, "com.example.video", 61_000L)
        val switched = observeForegroundPackage(unchanged, "com.example.chat", 62_000L)

        assertEquals(1_000L, unchanged.sinceMillis)
        assertEquals(62_000L, switched.sinceMillis)
    }
}
