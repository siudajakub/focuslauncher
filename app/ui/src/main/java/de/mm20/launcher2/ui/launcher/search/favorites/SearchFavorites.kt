package de.mm20.launcher2.ui.launcher.search.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.mm20.launcher2.search.SavableSearchable
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.Banner
import de.mm20.launcher2.ui.launcher.search.common.grid.SearchResultGrid
import de.mm20.launcher2.ui.layout.BottomReversed
import de.mm20.launcher2.ui.theme.transparency.transparency

fun LazyGridScope.SearchFavorites(
    favorites: List<SavableSearchable>,
    editButton: Boolean,
    reverse: Boolean,
) {
    item(
        key = "favorites",
        span = {
            GridItemSpan(maxLineSpan)
        }
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Column(
                modifier = Modifier
                    .padding(
                        top = if (reverse) 8.dp else 0.dp,
                        bottom = if (reverse) 0.dp else 8.dp,
                    )
                    .background(
                        MaterialTheme.colorScheme.surface.copy(
                            MaterialTheme.transparency.surface
                        ),
                        MaterialTheme.shapes.medium
                    )
                    .padding(vertical = 4.dp),
                verticalArrangement = if (reverse) Arrangement.BottomReversed else Arrangement.Top
            ) {
                if (favorites.isNotEmpty()) {
                    SearchResultGrid(favorites, reverse = reverse)
                } else {
                    Banner(
                        modifier = Modifier.padding(16.dp),
                        text = stringResource(R.string.favorites_empty),
                        icon = R.drawable.star_24px,
                    )
                }
            }
        }
    }
}
