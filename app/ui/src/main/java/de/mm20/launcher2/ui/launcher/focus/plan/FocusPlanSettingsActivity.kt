package de.mm20.launcher2.ui.launcher.focus.plan

import android.os.Bundle
import androidx.activity.compose.setContent
import de.mm20.launcher2.ui.base.BaseActivity
import de.mm20.launcher2.ui.theme.LauncherTheme

class FocusPlanSettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LauncherTheme {
                FocusPlanSettingsScreen(
                    onNavigateUp = { finish() },
                )
            }
        }
    }
}
