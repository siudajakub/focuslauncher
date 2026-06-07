package de.mm20.launcher2.ui.launcher.search

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
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
import de.mm20.launcher2.preferences.search.CalendarSearchSettings
import de.mm20.launcher2.preferences.search.ContactSearchSettings
import de.mm20.launcher2.preferences.search.FileSearchSettings
import de.mm20.launcher2.preferences.search.LocationSearchSettings
import de.mm20.launcher2.preferences.search.ShortcutSearchSettings
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import de.mm20.launcher2.profiles.Profile
import de.mm20.launcher2.profiles.ProfileManager
import de.mm20.launcher2.search.AppShortcut
import de.mm20.launcher2.search.Application
import de.mm20.launcher2.search.Article
import de.mm20.launcher2.search.CalendarEvent
import de.mm20.launcher2.search.Contact
import de.mm20.launcher2.search.File
import de.mm20.launcher2.search.Location
import de.mm20.launcher2.search.ResultScore
import de.mm20.launcher2.search.SavableSearchable
import de.mm20.launcher2.search.SearchFilters
import de.mm20.launcher2.search.SearchResults
import de.mm20.launcher2.search.SearchService
import de.mm20.launcher2.search.Searchable
import de.mm20.launcher2.search.Website
import de.mm20.launcher2.search.data.Calculator
import de.mm20.launcher2.search.data.UnitConverter
import de.mm20.launcher2.search.isUnspecified
import de.mm20.launcher2.searchable.SavableSearchableRepository
import de.mm20.launcher2.searchable.VisibilityLevel
import de.mm20.launcher2.searchactions.actions.SearchAction
import de.mm20.launcher2.ui.launcher.focus.FocusLaunchCoordinator
import de.mm20.launcher2.ui.launcher.focus.FocusAppClassifier
import de.mm20.launcher2.ui.launcher.focus.FocusAppType
import de.mm20.launcher2.ui.launcher.focus.FocusHomeCommandCenter
import de.mm20.launcher2.ui.launcher.focus.FocusHomeCommandType
import de.mm20.launcher2.services.favorites.FavoritesService
import de.mm20.launcher2.searchactions.actions.SearchActionIcon
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
    private val focusAppClassifier = FocusAppClassifier()

    private val favoritesService: FavoritesService by inject()
    private val searchableRepository: SavableSearchableRepository by inject()
    private val customAttributesRepository: CustomAttributesRepository by inject()
    private val permissionsManager: PermissionsManager by inject()
    private val profileManager: ProfileManager by inject()

    private val fileSearchSettings: FileSearchSettings by inject()
    private val contactSearchSettings: ContactSearchSettings by inject()
    private val calendarSearchSettings: CalendarSearchSettings by inject()
    private val shortcutSearchSettings: ShortcutSearchSettings by inject()
    private val searchUiSettings: SearchUiSettings by inject()
    private val locationSearchSettings: LocationSearchSettings by inject()
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

    val appShortcutResults = mutableStateListOf<AppShortcut>()
    val fileResults = mutableStateListOf<File>()
    val contactResults = mutableStateListOf<Contact>()
    val calendarResults = mutableStateListOf<CalendarEvent>()
    val articleResults = mutableStateListOf<Article>()
    val websiteResults = mutableStateListOf<Website>()
    val calculatorResults = mutableStateListOf<Calculator>()
    val unitConverterResults = mutableStateListOf<UnitConverter>()
    val searchActionResults = mutableStateListOf<SearchAction>()
    val locationResults = mutableStateListOf<Location>()

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
        } else if (bestMatch is SearchAction) {
            bestMatch.start(context)
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
                        ) { focusProfiles, appTypes ->
                            AllAppsWithFocusContext(
                                results = context.results,
                                hiddenKeys = context.hiddenKeys,
                                focusProfiles = focusProfiles,
                                appTypes = appTypes,
                            )
                        }
                    }
                    .collectLatest { context ->
                        val results = context.results
                        val hiddenKeys = context.hiddenKeys
                        val focusProfiles = context.focusProfiles
                        val appTypes = context.appTypes
                        val hiddenItems = mutableListOf<SavableSearchable>()

                        val (hiddenApps, apps) = results.standardProfileApps.partition {
                            hiddenKeys.contains(it.key) ||
                                (
                                    focusModeEnabled &&
                                        hideDistractingApps &&
                                        appTypes[it.key] == FocusAppType.Distracting &&
                                        focusProfiles[it.key]?.hasTemporaryUnlock() != true
                                    )
                        }
                        hiddenItems += hiddenApps

                        val (hiddenWorkApps, workApps) = results.workProfileApps.partition {
                            hiddenKeys.contains(it.key) ||
                                (
                                    focusModeEnabled &&
                                        hideDistractingApps &&
                                        appTypes[it.key] == FocusAppType.Distracting &&
                                        focusProfiles[it.key]?.hasTemporaryUnlock() != true
                                    )
                        }
                        hiddenItems += hiddenWorkApps

                        val (hiddenPrivateApps, privateApps) = results.privateSpaceApps.partition {
                            hiddenKeys.contains(it.key) ||
                                (
                                    focusModeEnabled &&
                                        hideDistractingApps &&
                                        appTypes[it.key] == FocusAppType.Distracting &&
                                        focusProfiles[it.key]?.hasTemporaryUnlock() != true
                                    )
                        }
                        hiddenItems += hiddenPrivateApps
                        previousResults = SearchResults(apps = apps)

                        searchActionResults.clear()
                        appResults.updateItems(apps)
                        workAppResults.updateItems(workApps)
                        privateSpaceAppResults.updateItems(privateApps)
                        hiddenResults.updateItems(hiddenItems)
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
                        ) { focusProfiles, appTypes ->
                            SearchWithFocusContext(
                                results = context.results,
                                hiddenKeys = context.hiddenKeys,
                                focusProfiles = focusProfiles,
                                appTypes = appTypes,
                            )
                        }
                    }
                    .collectLatest { context ->
                        val results = context.results
                        val hiddenKeys = context.hiddenKeys
                        val focusProfiles = context.focusProfiles
                        val appTypes = context.appTypes
                        previousResults = results

                        hiddenResults.clear()
                        workAppResults.clear()
                        privateSpaceAppResults.clear()
                        appShortcutResults.clear()
                        fileResults.clear()
                        contactResults.clear()
                        calendarResults.clear()
                        locationResults.clear()
                        articleResults.clear()
                        websiteResults.clear()
                        calculatorResults.clear()
                        unitConverterResults.clear()
                        searchActionResults.clear()

                        val focusCommands = buildFocusCommandActions(query)
                        searchActionResults.updateItems(focusCommands)

                        appResults.updateItems(
                            results.apps
                            ?.filterNot { hiddenKeys.contains(it.key) }
                            ?.applyRanking(query, appTypes, focusModeEnabled)
                        )

                        if (launchOnEnter.value) {
                            bestMatch.value = when {
                                hasExactFocusCommandMatch(query) && focusCommands.isNotEmpty() -> focusCommands.first()
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

    val missingCalendarPermission = combine(
        permissionsManager.hasPermission(PermissionGroup.Calendar),
        calendarSearchSettings.providers,
    ) { perm, providers -> !perm && providers.contains("local") }

    fun requestCalendarPermission(context: AppCompatActivity) {
        permissionsManager.requestPermission(context, PermissionGroup.Calendar)
    }

    fun disableCalendarSearch() {
        calendarSearchSettings.setProviderEnabled("local", false)
    }

    val missingContactsPermission = combine(
        permissionsManager.hasPermission(PermissionGroup.Contacts),
        contactSearchSettings.isProviderEnabled("local")
    ) { perm, enabled -> !perm && enabled }

    fun requestContactsPermission(context: AppCompatActivity) {
        permissionsManager.requestPermission(context, PermissionGroup.Contacts)
    }

    fun disableContactsSearch() {
        contactSearchSettings.setProviderEnabled("local", false)
    }

    val missingLocationPermission = combine(
        permissionsManager.hasPermission(PermissionGroup.Location),
        locationSearchSettings.osmLocations.distinctUntilChanged()
    ) { perm, enabled -> !perm && enabled }

    fun requestLocationPermission(context: AppCompatActivity) {
        permissionsManager.requestPermission(context, PermissionGroup.Location)
    }

    fun disableLocationSearch() {
        locationSearchSettings.setOsmLocations(false)
    }

    val missingFilesPermission = combine(
        permissionsManager.hasPermission(PermissionGroup.ExternalStorage),
        fileSearchSettings.localFiles
    ) { perm, enabled -> !perm && enabled }

    fun requestFilesPermission(context: AppCompatActivity) {
        permissionsManager.requestPermission(context, PermissionGroup.ExternalStorage)
    }

    fun disableFilesSearch() {
        fileSearchSettings.setLocalFiles(false)
    }

    val missingAppShortcutPermission = combine(
        permissionsManager.hasPermission(PermissionGroup.AppShortcuts),
        shortcutSearchSettings.enabled,
    ) { perm, enabled -> !perm && enabled }

    fun requestAppShortcutPermission(context: AppCompatActivity) {
        permissionsManager.requestPermission(context, PermissionGroup.AppShortcuts)
    }

    fun disableAppShortcutSearch() {
        shortcutSearchSettings.setEnabled(false)
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
            addAll(files ?: emptyList())
            addAll(contacts ?: emptyList())
            addAll(calendars ?: emptyList())
            addAll(locations ?: emptyList())
            addAll(wikipedia ?: emptyList())
            addAll(websites ?: emptyList())
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
            websites = false,
            articles = false,
            places = false,
            files = false,
            shortcuts = false,
            contacts = false,
            events = false,
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

    private fun buildFocusCommandActions(query: String): List<SearchAction> {
        val normalized = normalizeFocusCommandQuery(query)
        if (normalized.isBlank()) return emptyList()

        val specs = listOf(
            FocusCommandSpec(
                label = "Open Focus System",
                aliases = listOf("focus", "focus system", "open focus", "open focus system"),
                icon = SearchActionIcon.Search,
            ) { context ->
                context.startActivity(
                    Intent(context, SettingsActivity::class.java).apply {
                        putExtra(SettingsActivity.EXTRA_ROUTE, SettingsActivity.ROUTE_FOCUS_SYSTEM)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            },
            FocusCommandSpec(
                label = "Open Focus Apps",
                aliases = listOf("focus apps", "focus app", "open focus apps"),
                icon = SearchActionIcon.SearchList,
            ) { context ->
                context.startActivity(
                    Intent(context, SettingsActivity::class.java).apply {
                        putExtra(SettingsActivity.EXTRA_ROUTE, SettingsActivity.ROUTE_FOCUS_APPS)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            },
            FocusCommandSpec(
                label = "Open Focus Report",
                aliases = listOf("focus report", "report focus", "open focus report"),
                icon = SearchActionIcon.StatsSearch,
            ) { context ->
                context.startActivity(
                    Intent(context, SettingsActivity::class.java).apply {
                        putExtra(SettingsActivity.EXTRA_ROUTE, SettingsActivity.ROUTE_FOCUS_REPORT)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            },
            FocusCommandSpec(
                label = "Start Focus Session",
                aliases = listOf("start focus", "focus start", "focus session", "start focus session"),
                icon = SearchActionIcon.Timer,
            ) { context ->
                FocusHomeCommandCenter.dispatch(FocusHomeCommandType.StartSession)
                context.startActivity(
                    Intent(context, LauncherActivity::class.java).apply {
                        putExtra(
                            de.mm20.launcher2.ui.launcher.SharedLauncherActivity.EXTRA_FOCUS_HOME_COMMAND,
                            de.mm20.launcher2.ui.launcher.SharedLauncherActivity.FOCUS_HOME_COMMAND_START_SESSION,
                        )
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            },
            FocusCommandSpec(
                label = "Resume Focus",
                aliases = listOf("resume focus", "focus resume", "continue focus", "return to focus"),
                icon = SearchActionIcon.SearchPage,
            ) { context ->
                FocusHomeCommandCenter.dispatch(FocusHomeCommandType.ResumeFocus)
                context.startActivity(
                    Intent(context, LauncherActivity::class.java).apply {
                        putExtra(
                            de.mm20.launcher2.ui.launcher.SharedLauncherActivity.EXTRA_FOCUS_HOME_COMMAND,
                            de.mm20.launcher2.ui.launcher.SharedLauncherActivity.FOCUS_HOME_COMMAND_RESUME_FOCUS,
                        )
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            },
            FocusCommandSpec(
                label = "Open Today Plan",
                aliases = listOf("today plan", "focus plan", "open today plan"),
                icon = SearchActionIcon.Calendar,
            ) { context ->
                FocusHomeCommandCenter.dispatch(FocusHomeCommandType.OpenTodayPlan)
                context.startActivity(
                    Intent(context, LauncherActivity::class.java).apply {
                        putExtra(
                            de.mm20.launcher2.ui.launcher.SharedLauncherActivity.EXTRA_FOCUS_HOME_COMMAND,
                            de.mm20.launcher2.ui.launcher.SharedLauncherActivity.FOCUS_HOME_COMMAND_OPEN_TODAY_PLAN,
                        )
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            },
        )

        return specs
            .mapNotNull { spec ->
                val rank = scoreFocusCommandMatch(normalized, spec.aliases) ?: return@mapNotNull null
                FocusCommandAction(
                    label = spec.label,
                    icon = spec.icon,
                    score = ResultScore(
                        isPrefix = true,
                        isSubstring = true,
                        isPrimary = true,
                        similarity = 1f,
                    ),
                    rank = rank,
                    onStart = spec.onStart,
                )
            }
            .sortedByDescending { it.rank }
    }
}


enum class SearchCategory {
    Apps,
    Calculator,
    Calendar,
    Contacts,
    Files,
    UnitConverter,
    Articles,
    Website,
    Location,
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
    val focusProfiles: Map<String, FocusTemporaryUnlock>,
    val appTypes: Map<String, FocusAppType>,
)

@Immutable
data class AllAppsWithFocusContext(
    val results: SearchResults, // wait, it was AllAppsResults?
    val hiddenKeys: List<String>,
    val focusProfiles: Map<String, FocusTemporaryUnlock>,
    val appTypes: Map<String, FocusAppType>,
)

private data class FocusCommandSpec(
    val label: String,
    val aliases: List<String>,
    val icon: SearchActionIcon,
    val onStart: (Context) -> Unit,
)

private class FocusCommandAction(
    override val label: String,
    override val icon: SearchActionIcon,
    override val score: ResultScore,
    val rank: Float,
    private val onStart: (Context) -> Unit,
) : SearchAction {
    override val iconColor: Int = 0
    override val customIcon: String? = null

    override fun start(context: Context) {
        onStart(context)
    }
}

internal fun normalizeFocusCommandQuery(query: String): String {
    return query.trim().lowercase().replace(Regex("\\s+"), " ")
}

internal fun scoreFocusCommandMatch(
    query: String,
    aliases: List<String>,
): Float? {
    val normalizedAliases = aliases.map(::normalizeFocusCommandQuery)
    if (query in normalizedAliases) return 1.25f
    if (normalizedAliases.any { it.startsWith(query) && query.length >= 3 }) return 1.05f
    if (normalizedAliases.any { query.startsWith(it) && it.length >= 5 }) return 0.9f
    return null
}

private fun hasExactFocusCommandMatch(query: String): Boolean {
    val normalized = normalizeFocusCommandQuery(query)
    if (normalized.isBlank()) return false
    val aliases = listOf(
        "focus",
        "focus system",
        "open focus",
        "open focus system",
        "focus apps",
        "focus app",
        "open focus apps",
        "focus report",
        "report focus",
        "open focus report",
        "start focus",
        "focus start",
        "focus session",
        "start focus session",
        "resume focus",
        "focus resume",
        "continue focus",
        "return to focus",
        "today plan",
        "focus plan",
        "open today plan",
    )
    return normalized in aliases.map(::normalizeFocusCommandQuery)
}
