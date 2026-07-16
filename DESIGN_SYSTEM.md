# FocusLauncher Design System

FocusLauncher uses the inherited Compose theme and component library to present a calm,
information-efficient launcher. New UI should fit the existing hierarchy instead of introducing a
second visual system.

## Activities And Theming

Activities in `:app:ui` inherit from `BaseActivity` (or reproduce its permission integration),
enable edge-to-edge rendering, and wrap Compose content in `LauncherTheme` and the composition
locals needed by the surface. Use `OverlayHost` when the flow can display launcher overlays.

```kotlin
package de.mm20.launcher2.ui.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import de.mm20.launcher2.ui.base.BaseActivity
import de.mm20.launcher2.ui.base.ProvideCompositionLocals
import de.mm20.launcher2.ui.overlays.OverlayHost
import de.mm20.launcher2.ui.theme.LauncherTheme

class ExampleActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        setContent {
            LauncherTheme {
                ProvideCompositionLocals {
                    OverlayHost { ExampleScreen() }
                }
            }
        }
    }
}
```

Classes instantiated with Compose `viewModel()` must be visible to the generated factory; do not
mark them `private`.

## Components And Layout

- Reuse `PreferenceScreen`, `PreferenceCategory`, shared preference rows, launcher sheets, and
  existing launcher cards before creating new containers.
- Use `MaterialTheme.colorScheme`, typography, shapes, and spacing. Do not hardcode user-facing
  colors or assume a light wallpaper.
- Keep primary actions rare. Prefer `FilledTonalButton` or `OutlinedButton` for secondary or
  reversible actions.
- Use animation only when it explains state or continuity. Respect reduced-motion/system animation
  settings and avoid decorative motion in focus-critical flows.
- Keep focus gates, home cards, and settings concise; progressive disclosure is preferable to a
  dense wall of controls.

## Copy And Internationalization

- Canonical English copy belongs in `core/i18n/src/main/res/values/strings.xml`.
- Compose uses `stringResource`/`pluralStringResource`; no hardcoded user-facing strings.
- Copy should explain what changed, why an app is blocked, and how the user can safely reverse a
  choice without shame or urgency.

## Focus-Specific Rules

- Essential apps do not inherit distracting-app friction or visual de-emphasis.
- All app launches go through the shared focus launch coordinator unless an explicit, documented
  integration exception applies.
- Hiding, fading, and no-icons behavior must respect the focus master state and use the shared
  classification source.
- The launcher never changes display saturation, its own or the device's. Calm comes from what is
  shown, not from draining colour the user chose.
- Settings that require Usage Access, notification policy access, or notifications must state the
  dependency and degrade safely when it is missing.

## UI Verification

Check light/dark content, wallpaper contrast, system bars, font scaling, RTL, navigation/back,
permission-denied states, and the relevant Pixel smoke path. For rendering changes that may affect
GPU cost (blur, large layers), compare a device trace with the feature on and off before claiming a
performance result.
