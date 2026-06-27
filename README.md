# rewrite-recipes

Part of **[Anchor Migration](https://github.com/anchor-migration/migration-hub)** — OpenRewrite recipe catalog for legacy Java modernization.

> [ADR-007 — stack migration](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-007-rewrite-recipes-session-and-cmp-jpa.md)  
> [ADR-008 — language modernization](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-008-java-language-modernization-and-tuple-lists.md)

Deterministic **source transforms** using OpenRewrite LST at apply time ([ADR-003](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-003-ast-sidecar-vs-lst-rewrite-layer.md)). SSOT snapshots inform targeting; recipes do not mutate SQLite SSOT files.

## Status (0.1.0-alpha)

- [x] Maven + `rewrite-test` harness
- [x] Smoke recipe `AddAnchorProbeComment`
- [x] Duke's Bank–style raw-type fixture test (`OrderImports`)
- [ ] Session BeanState recipes (ADR-007 §3.2)
- [ ] CMP→JPA scalar entity (ADR-007 §3.3)
- [ ] L1 `Vector`→`ArrayList` (ADR-008 M1)

## Build & test

```bash
mvn test
```

Docker (no local JDK):

```bash
docker run --rm -v "$PWD:/app" -w /app maven:3.9-eclipse-temurin-17 mvn -B test
```

Windows:

```powershell
docker run --rm -v "C:/github/anchor-migration/rewrite-recipes:/app" -w /app maven:3.9-eclipse-temurin-17 mvn -B test
```

## Run smoke recipe on a project (after install)

```bash
mvn install -DskipTests
# In target project pom, depend on rewrite-recipes and activate AddAnchorProbeComment
mvn rewrite:run
```

## Layout

```
src/main/java/.../smoke/     Harness recipes
src/test/java/.../           rewrite-test fixtures
docs/recipe-families.md      Family + tier registry
rewrite.yml                  Active recipe specs
```

## License

MIT
