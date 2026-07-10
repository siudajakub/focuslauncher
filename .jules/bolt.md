## 2024-07-10 - Regex Recompilation Optimization
**Learning:** Found multiple instances where `Regex` instances were instantiated inline within functions that are frequently called (e.g. `UnitConverterRepository.queryUnitConverter()` on every search query, and `File.getFileType()` which may run on large lists of files). Recompiling regular expressions repeatedly causes unnecessary CPU overhead.
**Action:** Always check for inline `Regex` creations in functions that run frequently. Hoist them to top-level private properties to compile the regex only once.
