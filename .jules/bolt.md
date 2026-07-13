## 2024-05-01 - UnitConverterRepositoryImpl hot path optimizations
**Learning:** `Regex` object compilation and frequent instantiation of converters in `queryUnitConverter` cause significant overhead since it is called on every keystroke during searches.
**Action:** Always hoist `Regex` instances to companion objects and cache stateless objects like converters using `lazy` initialization or properties to avoid reallocation on hot paths.
