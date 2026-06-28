# rewrite-recipes



Part of **[Anchor Migration](https://github.com/anchor-migration/migration-hub)** — OpenRewrite recipe catalog for legacy Java modernization.



> [ADR-007 — stack migration](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-007-rewrite-recipes-session-and-cmp-jpa.md)  

> [ADR-008 — language modernization](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-008-java-language-modernization-and-tuple-lists.md)



Deterministic **source transforms** using OpenRewrite LST at apply time ([ADR-003](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-003-ast-sidecar-vs-lst-rewrite-layer.md)). SSOT snapshots inform targeting; recipes do not mutate SQLite SSOT files.



## Status (0.1.0-alpha)



- [x] Maven + `rewrite-test` harness

- [x] Smoke recipe `AddAnchorProbeComment`

- [x] Duke's Bank–style raw-type fixture test (`OrderImports`)
- [x] CMP→JPA capability matrix (3.1a) — [docs/cmp-jpa-capability-matrix.md](docs/cmp-jpa-capability-matrix.md)
- [x] Session `BeanState` spike (3.1b) — [docs/session-beanstate-spike-account-controller.md](docs/session-beanstate-spike-account-controller.md)
- [x] Session→Service recipe chain (3.2) — [docs/session-bean-to-spring-service-account-controller.md](docs/session-bean-to-spring-service-account-controller.md)
- [x] CMP→JPA scalar entity (3.3) — [docs/cmp-scalar-entity-to-jpa-account-bean.md](docs/cmp-scalar-entity-to-jpa-account-bean.md)
- [x] L1 `Vector`→`ArrayList` (ADR-008 M1) — [docs/vector-to-arraylist-l1.md](docs/vector-to-arraylist-l1.md)



## Quick start (Docker — recommended)



**Host requirements:** [Docker Desktop](https://docs.docker.com/desktop/) (or Docker Engine + Compose).  

**Not required on the host:** JDK, Maven, OpenRewrite CLI, or any global Java toolchain.



```powershell

git clone https://github.com/anchor-migration/rewrite-recipes.git

cd rewrite-recipes

.\scripts\run-test.ps1

```



Linux / macOS:



```bash

git clone https://github.com/anchor-migration/rewrite-recipes.git

cd rewrite-recipes

chmod +x scripts/run-test.sh

./scripts/run-test.sh

```



Equivalent one-liner (any OS):



```bash

docker compose run --rm mvn mvn -B test

```



First run downloads the Maven image and dependencies (~1–2 min). Later runs reuse the named volume `anchor-rewrite-recipes-m2` — nothing is installed into your host `JAVA_HOME` or `~/.m2` unless you choose to mount it.



### Other Maven goals



```powershell

# Install jar to container-local target/ (still no host JDK)

.\scripts\run-mvn.ps1 -MavenArgs @("-B", "install", "-DskipTests")



# Single test class

.\scripts\run-mvn.ps1 -MavenArgs @("-B", "test", "-Dtest=AddAnchorProbeCommentTest")

```



```bash

./scripts/run-mvn.sh -B install -DskipTests

./scripts/run-mvn.sh -B test -Dtest=AddAnchorProbeCommentTest

```



Interactive shell inside the toolchain container:



```bash

docker compose run --rm mvn bash

# then: mvn -B test

```



## Local JDK (optional)



If you already have **JDK 17** and **Maven 3.9+** and prefer native builds:



```bash

mvn test

```



Use this only when you intentionally want a host toolchain. For trials and CI parity with “clean machine”, prefer the Docker scripts above.



## What runs in the container



| Item | Value |

|------|--------|

| Image | `maven:3.9-eclipse-temurin-17` |

| JDK | 17 (Eclipse Temurin) |

| OpenRewrite BOM | 8.85.6 |

| Maven cache | Docker volume `anchor-rewrite-recipes-m2` |



Source and `target/` are bind-mounted from the repo checkout. Only Maven dependencies live in the named volume.



## Run smoke recipe on another project



After `install`, depend on this artifact from a target project and activate `AddAnchorProbeComment` in that project's `rewrite-maven-plugin` config. Build/install via Docker:



```powershell

.\scripts\run-mvn.ps1 -MavenArgs @("-B", "install", "-DskipTests")

```



Then in the target repo (host or its own container): `mvn rewrite:run`.



## Layout



```

scripts/                     Docker entrypoints (run-test, run-mvn)

docker-compose.yml           Maven service + dependency cache volume

src/main/java/.../smoke/     Harness recipes

src/test/java/.../           rewrite-test fixtures

docs/recipe-families.md      Family + tier registry

rewrite.yml                  Active recipe specs

```



## CI



GitHub Actions runs `mvn -B test` on Ubuntu with Temurin 17 (isolated runner — same JDK/Maven versions as the Docker image). Local Docker workflow is the recommended path for contributors who want zero host Java setup.



## License



MIT

