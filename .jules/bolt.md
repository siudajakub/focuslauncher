## 2024-05-18 - Hoist Regex Compilation
**Learning:** Found inline `Regex` compilation inside frequent Jetpack Compose `onValueChange` callbacks in color pickers, which causes unnecessary recompilation on every keystroke.
**Action:** Always hoist `Regex` instances to top-level private properties or companion objects when used in frequently executing UI callbacks like `onValueChange`.
