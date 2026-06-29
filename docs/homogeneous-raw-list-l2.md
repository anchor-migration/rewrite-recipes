# Homogeneous raw ArrayList typing — language modernization L2 (ADR-008 M3)

**Status:** Accepted (ADR-008 M3)  
**Date:** 2026-06-29  
**Primary artifact:** YAML composite `com.anchor.migration.rewrite.lang.LanguageModernizationL2`  
**Recipe:** `HomogeneousRawListTyping`  
**Preset:** `com.anchor.migration.presets.LanguageL2Only`  
**Proof tests:** `HomogeneousRawListTypingTest`, `ListUsageReportReaderTest`  
**Fixture source:** Duke's Bank `AccountControllerBean` style (`getAccountsOfCustomer` / `copyAccountsToDetails`)

**Related:** [ADR-008](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-008-java-language-modernization-and-tuple-lists.md) · [classify-lists (M2)](https://github.com/anchor-migration/java-ast-ssot/blob/main/docs/list-usage-classifier.md) · [vector-to-arraylist-l1.md](vector-to-arraylist-l1.md) · [rewrite-presets.md](rewrite-presets.md)

---

## 1. Scope

| In scope | Out of scope |
|----------|--------------|
| Raw `ArrayList` → `ArrayList<E>` on **homogeneous** sites | Tuple lists (`add` / cast evidence disagrees) |
| Method return types, local variable types, `new ArrayList<>()` initializers | `Vector` / `Hashtable` (L1) |
| Inline LST analysis (mirrors M2 `classify-lists` rules) | Inter-procedural analysis beyond return-type propagation |
| Optional gate via `--analysis-report` JSON from `classify-lists` | L3 tuple → result class (M4) |

**Modernization tier:** L2 — adds generic type parameters where add/cast evidence agrees on one element type.

**Fail-closed:** `failOnTupleList` defaults to `true`. Tuple and unknown sites are skipped.

---

## 2. Recommended run order

1. **L1** — `LanguageL1Only` (Vector → ArrayList, etc.)
2. **Optional but recommended** — `java-ast-ssot classify-lists` on target sources; review JSON report
3. **L2** — `LanguageL2Only` preset (or `LanguageModernizationL2` composite)

When `analysisReportPath` is set on `HomogeneousRawListTyping`, only sites listed as `homogeneous` in the report are typed. Tuple sites in the report are always excluded when `failOnTupleList` is true.

---

## 3. Recipe options

| Option | Default | Purpose |
|--------|---------|---------|
| `analysisReportPath` | _(unset)_ | Optional JSON from `classify-lists`; gates transforms to reported homogeneous sites |
| `failOnTupleList` | `true` | Skip tuple/unknown sites instead of guessing |

---

## 4. Verification

| Check | Status |
|-------|--------|
| `LanguageModernizationL2` YAML in `META-INF/rewrite/` | ✅ |
| `HomogeneousRawListTyping` in SPI | ✅ |
| `HomogeneousRawListTypingTest` (Duke's Bank homogeneous + tuple no-op) | ✅ |
| `ListUsageReportReaderTest` | ✅ |
| `PresetCatalogTest` resolves `LanguageL2Only` | ✅ |

Run: `.\scripts\run-test.ps1` (14 tests)

---

## 5. Next

ADR-008 M4 — L3 tuple list → result class (proposal-only recipe + human review).
