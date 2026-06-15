# Kvaesitso Focus Fork: Design System & Guidelines

This document outlines the standard UI patterns, thematic guidelines, and architectural rules for this fork. All AI agents and contributors must strictly adhere to these guidelines to ensure UI consistency and code quality.

## 1. Activities and Theming

All UI activities in the `:app:ui` module must inherit from `BaseActivity` (or apply the same composition locals and EdgeToEdge configuration) and wrap their Compose content with the standard `LauncherTheme` and `OverlayHost`.

**Correct Activity Structure:**
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
                    OverlayHost {
                        // Your Main Screen Composable here
                        ExampleScreen()
                    }
                }
            }
        }
    }
}
```

## 2. UI Components & Layouts

- **Do NOT use raw Material 3 `Scaffold` directly if `MainSettingsScreen` or standard `FocusSection` can be used.**
- If building a custom full-screen flow, use standard Material 3 components but always rely on `MaterialTheme.colorScheme` provided by `LauncherTheme`.
- Ensure dark mode support: do not hardcode colors like `Color.White` or `Color.Black`. Use `MaterialTheme.colorScheme.surface`, `onSurface`, `primary`, etc.
- **Animations:** Use `AnimatedVisibility` for list items, dialogs, and dynamic content. Use `fadeIn()`, `fadeOut()`, `slideInVertically()`, `slideOutVertically()`, and `animateContentSize()` modifier to give the app a polished, native feel.

## 3. Internationalization (I18N)

- **NEVER hardcode strings** directly in Compose code (e.g., `Text("Planowanie")`).
- Always define strings in `core/i18n/src/main/res/values/strings.xml` and use `stringResource(R.string.key)`.
- Keys should follow the naming convention: `feature_name_description` (e.g., `focus_plan_title`, `focus_home_agenda_title`).

## 4. Feature specific UI rules (Focus Mode)

- Focus components should follow a minimalist, low-distraction design.
- Use `FilledTonalButton` or `OutlinedButton` instead of solid primary buttons when actions are secondary or shouldn't draw immediate attention.
- When creating sheets/dialogs, use `LauncherBottomSheet` or standard Material 3 `AlertDialog`.

## 5. Summary Check-list before finishing a UI task:

1. Did I use `LauncherTheme`?
2. Did I use `strings.xml` for all text?
3. Did I inherit from `BaseActivity`?
4. Are my colors adapting to dark mode?
5. Did I add entry/exit animations for dynamic content?
