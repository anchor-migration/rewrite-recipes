# Vector to ArrayList — language modernization L1 (ADR-008 M1)

**Status:** Accepted (ADR-008 M1)  
**Date:** 2026-06-28  
**Primary artifact:** YAML composite `com.anchor.migration.rewrite.lang.LanguageModernizationL1`  
**Legacy Java wrapper:** `VectorToArrayList` (single-type tests / SPI)  
**Preset:** `com.anchor.migration.presets.LanguageL1Only`  
**Proof tests:** `LanguageModernizationL1Test`, `VectorToArrayListTest`  
**Fixture source:** Duke's Bank `CartBean` style (`ejb/cart`)

**Related:** [ADR-008](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-008-java-language-modernization-and-tuple-lists.md) · [rewrite-presets.md](rewrite-presets.md) · [recipe-families.md](recipe-families.md)

---

## 1. Scope

| In scope | Out of scope |
|----------|--------------|
| `java.util.Vector` → `java.util.ArrayList` | Adding generic type parameters (L2) |
| `java.util.Hashtable` → `java.util.HashMap` (via L1 composite) | L2 generic typing (M3) |
| `java.lang.StringBuffer` → `java.lang.StringBuilder` (via L1 composite) | Thread-safety semantics review |
| Import rewrite for the above | Tuple-list gate — use [`classify-lists`](https://github.com/anchor-migration/java-ast-ssot/blob/main/docs/list-usage-classifier.md) (M2 ✅) before L2 |

**Modernization tier:** L1 — mechanical API swap only. Use **`LanguageModernizationL1`** (not the Java wrapper alone) for full L1 coverage.

---

## 2. Recommended run order

Per ADR-008: run **`LanguageL1Only`** preset before stack migration presets on the same compilation units.

---

## 3. Verification

| Check | Status |
|-------|--------|
| `LanguageModernizationL1` YAML in `META-INF/rewrite/` | ✅ |
| `LanguageModernizationL1Test` | ✅ |
| `VectorToArrayList` still in SPI (legacy) | ✅ |
| `PresetCatalogTest` resolves `LanguageL1Only` | ✅ |

Run: `.\scripts\run-test.ps1`

---

## 4. Next

ADR-008 M3 — L2 homogeneous recipe; run [`classify-lists`](https://github.com/anchor-migration/java-ast-ssot/blob/main/docs/list-usage-classifier.md) first and skip `tuple` sites.
