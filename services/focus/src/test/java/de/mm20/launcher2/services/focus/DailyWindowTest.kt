package de.mm20.launcher2.services.focus

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Boundary coverage for [isWithinDailyWindow], including the past-midnight wind-down case. */
class DailyWindowTest {

    private fun m(hour: Int, minute: Int = 0) = hour * 60 + minute

    @Test
    fun `same-day window is half-open`() {
        // 09:00–17:00
        assertFalse(isWithinDailyWindow(m(9), m(17), m(8, 59)))
        assertTrue(isWithinDailyWindow(m(9), m(17), m(9)))       // start inclusive
        assertTrue(isWithinDailyWindow(m(9), m(17), m(16, 59)))
        assertFalse(isWithinDailyWindow(m(9), m(17), m(17)))     // end exclusive
    }

    @Test
    fun `wrap-past-midnight evening window`() {
        // 20:00–08:00 wind-down
        assertTrue(isWithinDailyWindow(m(20), m(8), m(22)))      // late evening
        assertTrue(isWithinDailyWindow(m(20), m(8), m(20)))      // start inclusive
        assertTrue(isWithinDailyWindow(m(20), m(8), m(3)))       // after midnight
        assertTrue(isWithinDailyWindow(m(20), m(8), m(7, 59)))
        assertFalse(isWithinDailyWindow(m(20), m(8), m(8)))      // end exclusive
        assertFalse(isWithinDailyWindow(m(20), m(8), m(12)))     // midday
    }

    @Test
    fun `zero-length window is off`() {
        assertFalse(isWithinDailyWindow(m(8), m(8), m(8)))
        assertFalse(isWithinDailyWindow(m(8), m(8), m(20)))
    }
}
