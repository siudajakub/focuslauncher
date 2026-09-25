package de.mm20.launcher2.preferences.search

import de.mm20.launcher2.search.SearchFilters
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchFilterSettingsTest {
    @Test
    fun `focus first filter keeps only apps`() {
        val filters = SearchFilters(apps = false, shortcuts = true, tools = true)
            .sanitizedForFocusFirst()

        assertTrue(filters.apps)
        assertFalse(filters.shortcuts)
        assertFalse(filters.tools)
        assertFalse(filters.allowNetwork)
    }
}
