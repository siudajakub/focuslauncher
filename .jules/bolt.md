## 2024-07-04 - Optimize UnitConverterRepositoryImpl instantiation and regex evaluation
**Learning:** Repetitive instantiation of stateless objects (`MassConverter`, `LengthConverter`, etc.) and `Regex` compilation inside frequent evaluation callbacks (`queryUnitConverter` during search queries) adds unnecessary allocation and CPU overhead overhead in Kotlin/Android.
**Action:** Always hoist `Regex` compilation to companion objects/top-level and cache stateless object instances using lazy initialization or pre-initialized properties instead of recreating them on every method invocation.
