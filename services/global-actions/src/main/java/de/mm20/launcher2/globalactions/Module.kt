package de.mm20.launcher2.globalactions

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import de.mm20.launcher2.services.focus.FocusForegroundController

val globalActionsModule = module {
    single { GlobalActionsService(androidContext()) }
    single<FocusForegroundController> { get<GlobalActionsService>() }
}
