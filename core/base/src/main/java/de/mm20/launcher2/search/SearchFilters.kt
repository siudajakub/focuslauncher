package de.mm20.launcher2.search

import de.mm20.launcher2.ktx.toInt
import kotlinx.serialization.Serializable

@Serializable
data class SearchFilters(
    val allowNetwork: Boolean = false,
    val hiddenItems: Boolean = false,
    val apps: Boolean = true,
    val websites: Boolean = false,
    val articles: Boolean = false,
    val places: Boolean = false,
    val files: Boolean = false,
    val shortcuts: Boolean = false,
    val contacts: Boolean = false,
    val events: Boolean = false,
    val tools: Boolean = false,
) {
    // Focus-first launcher only treats these utility/result groups as user-facing categories.
    private val categories = listOf(apps, files, shortcuts, contacts, events, tools)

    val allCategoriesEnabled
        get() = categories.all { it }

    val enabledCategories: Int
        get() = categories.count { it }
}
