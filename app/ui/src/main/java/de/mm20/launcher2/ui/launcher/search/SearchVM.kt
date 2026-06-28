package de.mm20.launcher2.ui.launcher.search

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.mm20.launcher2.data.customattrs.CustomAttributesRepository
import de.mm20.launcher2.data.customattrs.FocusTemporaryUnlock
import de.mm20.launcher2.devicepose.DevicePoseProvider
import de.mm20.launcher2.ktx.isAtLeastApiLevel
import de.mm20.launcher2.permissions.PermissionGroup
import de.mm20.launcher2.permissions.PermissionsManager

import de.mm20.launcher2.preferences.ui.SearchUiSettings
import de.mm20.launcher2.profiles.Profile
import de.mm20.launcher2.profiles.ProfileManager
import de.mm20.launcher2.search.AppShortcut
import de.mm20.launcher2.search.Application

import de.mm20.launcher2.search.ResultScore
import de.mm20.launcher2.search.SavableSearchable
import de.mm20.launcher2.search.SearchFilters
import de.mm20.launcher2.search.SearchResults
import de.mm20.launcher2.search.SearchService
import de.mm20.launcher2.search.Searchable

import de.mm20.launcher2.search.isUnspecified
import de.mm20.launcher2.searchable.SavableSearchableRepository
import de.mm20.launcher2.searchable.VisibilityLevel
import de.mm20.launcher2.ui.launcher.focus.FocusLaunchCoordinator
import de.mm20.launcher2.services.focus.FocusAppClassifier
import de.mm20.launcher2.services.focus.FocusAppType
import de.mm20.launcher2.services.favorites.FavoritesService
import de.mm20.launcher2.ui.settings.SettingsActivity
import de.mm20.launcher2.ui.launcher.LauncherActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SearchVM : ViewModel(), KoinComponent {
    private val focusLaunchCoordinator = FocusLaunchCoordinator()
    private val focusAppClassifier: FocusAppClassifier by inject()

    private val favoritesService: FavoritesService by inject()
    private val searchableRepository: SavableSearchableRepository by inject()
    private val customAttributesRepository: CustomAttributesRepository by inject()
    private val permissionsManager: PermissionsManager by inject()
    private val profileManager: ProfileManager by inject()

    private val searchUiSettings: SearchUiSettings by inject()
    private val devicePoseProvider: DevicePoseProvider by inject()

    val launchOnEnter = searchUiSettings.launchOnEnter
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val strictAppsOnly = flowOf(true)
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val searchService: SearchService by inject()

    val searchQuery = mutableStateOf("")
    val isSearchEmpty = mutableStateOf(true)

    val expandedCategory = mutableStateOf<SearchCategory?>(null)

    val profiles = profileManager.profiles.shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        replay = 1
    )
    val profileStates = profiles.flatMapLatest {
        combine(it.map { profileManager.getProfileState(it) }) {
            it.toList()
        }
    }

    val hasProfilesPermission = permissionsManager.hasPermission(PermissionGroup.ManageProfiles)

    fun setProfileLock(profile: Profile?, locked: Boolean) {
        if (isAtLeastApiLevel(28) && profile != null) {
            if (locked) {
                profileManager.lockProfile(profile)
            } else {
                profileManager.unlockProfile(profile)
            }
        }
    }

    val appResults = mutableStateListOf<Application>()
    val workAppResults = mutableStateListOf<Application>()
    val privateSpaceAppResults = mutableStateListOf<Application>()

    // Pinned launcher shortcuts (incl. PWAs added from a browser) that match the current query.
    val shortcutResults = mutableStateListOf<SavableSearchable>()



    var previousResults: SearchResults? = null

    val hiddenResultsButton = searchUiSettings.hiddenItemsButton
    val hiddenResults = mutableStateListOf<SavableSearchable>()

    val favoritesEnabled = searchUiSettings.favorites
    val hideFavorites = mutableStateOf(false)

    val allAppsEnabled = searchUiSettings.allApps

    val bestMatch = mutableStateOf<Searchable?>(null)

    init {
        search("", forceRestart = true)
    }

    fun launchBestMatchOrAction(context: Context) {
        val bestMatch = bestMatch.value
        if (bestMatch is SavableSearchable) {
            focusLaunchCoordinator.launch(bestMatch, context, null)
            return
        }
    }

    fun reset() {
        search("")
    }

    private var searchJob: Job? = null
    fun search(query: String, forceRestart: Boolean = false) {
        if (searchQuery.value == query && !forceRestart) return
        searchQuery.value = query
        isSearchEmpty.value = query.isEmpty()
        expandedCategory.value = null

        if (isSearchEmpty.value)
            bestMatch.value = null
        try {
            searchJob?.cancel()
        } catch (_: CancellationException) {
        }
        hideFavorites.value = query.isNotEmpty()

        searchJob = viewModelScope.launch {
            val focusModeEnabled = searchUiSettings.focusModeEnabled.first()
            val hideDistractingApps = searchUiSettings.focusHideDistractingApps.first()
            if (query.isEmpty()) {
                val hiddenItemKeys = searchableRepository.getKeys(
                    maxVisibility = VisibilityLevel.SearchOnly,
                    includeTypes = listOf("app"),
                )
                val allApps = searchService.getAllApps()

                allApps
                    .combine(hiddenItemKeys) { results, hiddenKeys ->
                        AllAppsContext(results = results, hiddenKeys = hiddenKeys)
                    }
                    .flatMapLatest { context ->
                        val searchables = context.results.standardProfileApps +
                            context.results.workProfileApps +
                            context.results.privateSpaceApps
                        combine(
                            customAttributesRepository.getFocusTemporaryUnlocks(searchables),
                            focusAppClassifier.classify(searchables.map { it.key }),
                        ) { temporaryUnlocks, appTypes ->
                            AllAppsWithFocusContext(
                                results = context.results,
                                hiddenKeys = context.hiddenKeys,
                                temporaryUnlocks = temporaryUnlocks,
                                appTypes = appTypes,
                            )
                        }
                    }
                    .collectLatest { context ->
                        val results = context.results
                        val hiddenKeys = context.hiddenKeys
                        val temporaryUnlocks = context.temporaryUnlocks
                        val appTypes = context.appTypes
                        val hiddenItems = mutableListOf<SavableSearchable>()

                        val (hiddenApps, apps) = results.standardProfileApps.partition {
                            hiddenKeys.contains(it.key) ||
                                (
                                    focusModeEnabled &&
                                        hideDistractingApps &&
                                        appTypes[it.key] == FocusAppType.Distracting &&
                                        temporaryUnlocks[it.key]?.hasTemporaryUnlock() != true
                                    )
                        }
                        hiddenItems += hiddenApps

                        val (hiddenWorkApps, workApps) = results.workProfileApps.partition {
                            hiddenKeys.contains(it.key) ||
                                (
                                    focusModeEnabled &&
                                        hideDistractingApps &&
                                        appTypes[it.key] == FocusAppType.Distracting &&
                                        temporaryUnlocks[it.key]?.hasTemporaryUnlock() != true
                                    )
                        }
                        hiddenItems += hiddenWorkApps

                        val (hiddenPrivateApps, privateApps) = results.privateSpaceApps.partition {
                            hiddenKeys.contains(it.key) ||
                                (
                                    focusModeEnabled &&
                                        hideDistractingApps &&
                                        appTypes[it.key] == FocusAppType.Distracting &&
                                        temporaryUnlocks[it.key]?.hasTemporaryUnlock() != true
                                    )
                        }
                        hiddenItems += hiddenPrivateApps
                        previousResults = SearchResults(apps = apps)

                        appResults.updateItems(apps)
                        workAppResults.updateItems(workApps)
                        privateSpaceAppResults.updateItems(privateApps)
                        hiddenResults.updateItems(hiddenItems)
                        shortcutResults.clear()
                    }

            } else {
                val hiddenItemKeys = searchableRepository.getKeys(
                    maxVisibility = VisibilityLevel.Hidden,
                )
                searchService.search(
                    query,
                    filters = launcherSearchFilters(),
                    previousResults,
                )
                    .combine(hiddenItemKeys) { results, hiddenKeys ->
                        SearchContext(results = results, hiddenKeys = hiddenKeys)
                    }
                    .flatMapLatest { context ->
                        combine(
                            customAttributesRepository.getFocusTemporaryUnlocks(context.results.collectSavableSearchables()),
                            focusAppClassifier.classify((context.results.apps ?: emptyList()).map { it.key }),
                        ) { temporaryUnlocks, appTypes ->
                            SearchWithFocusContext(
                                results = context.results,
                                hiddenKeys = context.hiddenKeys,
                                temporaryUnlocks = temporaryUnlocks,
                                appTypes = appTypes,
                            )
                        }
                    }
                    .collectLatest { context ->
                        val results = context.results
                        val hiddenKeys = context.hiddenKeys
                        val temporaryUnlocks = context.temporaryUnlocks
                        val appTypes = context.appTypes
                        previousResults = results

                        hiddenResults.clear()
                        workAppResults.clear()
                        privateSpaceAppResults.clear()

                        appResults.updateItems(
                            results.apps
                            ?.filterNot { hiddenKeys.contains(it.key) }
                            ?.applyRanking(query, appTypes, focusModeEnabled)
                        )

                        shortcutResults.updateItems(
                            results.shortcuts
                            ?.filterNot { hiddenKeys.contains(it.key) }
                        )

                        if (launchOnEnter.value) {
                            bestMatch.value = when {
                                appResults.isNotEmpty() -> appResults.first()
                                else -> null
                            }
                        } else {
                            bestMatch.value = null
                        }
                    }
            }
        }
    }



    fun expandCategory(category: SearchCategory) {
        expandedCategory.value = null
    }

    private suspend fun <T : SavableSearchable> List<T>.applyRanking(
        query: String,
        appTypes: Map<String, FocusAppType>,
        prioritizeFocus: Boolean,
    ): List<T> {
        if (size <= 1) return this
        val sequence = asSequence()
        val weights = searchableRepository.getWeights(map { it.key }).first()
        val sorted = sequence.sortedWith { a, b ->
            val aWeight = weights[a.key] ?: 0.0
            val bWeight = weights[b.key] ?: 0.0

            val aScore = if (a.score.isUnspecified) {
                ResultScore.from(query = query, primaryFields = listOf(a.labelOverride ?: a.label)).score
            } else {
                a.score.score
            }

            val bScore = if (b.score.isUnspecified) {
                ResultScore.from(query = query, primaryFields = listOf(b.labelOverride ?: b.label)).score
            } else {
                b.score.score
            }

            val aFocusAdjustment = if (prioritizeFocus) appTypes[a.key].focusAdjustment() else 0f
            val bFocusAdjustment = if (prioritizeFocus) appTypes[b.key].focusAdjustment() else 0f

            val aTotal = aScore * 0.6f + aWeight.toFloat() * 0.4f + aFocusAdjustment
            val bTotal = bScore * 0.6f + bWeight.toFloat() * 0.4f + bFocusAdjustment

            bTotal.compareTo(aTotal)
        }
        return sorted.distinctBy { it.key }.toList()
    }

    private fun SearchResults.collectSavableSearchables(): List<SavableSearchable> {
        return buildList {
            addAll(apps ?: emptyList())
            addAll(shortcuts ?: emptyList())
        }
    }

    private fun FocusAppType?.focusAdjustment(): Float {
        return when (this) {
            FocusAppType.Essential -> 0.08f
            FocusAppType.Distracting -> -0.18f
            else -> 0f
        }
    }

    private fun launcherSearchFilters(): SearchFilters {
        return SearchFilters(
            allowNetwork = false,
            hiddenItems = false,
            apps = true,
            shortcuts = true,
            tools = false,
        )
    }

    /**
     * Merges a list of new items into the current SnapshotStateList.
     * It removes items that are in the current list but not in the new list.
     * Then, it updates existing items or adds new items from the new list.
     *
     * @param T The type of items in the list.
     * @param newItems The list of new items to merge with. If null, an empty list is used.
     */
    private fun <T> SnapshotStateList<T>.updateItems(newItems: List<T>?) {
        clear()
        addAll(newItems ?: emptyList())
    }
}


enum class SearchCategory {
    Apps,
    Shortcuts,
}

private data class AllAppsContext(
    val results: de.mm20.launcher2.search.AllAppsResults,
    val hiddenKeys: List<String>,
)

private data class SearchContext(
    val results: SearchResults,
    val hiddenKeys: List<String>,
)

private data class SearchWithFocusContext(
    val results: SearchResults,
    val hiddenKeys: List<String>,
    val temporaryUnlocks: Map<String, FocusTemporaryUnlock>,
    val appTypes: Map<String, FocusAppType>,
)

@Immutable
data class AllAppsWithFocusContext(
    val results: de.mm20.launcher2.search.AllAppsResults,
    val hiddenKeys: List<String>,
    val temporaryUnlocks: Map<String, FocusTemporaryUnlock>,
    val appTypes: Map<String, FocusAppType>,
)
