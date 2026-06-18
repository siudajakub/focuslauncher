## 2026-06-18 - [Hoist Regex in Compose UI Callbacks]
**Learning:** Compiling Regex inside a high-frequency Compose callback like `onValueChange` forces the engine to reconstruct the Regex object on every keystroke, which causes micro-stutters. Hoisting the Regex compilation to a top-level constant or a companion object prevents these unnecessary allocations and keeps text input smooth.
**Action:** Always extract `Regex(...)` to a top-level `val` when it's used inside Composables, particularly inside loops or frequent event listeners like `onValueChange` or `onDrag`.
