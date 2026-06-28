package de.mm20.launcher2.ui.launcher.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.mm20.launcher2.profiles.Profile
import de.mm20.launcher2.search.Application
import de.mm20.launcher2.ui.component.LauncherCard
import de.mm20.launcher2.ui.launcher.search.apps.AppResults
import de.mm20.launcher2.ui.launcher.search.common.grid.SearchResultGrid
import de.mm20.launcher2.ui.launcher.search.common.list.ListItem
import de.mm20.launcher2.ui.launcher.search.common.list.ListResults
import de.mm20.launcher2.ui.launcher.search.favorites.SearchFavorites
import de.mm20.launcher2.ui.launcher.search.favorites.SearchFavoritesVM
import de.mm20.launcher2.ui.launcher.sheets.HiddenItemsSheet
import de.mm20.launcher2.ui.launcher.sheets.LocalBottomSheetManager
import de.mm20.launcher2.ui.locals.LocalGridSettings
import de.mm20.launcher2.ui.theme.transparency.transparency

@Composable
fun SearchColumn(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    state: LazyGridState = rememberLazyGridState(),
    reverse: Boolean = false,
    userScrollEnabled: Boolean = true,
    onHideKeyboard: () -> Unit = {},
) {
    val columns = LocalGridSettings.current.columnCount
    val showList = LocalGridSettings.current.showList

    val viewModel: SearchVM = viewModel()
    val favoritesVM: SearchFavoritesVM = viewModel()

    val favorites by favoritesVM.favorites.collectAsState(emptyList())
    val favoritesEditButton by favoritesVM.showEditButton.collectAsState(false)
    val favoritesEnabled by viewModel.favoritesEnabled.collectAsState(false)
    val allAppsEnabled by viewModel.allAppsEnabled.collectAsState(false)
    val hasProfilesPermission by viewModel.hasProfilesPermission.collectAsState(false)

    val hideFavs by viewModel.hideFavorites
    val bestMatch by viewModel.bestMatch
    val query by viewModel.searchQuery
    val isSearchEmpty by viewModel.isSearchEmpty

    val apps = viewModel.appResults
    val workApps = viewModel.workAppResults
    val privateApps = viewModel.privateSpaceAppResults
    val shortcuts = viewModel.shortcutResults
    val hiddenResults = viewModel.hiddenResults

    val profiles by viewModel.profiles.collectAsState(emptyList())
    val profileStates by viewModel.profileStates.collectAsState(emptyList())

    var selectedAppProfileIndex by remember(isSearchEmpty) { mutableIntStateOf(0) }
    var selectedAppIndex by remember(query) { mutableIntStateOf(-1) }
    var selectedShortcutIndex by remember(query) { mutableIntStateOf(-1) }

    LazyVerticalGrid(
        modifier = modifier.padding(horizontal = 8.dp),
        state = state,
        userScrollEnabled = userScrollEnabled,
        contentPadding = paddingValues,
        reverseLayout = reverse,
        columns = GridCells.Fixed(columns),
    ) {
        if (!hideFavs && favoritesEnabled) {
            SearchFavorites(
                favorites = favorites,
                editButton = favoritesEditButton,
                reverse = reverse,
            )
        } else {
            item(key = "favorites", span = { GridItemSpan(maxLineSpan) }) {}
        }

        if (isSearchEmpty && profiles.size > 1 && allAppsEnabled) {
            AppResults(
                apps = when (profiles.getOrNull(selectedAppProfileIndex)?.type) {
                    Profile.Type.Private -> privateApps
                    Profile.Type.Work -> workApps
                    else -> apps
                },
                highlightedItem = bestMatch as? Application,
                profiles = profiles,
                selectedProfileIndex = selectedAppProfileIndex,
                onProfileSelected = {
                    selectedAppProfileIndex = it
                    onHideKeyboard()
                },
                isProfileLocked = profileStates.getOrNull(selectedAppProfileIndex)?.locked == true,
                onProfileLockChange = { profile, locked ->
                    viewModel.setProfileLock(profile, locked)
                },
                columns = columns,
                reverse = reverse,
                showProfileLockControls = hasProfilesPermission,
                showList = showList,
                selectedIndex = selectedAppIndex,
                onSelect = { selectedAppIndex = it },
            )
        } else if (!isSearchEmpty || allAppsEnabled) {
            AppResults(
                apps = apps,
                highlightedItem = bestMatch as? Application,
                onProfileSelected = { selectedAppProfileIndex = it },
                columns = columns,
                reverse = reverse,
                showList = showList,
                selectedIndex = selectedAppIndex,
                onSelect = { selectedAppIndex = it },
            )
        }

        if (shortcuts.isNotEmpty()) {
            if (showList) {
                // Render shortcuts (incl. browser/PWA "add to home screen" links) the same way
                // apps are rendered in list mode, so they honor the list / no-icons preferences
                // instead of always showing as an icon grid.
                ListResults(
                    key = "shortcuts",
                    items = shortcuts,
                    selectedIndex = selectedShortcutIndex,
                    itemContent = { shortcut, showDetails, index ->
                        ListItem(
                            modifier = Modifier.fillMaxWidth(),
                            item = shortcut,
                            showDetails = showDetails,
                            onShowDetails = { selectedShortcutIndex = if (it) index else -1 },
                        )
                    },
                    reverse = reverse,
                )
            } else {
                item(key = "shortcuts", span = { GridItemSpan(maxLineSpan) }) {
                    LauncherCard(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        SearchResultGrid(shortcuts, reverse = reverse)
                    }
                }
            }
        }
    }

    val sheetManager = LocalBottomSheetManager.current
    HiddenItemsSheet(
        expanded = sheetManager.hiddenItemsSheetShown.value,
        items = hiddenResults,
        onDismiss = { sheetManager.dismissHiddenItemsSheet() },
    )
}

fun LazyListScope.SingleResult(
    highlight: Boolean = false,
    content: @Composable (() -> Unit)?,
) {
    if (content == null) return
    item {
        LauncherCard(
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 4.dp,
            ),
            color = if (highlight) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface.copy(MaterialTheme.transparency.surface)
            },
        ) {
            content()
        }
    }
}
