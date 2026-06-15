package de.mm20.launcher2.search

import android.util.Log
import de.mm20.launcher2.data.customattrs.CustomAttributesRepository
import de.mm20.launcher2.data.customattrs.utils.withCustomLabels
import de.mm20.launcher2.profiles.Profile
import de.mm20.launcher2.profiles.ProfileManager
import de.mm20.launcher2.search.data.UnitConverter
import de.mm20.launcher2.unitconverter.UnitConverterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

interface SearchService {
    fun search(
        query: String,
        filters: SearchFilters,
        initialResults: SearchResults? = null,
    ): Flow<SearchResults>

    fun getAllApps(): Flow<AllAppsResults>
}

internal class SearchServiceImpl(
    private val appRepository: SearchableRepository<Application>,
    private val appShortcutRepository: SearchableRepository<AppShortcut>,
    private val unitConverterRepository: UnitConverterRepository,
    private val profileManager: ProfileManager,
    private val customAttributesRepository: CustomAttributesRepository,
) : SearchService {

    override fun search(
        query: String,
        filters: SearchFilters,
        initialResults: SearchResults?,
    ): Flow<SearchResults> {
        if (query.isBlank()) {
            return flow {
                emit(SearchResults())
            }
        }
        return flow {
            val results = MutableStateFlow(
                initialResults?.copy(
                    apps = initialResults.apps.takeIf { filters.apps },
                    shortcuts = initialResults.shortcuts.takeIf { filters.shortcuts },
                    unitConverters = initialResults.unitConverters.takeIf { filters.tools },
                ) ?: SearchResults()
            )
            supervisorScope {
                if (filters.apps) {
                    launch {
                        appRepository.search(query, filters.allowNetwork)
                            .withCustomLabels(customAttributesRepository)
                            .collectLatest { r ->
                                results.update {
                                    it.copy(apps = r)
                                }
                            }
                    }
                }
                if (filters.shortcuts) {
                    launch {
                        appShortcutRepository.search(query, filters.allowNetwork)
                            .withCustomLabels(customAttributesRepository)
                            .collectLatest { r ->
                                results.update {
                                    it.copy(shortcuts = r)
                                }
                            }
                    }
                }
                if (filters.tools) {
                    launch {
                        unitConverterRepository.search(query)
                            .collectLatest { r ->
                                results.update {
                                    it.copy(unitConverters = r?.let { listOf(it) }
                                        ?: listOf())
                                }
                            }
                    }
                }

                emitAll(results)
            }
        }
    }

    override fun getAllApps(): Flow<AllAppsResults> {
        return profileManager.profiles.flatMapLatest { profiles ->
            val standardProfile = profiles.find { it.type == Profile.Type.Personal }
            val workProfile = profiles.find { it.type == Profile.Type.Work }
            val privateSpace = profiles.find { it.type == Profile.Type.Private }
            appRepository.search("", false)
                .withCustomLabels(customAttributesRepository)
                .map { apps ->
                    val standardProfileApps = mutableListOf<Application>()
                    val workProfileApps = mutableListOf<Application>()
                    val privateSpaceApps = mutableListOf<Application>()
                    for (app in apps) {
                        when {
                            standardProfile != null && app.user == standardProfile.userHandle -> standardProfileApps.add(
                                app
                            )

                            workProfile != null && app.user == workProfile.userHandle -> workProfileApps.add(
                                app
                            )

                            privateSpace != null && app.user == privateSpace.userHandle -> privateSpaceApps.add(
                                app
                            )

                            else -> {
                                Log.w(
                                    "MM20",
                                    "App ${app.label} does not belong to any known profile. Ignoring."
                                )
                            }
                        }
                    }

                    AllAppsResults(
                        standardProfileApps = standardProfileApps.sorted(),
                        workProfileApps = workProfileApps.sorted(),
                        privateSpaceApps = privateSpaceApps.sorted(),
                    )
                }
        }
    }
}

data class SearchResults(
    val apps: List<Application>? = null,
    val shortcuts: List<AppShortcut>? = null,
    val unitConverters: List<UnitConverter>? = null,
)

data class AllAppsResults(
    val standardProfileApps: List<Application>,
    val workProfileApps: List<Application>,
    val privateSpaceApps: List<Application>,
)

fun SearchResults.toList(): List<Searchable> {
    return listOfNotNull(
        apps,
        shortcuts,
        unitConverters,
    ).flatten()
}
