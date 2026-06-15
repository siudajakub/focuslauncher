package de.mm20.launcher2.ui.launcher.focus.plan

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import de.mm20.launcher2.ui.base.BaseActivity
import de.mm20.launcher2.ui.base.ProvideCompositionLocals
import de.mm20.launcher2.ui.overlays.OverlayHost
import de.mm20.launcher2.ui.settings.SettingsActivity
import de.mm20.launcher2.ui.theme.LauncherTheme

class FocusPlanActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        setContent {
            LauncherTheme {
                ProvideCompositionLocals {
                    OverlayHost {
                        FocusPlanScreen(
                            onNavigateUp = { finish() },
                            onOpenTodoistSettings = {
                                startActivity(Intent(this@FocusPlanActivity, SettingsActivity::class.java).apply {
                                    putExtra(SettingsActivity.EXTRA_ROUTE, SettingsActivity.ROUTE_FOCUS_INTEGRATIONS)
                                })
                            },
                            onOpenPlanSettings = {
                                startActivity(Intent(this@FocusPlanActivity, SettingsActivity::class.java).apply {
                                    putExtra(SettingsActivity.EXTRA_ROUTE, SettingsActivity.ROUTE_FOCUS_SETTINGS)
                                })
                            }
                        )
                    }
                }
            }
        }
    }
}
