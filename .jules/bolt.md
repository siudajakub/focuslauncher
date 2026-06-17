## 2024-06-17 - Hoist Regex Compilation in Compose Callbacks
**Learning:** In Jetpack Compose, expensive object creations like compiling a `Regex` pattern directly inside frequent UI callbacks such as `onValueChange` can cause performance degradation, as these callbacks run on every keystroke.
**Action:** Always hoist `Regex` compilation to top-level private properties or companion objects, especially when used in frequent UI callbacks.
