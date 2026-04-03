package de.mm20.launcher2.ui.settings.focusschedule

import de.mm20.launcher2.preferences.ScheduleDockMapping
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleDockSupportTest {

    @Test
    fun `findScheduleDockMapping matches normalized event name`() {
        val mappings = listOf(
            ScheduleDockMapping(eventName = "Morning Standup", appKeys = listOf("app-1")),
            ScheduleDockMapping(eventName = "morning standup", appKeys = listOf("app-2")),
        )

        assertEquals(
            listOf("app-1"),
            findScheduleDockMapping("Morning Standup", mappings)?.appKeys,
        )
        assertEquals(
            listOf("app-1"),
            findScheduleDockMapping(" Morning   Standup ", mappings)?.appKeys,
        )
        assertNull(findScheduleDockMapping("Standup", mappings))
    }
}
