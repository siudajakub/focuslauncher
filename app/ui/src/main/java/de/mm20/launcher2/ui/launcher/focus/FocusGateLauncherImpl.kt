package de.mm20.launcher2.ui.launcher.focus

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.IntRect
import de.mm20.launcher2.search.Application
import de.mm20.launcher2.services.focus.FocusGateLauncher
import de.mm20.launcher2.services.focus.LaunchBounds

/** Converts Compose [IntRect] launch bounds into the platform-free [LaunchBounds] domain type. */
fun IntRect.toLaunchBounds(): LaunchBounds = LaunchBounds(left, top, right, bottom)

/**
 * App/ui implementation of [FocusGateLauncher]. This is the only class that references
 * [FocusGateActivity], so it carries the previously-circular edge from `:services:focus` back into
 * `app/ui` at the construction boundary instead of at compile time.
 */
class FocusGateLauncherImpl : FocusGateLauncher {
    override fun openGate(context: Context, app: Application, bounds: LaunchBounds?) {
        val intent = FocusGateActivity.intent(context, app, bounds)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
