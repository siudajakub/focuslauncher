package de.mm20.launcher2.search

import de.mm20.launcher2.ktx.toInt
import kotlinx.serialization.Serializable

@Serializable
data class SearchFilters(
    val allowNetwork: Boolean = false,
    val hiddenItems: Boolean = false,
    val apps: Boolean = true,
    val shortcuts: Boolean = false,
    val tools: Boolean = false,
) {
    // Focus-first launcher only treats these utility/result groups as user-facing categories.
    private val categories = listOf(apps, shortcuts, tools)

    val allCategoriesEnabled
        get() = categories.all { it }

    val enabledCategories: Int
        get() = categories.count { it }

    fun toInt(): Int {
        return (apps.toInt() shl 0) or
            (shortcuts.toInt() shl 1) or
            (tools.toInt() shl 2) or
            (allowNetwork.toInt() shl 3) or
            (hiddenItems.toInt() shl 4)
    }

    companion object {
        fun fromInt(value: Int): SearchFilters {
            return SearchFilters(
                apps = (value and (1 shl 0)) != 0,
                shortcuts = (value and (1 shl 1)) != 0,
                tools = (value and (1 shl 2)) != 0,
                allowNetwork = (value and (1 shl 3)) != 0,
                hiddenItems = (value and (1 shl 4)) != 0,
            )
        }
    }
}
