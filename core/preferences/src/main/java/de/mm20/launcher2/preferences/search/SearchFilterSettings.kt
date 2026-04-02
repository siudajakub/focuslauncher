package de.mm20.launcher2.preferences.search

import de.mm20.launcher2.preferences.KeyboardFilterBarItem
import de.mm20.launcher2.preferences.LauncherDataStore
import de.mm20.launcher2.search.SearchFilters
import kotlinx.coroutines.flow.map

private val disallowedFocusFirstItems = setOf(
    KeyboardFilterBarItem.OnlineResults,
    KeyboardFilterBarItem.Websites,
    KeyboardFilterBarItem.Articles,
    KeyboardFilterBarItem.Places,
)

val focusFirstFilterBarItems = KeyboardFilterBarItem.entries.filterNot { it in disallowedFocusFirstItems }

fun SearchFilters.sanitizedForFocusFirst(): SearchFilters {
    return copy(
        allowNetwork = false,
        websites = false,
        articles = false,
        places = false,
    )
}

class SearchFilterSettings internal constructor(
    private val launcherDataStore: LauncherDataStore,
) {
    val defaultFilter
        get() = launcherDataStore.data.map { it.searchFilter.sanitizedForFocusFirst() }

    fun setDefaultFilter(filter: SearchFilters) {
        launcherDataStore.update {
            it.copy(searchFilter = filter.sanitizedForFocusFirst())
        }
    }

    val filterBar
        get() = launcherDataStore.data.map { it.searchFilterBar }

    fun setFilterBar(filterBar: Boolean) {
        launcherDataStore.update {
            it.copy(searchFilterBar = filterBar)
        }
    }

    val filterBarItems
        get() = launcherDataStore.data.map {
            it.searchFilterBarItems
                .filterNot { item -> item in disallowedFocusFirstItems }
                .distinct()
        }

    fun setFilterBarItems(items: List<KeyboardFilterBarItem>) {
        launcherDataStore.update {
            it.copy(
                searchFilterBarItems = items
                    .filterNot { item -> item in disallowedFocusFirstItems }
                    .distinct()
            )
        }
    }
}
