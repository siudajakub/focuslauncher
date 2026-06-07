package de.mm20.launcher2.ui.launcher.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusCommandMatcherTest {
    @Test
    fun `exact focus command match outranks prefix match`() {
        val exact = scoreFocusCommandMatch(
            query = normalizeFocusCommandQuery("focus report"),
            aliases = listOf("focus report"),
        )
        val prefix = scoreFocusCommandMatch(
            query = normalizeFocusCommandQuery("focus rep"),
            aliases = listOf("focus report"),
        )

        assertTrue(exact != null && prefix != null)
        assertTrue(exact!! > prefix!!)
    }

    @Test
    fun `unknown query does not create focus command match`() {
        val score = scoreFocusCommandMatch(
            query = normalizeFocusCommandQuery("calendar"),
            aliases = listOf("focus report", "resume focus"),
        )

        assertNull(score)
    }

    @Test
    fun `query normalization trims and collapses whitespace`() {
        assertEquals("resume focus", normalizeFocusCommandQuery("  resume   focus "))
    }
}
