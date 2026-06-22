## 2024-05-24 - Hoisting Regex compilations
**Learning:** Compiling regular expressions is a CPU-intensive operation. When used inside functions that are called frequently (like `onValueChange` in Compose or inside loops processing lists), inline `Regex("...")` causes repeated compilation and object allocation.
**Action:** Always hoist `Regex` compilation to top-level private constants (e.g., `private val MY_REGEX = Regex("...")`) to ensure they are compiled only once, improving UI responsiveness and reducing garbage collection overhead.
