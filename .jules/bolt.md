## 2024-05-15 - [Top-Level Regex Compilation]
**Learning:** Recompiling a `Regex` on every UI input change or frequent string processing function causes performance degradation, especially in a Kotlin/Android codebase where the main thread is critical.
**Action:** Always hoist `Regex` instances to top-level properties or companion objects when they are static and reused.
