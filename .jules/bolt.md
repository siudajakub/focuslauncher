## 2024-06-29 - Regex Hoisting

**Learning:** `Regex` objects were being instantiated inline within frequent UI callbacks (e.g., `onValueChange` in Compose) and recurrent string manipulation functions (e.g., `String.replace`). This creates unnecessary CPU overhead and memory allocations on every event.
**Action:** Always hoist `Regex` compilation to top-level `private val` properties or companion objects so they are compiled exactly once, drastically reducing overhead on hot paths.
