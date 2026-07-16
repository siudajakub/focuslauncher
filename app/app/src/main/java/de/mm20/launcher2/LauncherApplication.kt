package de.mm20.launcher2

import android.app.Application
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
            focusPolicyService.reconcileFocusSession(this@LauncherApplication)
        }
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
}
