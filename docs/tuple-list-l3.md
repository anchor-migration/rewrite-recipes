# Tuple list → result class — language modernization L3 (ADR-008 M4)

**Status:** Accepted (ADR-008 M4)  
**Date:** 2026-06-29  
**Primary artifact:** YAML composite `com.anchor.migration.rewrite.lang.LanguageModernizationL3`  
**Recipe:** `TupleListToResultClass`  
**Preset:** `com.anchor.migration.presets.LanguageL3Only` (proposal mode)  
**Proof tests:** `TupleListToResultClassTest`  
**Fixture source:** Synthetic tuple producer (`demo.tuple.TupleFixture` style)

**Related:** [ADR-008](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-008-java-language-modernization-and-tuple-lists.md) · [homogeneous-raw-list-l2.md](homogeneous-raw-list-l2.md) · [classify-lists (M2)](https://github.com/anchor-migration/java-ast-ssot/blob/main/docs/list-usage-classifier.md)

---

## 1. Scope

| In scope | Out of scope |
|----------|--------------|
| Tuple `ArrayList` sites → dedicated `*Result` class (proposal + approved apply) | Homogeneous lists (L2) |
| Producer methods that `add()` mixed types and return the list | Consumer-only `get(i)`+cast refactors (future) |
| JSON proposal output with suggested class/field names | Fully unattended naming |
| Apply mode with human-approved class + field names | Inter-procedural tuple tracking |

**Modernization tier:** L3 — low automation; **human review required** before apply.

---

## 2. Workflow

1. Run `classify-lists` and confirm `usageClass: tuple` on target sites.
2. Run L3 in **proposal mode** (default): set `proposalOutputPath` on `TupleListToResultClass`.
3. Review JSON — edit class name and field names.
4. Save approved mapping JSON and re-run with `applyApproved=true` + `approvedProposalsPath`.

### Proposal output (example)

```json
{
  "proposals": [
    {
      "siteStableId": "demo.tuple.TupleFixture#transferFunds()#local:out",
      "suggestedClassName": "TransferFundsResult",
      "suggestedFieldNames": ["element0", "element1"],
      "slotTypes": ["String", "Integer"],
      "status": "pending_review"
    }
  ]
}
```

### Approved apply input (example)

```json
{
  "approved": {
    "demo.tuple.TupleFixture#transferFunds()#local:out": {
      "className": "TransferFundsResult",
      "fieldNames": ["code", "transactionId"]
    }
  }
}
```

---

## 3. Recipe options

| Option | Default | Purpose |
|--------|---------|---------|
| `proposalOutputPath` | _(unset)_ | Write JSON proposals when `applyApproved` is false |
| `approvedProposalsPath` | _(unset)_ | Human-approved class/field names per `siteStableId` |
| `applyApproved` | `false` | When true, transform sources using approved mapping |

---

## 4. Verification

| Check | Status |
|-------|--------|
| `LanguageModernizationL3` YAML | ✅ |
| `TupleListToResultClass` in SPI | ✅ |
| Proposal mode (no source changes) | ✅ |
| Approved apply on synthetic tuple fixture | ✅ |
| `PresetCatalogTest` resolves `LanguageL3Only` | ✅ |

Run: `.\scripts\run-test.ps1` (21 tests — full catalog)

---

## 5. Recommended run order

L1 → stack migration → L2 (homogeneous only) → **L3** (tuple sites only, after proposal review).

Do **not** run L2 on sites classified as `tuple`.
