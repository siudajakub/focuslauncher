## 2024-05-18 - Hoist Regex Compilation in Jetpack Compose
**Learning:** Recompiling a `Regex` on every keystroke inside a Compose `onValueChange` callback (e.g., in `OutlinedTextField`) causes unnecessary memory allocations and CPU overhead, creating a micro-stuttering bottleneck during rapid text input. This is a codebase-specific anti-pattern observed in the color pickers.
**Action:** Always hoist `Regex` compilation to top-level private properties or companion objects when they are used within frequent UI callbacks like `onValueChange`.
