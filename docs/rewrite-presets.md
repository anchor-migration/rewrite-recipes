# Rewrite presets (ADR-009)

**Status:** Accepted  
**Related:** [ADR-009](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-009-rewrite-engine-presets-and-run-manifest.md) · [recipe-families.md](recipe-families.md)

Presets are **ordered recipe chains** declared in YAML under `src/main/resources/META-INF/rewrite/presets/`. They are the primary way to run Anchor migrations — not hardcoded `activeRecipes` in consumer projects.

---

## Three layers

| Layer | Location | Examples |
|-------|----------|----------|
| **Upstream** | OpenRewrite BOM / `rewrite-migrate-java` | `org.openrewrite.java.ChangeType` |
| **Anchor recipes** | Java + SPI + `META-INF/rewrite/*.yml` | `SessionBeanToSpringService`, `LanguageModernizationL1` |
| **Presets** | `META-INF/rewrite/presets/*.yml` | `DukesBankStackMigration` |

---

## Available presets

| Preset name | Purpose |
|-------------|---------|
| `com.anchor.migration.presets.Smoke` | Default CI — harness probe only |
| `com.anchor.migration.presets.LanguageL1Only` | ADR-008 L1 mechanical API swaps |
| `com.anchor.migration.presets.DukesBankStackMigration` | L1 → Session→Service → CMP→JPA (Duke's Bank Phase D) |

---

## Activate a preset

### In this repo (`rewrite-maven-plugin`)

Property `anchor.rewrite.preset` defaults to `Smoke`. Override at run time:

```powershell
.\scripts\run-mvn.ps1 -Preset com.anchor.migration.presets.DukesBankStackMigration `
  -MavenArgs @("-B", "rewrite:run")
```

```bash
./scripts/run-mvn.sh -Preset com.anchor.migration.presets.LanguageL1Only -B rewrite:run
```

Or pass the property directly:

```bash
mvn -B rewrite:run -Danchor.rewrite.preset=com.anchor.migration.presets.DukesBankStackMigration
```

### In a target project

After depending on `rewrite-recipes`:

```xml
<properties>
  <anchor.rewrite.preset>com.anchor.migration.presets.DukesBankStackMigration</anchor.rewrite.preset>
</properties>
<plugin>
  <groupId>org.openrewrite.maven</groupId>
  <artifactId>rewrite-maven-plugin</artifactId>
  <configuration>
    <activeRecipes>
      <recipe>${anchor.rewrite.preset}</recipe>
    </activeRecipes>
  </configuration>
</plugin>
```

---

## YAML composites vs Java wrappers

Prefer YAML `recipeList` composites for mechanical chains (e.g. `LanguageModernizationL1`). Java recipe classes remain for domain logic (BeanState, CMP→JPA) and narrow unit tests.

`VectorToArrayList` (Java) is a legacy single-type wrapper; new work should reference `LanguageModernizationL1` or `LanguageL1Only` preset.

---

## Verification

| Check | Test |
|-------|------|
| L1 YAML composite | `LanguageModernizationL1Test` |
| All presets resolve | `PresetCatalogTest` |
| Duke's Bank stack order | `PresetCatalogTest#dukesBankStackPresetHasOrderedChainLength` |

Run: `.\scripts\run-test.ps1`
