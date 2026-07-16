package de.mm20.launcher2.services.focus

import de.mm20.launcher2.database.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val focusModule = module {
    factory { FocusAppClassifier(get()) }
    factory { FocusSessionRepository(get<AppDatabase>()) }
    factory { FocusHistoryRepository(get()) }
    // Single instance so its cache is actually shared between Focus Home and Focus Insights.
    single { FocusUsageStatsRepository(androidContext()) }
    factory { FocusPolicyService(get(), get(), get(), get(), get(), get()) }
    factory { (gateLauncher: FocusGateLauncher) ->
        FocusLaunchCoordinator(get(), get(), get(), gateLauncher)
    }
}
