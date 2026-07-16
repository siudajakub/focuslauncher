package de.mm20.launcher2

import android.app.Application
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import de.mm20.launcher2.applications.applicationsModule
import de.mm20.launcher2.appshortcuts.appShortcutsModule
import de.mm20.launcher2.backup.backupModule
import de.mm20.launcher2.badges.badgesModule
import de.mm20.launcher2.calendar.calendarModule
import de.mm20.launcher2.data.customattrs.customAttrsModule
import de.mm20.launcher2.data.i18nDataModule
import de.mm20.launcher2.searchable.searchableModule
import de.mm20.launcher2.icons.iconsModule
import de.mm20.launcher2.music.musicModule
import de.mm20.launcher2.search.searchModule
import de.mm20.launcher2.widgets.widgetsModule
import de.mm20.launcher2.database.databaseModule
import de.mm20.launcher2.debug.initDebugMode
import de.mm20.launcher2.globalactions.globalActionsModule
import de.mm20.launcher2.notifications.notificationsModule
import de.mm20.launcher2.permissions.permissionsModule
import de.mm20.launcher2.preferences.preferencesModule
import de.mm20.launcher2.profiles.profilesModule
import de.mm20.launcher2.services.favorites.favoritesModule
import de.mm20.launcher2.services.tags.servicesTagsModule
import de.mm20.launcher2.services.widgets.widgetsServiceModule
import de.mm20.launcher2.themes.themesModule
import de.mm20.launcher2.services.focus.FocusPolicyService
import de.mm20.launcher2.services.focus.focusModule
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import kotlin.coroutines.CoroutineContext

class LauncherApplication : Application(), CoroutineScope, ImageLoaderFactory {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + SupervisorJob()

    private val focusPolicyService: FocusPolicyService by inject()

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.BUILD_TYPE == "debug") initDebugMode()

        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@LauncherApplication)
            modules(
                listOf(
                    applicationsModule,
                    appShortcutsModule,
                    baseModule,
                    badgesModule,
                    calendarModule,
                    customAttrsModule,
                    databaseModule,
                    favoritesModule,
                    searchableModule,
                    globalActionsModule,
                    iconsModule,
                    musicModule,
                    notificationsModule,
                    permissionsModule,
                    preferencesModule,
                    searchModule,
                    themesModule,
                    widgetsModule,
                    servicesTagsModule,
                    widgetsServiceModule,
                    backupModule,
                    profilesModule,
                    i18nDataModule,
                    focusModule,
                )
            )
        }

        launch {
            cancelRemovedFeatureWork()
            focusPolicyService.reconcileFocusSession(this@LauncherApplication)
        }
    }

    /**
     * Cancels the periodic work of the removed weather and currency-converter features.
     *
     * Deleting the worker classes was not enough: WorkManager persists enqueued work in its own
     * database, which survives the upgrade, so on an existing install those jobs stay scheduled
     * and now wake the device every hour only to fail instantiating a class that no longer exists
     * and retry on backoff. Cancelling by unique name is the only way to reach them.
     *
     * Safe to call repeatedly — cancelling an unknown name is a no-op — and safe to delete once no
     * install can still be upgrading from a build that had these features.
     */
    private fun cancelRemovedFeatureWork() {
        val workManager = WorkManager.getInstance(this)
        workManager.cancelUniqueWork(OBSOLETE_EXCHANGE_RATE_WORK)
        workManager.cancelUniqueWork(OBSOLETE_WEATHER_WORK)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(applicationContext)
            .components {
                add(SvgDecoder.Factory())
            }
            .crossfade(true)
            .crossfade(200)
            .build()
    }

    private companion object {
        // The unique names the deleted CurrencyRepository and WeatherRepositoryImpl enqueued under.
        const val OBSOLETE_EXCHANGE_RATE_WORK = "ExchangeRates"
        const val OBSOLETE_WEATHER_WORK = "weather"
    }
}
