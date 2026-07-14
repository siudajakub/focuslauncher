## 2024-05-14 - Optimize UnitConverter Regex Compilation
**Learning:** In Kotlin, creating `Regex` objects locally inside functions called frequently (like `search(query)`) incurs repeated parsing and compilation overhead. This is especially true for search handlers or input validation blocks where queries may stream rapidly.
**Action:** Always hoist `Regex` compilation to top-level properties or `companion object`s, particularly when used in frequent UI callbacks, search operations, or `onValueChange` listeners.
