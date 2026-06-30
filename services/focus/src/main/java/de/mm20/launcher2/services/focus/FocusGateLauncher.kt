package de.mm20.launcher2.services.focus

import android.content.Context
import de.mm20.launcher2.search.Application

/**
 * Plain domain representation of the on-screen bounds of the launched item, used to seed the
 * focus-gate reveal animation. Kept free of platform-graphics types so this contract can live in
 * `:services:focus` without depending on Compose or `app/ui`.
 */
data class LaunchBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

/**
 * Opens the focus gate for [app]. Implemented in `app/ui` (which owns `FocusGateActivity`) and
 * supplied to [FocusLaunchCoordinator] at construction time, inverting the dependency so the
 * coordinator no longer compiles against `app/ui`.
 */
interface FocusGateLauncher {
    fun openGate(context: Context, app: Application, bounds: LaunchBounds?)
}
