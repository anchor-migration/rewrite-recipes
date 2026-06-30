# CMP scalar entity to JPA — `TxBean` (v0.4c)

**Status:** Accepted (ADR-007 v0.4c)  
**Primary source:** `C:\github\dukesbank\src\j2eetutorial14\examples\bank\src\com\sun\ebank\ejb\tx\TxBean.java`  
**Column bindings:** `dukesbank/.../dd/ejb/jbosscmp-jdbc.xml` (TxBean entity)  
**Recipe:** `CmpScalarEntityToJpa` with `targetClassName=TxBean`  
**Proof test:** `TxBeanCmpToJpaTest`

**Related:** [cmp-scalar-entity-to-jpa-account-bean.md](cmp-scalar-entity-to-jpa-account-bean.md) · [cmp-foreign-key-to-jpa-tx-bean.md](cmp-foreign-key-to-jpa-tx-bean.md)

---

## Scope

| Item | Detail |
|------|--------|
| Target | `TxBean` |
| Fields | 5 scalars — `txId`, `timeStamp`, `amount`, `balance`, `description` |
| Table | `@Table(name = "TX")` |
| PK | `@Id` on `txId` → column `tx_id` |
| Removal | CMR `account` accessors, all `ejb*` / EntityBean lifecycle |

## Recommended sequence

1. `CmpScalarEntityToJpa` (`targetClassName=TxBean`) — this doc
2. `CmpForeignKeyToJpa` — `@ManyToOne account` FK ([v0.4b](cmp-foreign-key-to-jpa-tx-bean.md))

## Verify

```powershell
cd rewrite-recipes
.\scripts\run-test.ps1
# TxBeanCmpToJpaTest
```

## Deferred

- `ejbCreate` / `EntityManager.persist` factory migration
- Unused import cleanup (`javax.ejb.*`, `LocalAccount`)
- Inverse `@OneToMany` on `AccountBean`
