package de.mm20.launcher2.services.focus

import android.content.Context
import android.os.Bundle
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import de.mm20.launcher2.search.AppShortcut
import de.mm20.launcher2.search.Application
import de.mm20.launcher2.search.SavableSearchable
import de.mm20.launcher2.services.favorites.FavoritesService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FocusLaunchCoordinator(
    private val favoritesService: FavoritesService,
    private val searchUiSettings: SearchUiSettings,
    private val focusPolicyService: FocusPolicyService,
    private val gateLauncher: FocusGateLauncher,
) {
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    /**
     * Launches [searchable]. For non-applications and when focus mode is disabled this resolves
     * synchronously and returns whether the launch succeeded. For applications with focus mode
     * enabled the gating decision (DataStore + DB reads) is evaluated off the main thread to avoid
     * blocking the UI thread; the launch is then completed asynchronously (either a direct launch
     * or by opening the focus gate). In that case [launch] returns `true` to signal that the launch
     * has been handled so callers do not fall back to showing item details.
     */
    fun launch(
        searchable: SavableSearchable,
        context: Context,
        options: Bundle? = null,
        launchBounds: LaunchBounds? = null,
    ): Boolean {
        if (searchable !is Application) {
            return launchDirect(searchable, context, options)
        }
        // Application + potential focus gating: resolve off the main thread. We have committed to
        // handling the launch here, so report it as handled to the caller and complete it async.
        scope.launch {
            val focusModeEnabled = withContext(Dispatchers.Default) {
                searchUiSettings.focusModeEnabled.first()
            }
            if (!focusModeEnabled) {
                launchDirect(searchable, context, options)
                return@launch
            }
            val decision = withContext(Dispatchers.Default) {
                focusPolicyService.evaluate(searchable)
            }
            if (!decision.requiresGate) {
                launchDirect(searchable, context, options)
                return@launch
            }
            favoritesService.upsert(searchable)

            gateLauncher.openGate(context, searchable, launchBounds)
        }
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
