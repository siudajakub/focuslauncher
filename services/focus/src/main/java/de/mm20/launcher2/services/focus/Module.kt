package de.mm20.launcher2.services.focus

import org.koin.dsl.module

val focusModule = module {
    factory { FocusAppClassifier(get()) }
    factory { FocusSessionRepository(get()) }
    factory { FocusHistoryRepository(get()) }
    factory { FocusPolicyService(get(), get(), get(), get(), get(), get()) }
}
