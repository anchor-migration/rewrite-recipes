# Recipe families (ADR-007 / ADR-008)

Each recipe declares metadata in `rewrite.yml` and/or Java `@Option` fields.

## Families

| `recipeFamily` | ADR | Purpose |
|----------------|-----|---------|
| `stack-migration` | [ADR-007](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-007-rewrite-recipes-session-and-cmp-jpa.md) | EJB Session→Service (BeanState), CMP→JPA |
| `language-modernization` | [ADR-008](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-008-java-language-modernization-and-tuple-lists.md) | L1/L2/L3 — Vector, generics, tuple lists |

## Smoke (3.0)

| Recipe | Family | Tier |
|--------|--------|------|
| `AddAnchorProbeComment` | harness | — |

## Stack migration — CMP→JPA (ADR-007)

| Artifact | Status |
|----------|--------|
| [cmp-jpa-capability-matrix.md](cmp-jpa-capability-matrix.md) | ✅ 3.1a |
| [session-beanstate-spike-account-controller.md](session-beanstate-spike-account-controller.md) | ✅ 3.1b |
| [session-bean-to-spring-service-account-controller.md](session-bean-to-spring-service-account-controller.md) | ✅ 3.2 |
| [cmp-scalar-entity-to-jpa-account-bean.md](cmp-scalar-entity-to-jpa-account-bean.md) | ✅ 3.3 |
| [cmp-scalar-entity-to-jpa-tx-bean.md](cmp-scalar-entity-to-jpa-tx-bean.md) | ✅ v0.4c |
| [cmp-scalar-entity-to-jpa-customer-bean.md](cmp-scalar-entity-to-jpa-customer-bean.md) | ✅ v0.4c |
| `CmpManyToManyToJpa` | ✅ v0.4a |
| `CmpForeignKeyToJpa` | ✅ v0.4b |

## Presets (ADR-009)

| Preset | Chain |
|--------|-------|
| `com.anchor.migration.presets.Smoke` | Harness probe (default CI) |
| `com.anchor.migration.presets.LanguageL1Only` | `LanguageModernizationL1` YAML composite |
| `com.anchor.migration.presets.LanguageL2Only` | `LanguageModernizationL2` — homogeneous raw `ArrayList` typing |
| `com.anchor.migration.presets.LanguageL3Only` | `LanguageModernizationL3` — tuple list proposals (default: no source changes) |
| `com.anchor.migration.presets.DukesBankStackMigration` | L1 → Session→Service → CMP→JPA |

Details: [rewrite-presets.md](rewrite-presets.md)

## Language modernization (ADR-008)

| Artifact | Status |
|----------|--------|
| [vector-to-arraylist-l1.md](vector-to-arraylist-l1.md) | ✅ M1 / L1 (prefer `LanguageModernizationL1` YAML) |
| [classify-lists (java-ast-ssot)](https://github.com/anchor-migration/java-ast-ssot/blob/main/docs/list-usage-classifier.md) | ✅ M2 — run before L2/L3 |
| [homogeneous-raw-list-l2.md](homogeneous-raw-list-l2.md) | ✅ M3 / L2 (`LanguageModernizationL2`, `LanguageL2Only`) |
| [tuple-list-l3.md](tuple-list-l3.md) | ✅ M4 / L3 (`LanguageModernizationL3`, `LanguageL3Only`) |

## Recommended run order

Use preset **`com.anchor.migration.presets.DukesBankStackMigration`** for the full Duke's Bank stack, or compose manually:

1. **L1** — `LanguageL1Only` preset (or `LanguageModernizationL1`)
2. **Stack** — session + CMP recipes
3. **L2** — `LanguageL2Only` after [`classify-lists`](https://github.com/anchor-migration/java-ast-ssot/blob/main/docs/list-usage-classifier.md) report (ADR-008 M2 ✅)
4. **L3** — `LanguageL3Only` proposal, then approved apply for tuple sites
