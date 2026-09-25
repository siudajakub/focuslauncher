package de.mm20.launcher2.services.focus

import android.content.Context
import de.mm20.launcher2.database.AppDatabase
import org.koin.dsl.module

val focusModule = module {
    factory { FocusAppClassifier(get()) }
    factory { FocusSessionRepository(get<AppDatabase>()) }
    factory { FocusHistoryRepository(get()) }
    factory { FocusPolicyService(get(), get(), get(), get(), get(), get()) }
    factory { FocusLaunchCoordinator(get(), get(), get(), get()) }
    single { FocusSystemInterceptionService(get<Context>(), get(), get(), get(), get(), get()) }
}
