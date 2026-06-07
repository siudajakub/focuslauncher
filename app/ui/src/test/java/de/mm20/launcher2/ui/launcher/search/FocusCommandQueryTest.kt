package de.mm20.launcher2.ui.launcher.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FocusCommandQueryTest {
    @Test
    fun `exact focus command gets highest score`() {
        val score = scoreFocusCommandMatch("focus report", listOf("focus report"))
        assertEquals(1.25f, score ?: error("score should not be null"), 0.001f)
    }

    @Test
    fun `prefix focus command is supported`() {
        val score = scoreFocusCommandMatch("focus rep", listOf("focus report"))
        assertEquals(1.05f, score ?: error("score should not be null"), 0.001f)
    }

    @Test
    fun `unrelated query has no focus command score`() {
        assertNull(scoreFocusCommandMatch("weather tomorrow", listOf("focus report")))
    }
}
