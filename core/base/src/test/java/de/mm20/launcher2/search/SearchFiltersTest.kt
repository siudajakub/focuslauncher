package de.mm20.launcher2.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchFiltersTest {

    @Test
    fun `toInt and fromInt preserve focus first filters`() {
        val filters = SearchFilters(
            allowNetwork = true,
            hiddenItems = true,
            apps = true,
            shortcuts = false,
            tools = true,
        )

        assertEquals(filters, SearchFilters.fromInt(filters.toInt()))
    }

    @Test
    fun `fromInt ignores removed legacy category bits`() {
        val legacyBits = (1 shl 5) or (1 shl 6) or (1 shl 7) or (1 shl 8)
        val filters = SearchFilters.fromInt(legacyBits)

        assertFalse(filters.apps)
        assertFalse(filters.shortcuts)
        assertFalse(filters.tools)
        assertFalse(filters.allowNetwork)
        assertFalse(filters.hiddenItems)
    }

    @Test
    fun `all categories only includes apps shortcuts and tools`() {
        val filters = SearchFilters(apps = true, shortcuts = true, tools = true)

        assertTrue(filters.allCategoriesEnabled)
        assertEquals(3, filters.enabledCategories)
    }
}
