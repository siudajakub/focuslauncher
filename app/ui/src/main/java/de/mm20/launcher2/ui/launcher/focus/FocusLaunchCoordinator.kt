package de.mm20.launcher2.ui.launcher.focus

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.ui.unit.IntRect
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import de.mm20.launcher2.search.AppShortcut
import de.mm20.launcher2.search.Application
import de.mm20.launcher2.search.SavableSearchable
import de.mm20.launcher2.services.favorites.FavoritesService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FocusLaunchCoordinator : KoinComponent {
    private val favoritesService: FavoritesService by inject()
    private val searchUiSettings: SearchUiSettings by inject()
    private val focusPolicyService = FocusPolicyService()

    fun launch(
        searchable: SavableSearchable,
        context: Context,
        options: Bundle? = null,
        launchBounds: IntRect? = null,
    ): Boolean {
        if (searchable !is Application) {
            return launchDirect(searchable, context, options)
        }
        val focusModeEnabled = runBlocking {
            searchUiSettings.focusModeEnabled.first()
        }
        if (!focusModeEnabled) {
            return launchDirect(searchable, context, options)
        }
        val decision = runBlocking {
            focusPolicyService.evaluate(searchable)
        }
        if (!decision.requiresGate) {
            return launchDirect(searchable, context, options)
        }
        favoritesService.upsert(searchable)

        val intent = FocusGateActivity.intent(context, searchable, launchBounds)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return true
    }

    fun launchDirect(searchable: SavableSearchable, context: Context, options: Bundle? = null): Boolean {
        if (searchable.launch(context, options)) {
            favoritesService.reportLaunch(searchable)
            return true
        }
        if (searchable is Application || searchable is AppShortcut) {
            favoritesService.reset(searchable)
        }
        return false
    }
}
