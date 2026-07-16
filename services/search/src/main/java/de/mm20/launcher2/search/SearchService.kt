package de.mm20.launcher2.search

import android.util.Log
import de.mm20.launcher2.data.customattrs.CustomAttributesRepository
import de.mm20.launcher2.data.customattrs.utils.withCustomLabels
import de.mm20.launcher2.profiles.Profile
import de.mm20.launcher2.profiles.ProfileManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

interface SearchService {
    /**
     * Search installed applications. [initialResults] is emitted first so the caller can keep the
     * previous query's results on screen instead of flashing empty while the new query resolves.
     */
    fun search(
        query: String,
        initialResults: SearchResults? = null,
    ): Flow<SearchResults>

    fun getAllApps(): Flow<AllAppsResults>
}

internal class SearchServiceImpl(
    private val appRepository: SearchableRepository<Application>,
    private val profileManager: ProfileManager,
    private val customAttributesRepository: CustomAttributesRepository,
) : SearchService {

    override fun search(
        query: String,
        initialResults: SearchResults?,
    ): Flow<SearchResults> {
        if (query.isBlank()) {
            return flow {
                emit(SearchResults())
            }
        }
        return flow {
            emit(initialResults ?: SearchResults())
            emitAll(
                appRepository.search(query, allowNetwork = false)
                    .withCustomLabels(customAttributesRepository)
                    .map { SearchResults(apps = it) }
            )
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
)

data class AllAppsResults(
    val standardProfileApps: List<Application>,
    val workProfileApps: List<Application>,
    val privateSpaceApps: List<Application>,
)

fun SearchResults.toList(): List<Searchable> {
    return listOfNotNull(apps).flatten()
}
