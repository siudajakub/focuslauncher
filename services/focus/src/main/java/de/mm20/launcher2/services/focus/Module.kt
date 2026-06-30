package de.mm20.launcher2.services.focus

import de.mm20.launcher2.database.AppDatabase
import org.koin.dsl.module

val focusModule = module {
    factory { FocusAppClassifier(get()) }
    factory { FocusSessionRepository(get<AppDatabase>()) }
    factory { FocusHistoryRepository(get()) }
    factory { FocusPolicyService(get(), get(), get(), get(), get(), get()) }
    factory { (gateLauncher: FocusGateLauncher) ->
        FocusLaunchCoordinator(get(), get(), get(), gateLauncher)
    }
}
