# CMP ManyToMany to JPA (AccountBean customers)

**Recipe:** `CmpManyToManyToJpa`  
**ADR:** [ADR-007 §v0.4a](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-007-rewrite-recipes-session-and-cmp-jpa.md)  
**Pattern:** `cmp-many-to-many-to-jpa-account-bean` in [pattern-catalog](https://github.com/anchor-migration/pattern-catalog)

## Scope

Runs **after** `CmpScalarEntityToJpa` on an existing `@Entity` class. Adds:

- `@ManyToMany` + `@JoinTable(name = "CUSTOMER_ACCOUNT_XREF", ...)`
- `customers` collection field + getter/setter

Defaults match Duke's Bank `AccountBean` ↔ `CustomerBean` xref table.

## Verify

```powershell
cd rewrite-recipes
.\scripts\run-test.ps1
# AccountBeanCmpManyToManyTest
```

## Deferred

- Inverse side on `CustomerBean`
- `relationship_maps_to_table` crosswalk edges (ADR-004 planned)
