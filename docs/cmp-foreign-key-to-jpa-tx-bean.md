# CMP ForeignKey to JPA (TxBean account)

**Recipe:** `CmpForeignKeyToJpa`  
**ADR:** [ADR-007 §v0.4b](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-007-rewrite-recipes-session-and-cmp-jpa.md)  
**Pattern:** `cmp-foreign-key-to-jpa-tx-bean` in [pattern-catalog](https://github.com/anchor-migration/pattern-catalog)

## Scope

Runs **after** `CmpScalarEntityToJpa` on an existing `@Entity` class. Adds:

- `@ManyToOne` + `@JoinColumn(name = "account_id")`
- `account` field typed as `AccountBean` + getter/setter

Defaults match Duke's Bank `TxBean` ↔ `AccountBean` FK CMR (`tx-account` in `jbosscmp-jdbc.xml`).

## Verify

```powershell
cd rewrite-recipes
.\scripts\run-test.ps1
# TxBeanCmpForeignKeyTest
```

## Deferred

- Inverse `@OneToMany` on `AccountBean.transactions`
- `relationship_maps_to_table` crosswalk edges (ADR-004 planned)
- `CmpScalarEntityToJpa` for `TxBean` scalars (v0.4c)
