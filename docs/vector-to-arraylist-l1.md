# Vector to ArrayList — language modernization L1 (ADR-008 M1)

**Status:** Accepted (ADR-008 M1)  
**Date:** 2026-06-28  
**Recipe:** `VectorToArrayList` (delegates to OpenRewrite `ChangeType`)  
**Proof test:** `VectorToArrayListTest`  
**Fixture source:** Duke's Bank `CartBean` (`C:\github\dukesbank\...\ejb\cart\src\CartBean.java`)

**Related:** [ADR-008](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-008-java-language-modernization-and-tuple-lists.md) · [recipe-families.md](recipe-families.md)

---

## 1. Scope

| In scope | Out of scope |
|----------|--------------|
| `java.util.Vector` → `java.util.ArrayList` in types, fields, parameters, return types | Adding generic type parameters (L2) |
| `new Vector(...)` → `new ArrayList(...)` | `Hashtable`, `StringBuffer` (future L1 recipes) |
| Import rewrite (`Vector` → `ArrayList`) | Tuple-list detection (M2) |
| Fully qualified or simple names | Thread-safety semantics review |

**Modernization tier:** L1 — mechanical API swap only.

---

## 2. Recommended run order

Per ADR-008: run **L1 before stack migration recipes** on the same compilation units when possible.

---

## 3. Verification

| Check | Status |
|-------|--------|
| `VectorToArrayList` registered in `META-INF/services` | ✅ |
| `VectorToArrayListTest` (Cart-style + FQN) | ✅ (`.\scripts\run-test.ps1`) |

---

## Changelog

| Date | Change |
|------|--------|
| 2026-06-28 | Initial M1 / L1 recipe |
