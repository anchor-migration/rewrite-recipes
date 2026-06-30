# CMP scalar entity to JPA — `CustomerBean` (v0.4c)

**Status:** Accepted (ADR-007 v0.4c)  
**Primary source:** `C:\github\dukesbank\src\j2eetutorial14\examples\bank\src\com\sun\ebank\ejb\customer\CustomerBean.java`  
**Column bindings:** `dukesbank/.../dd/ejb/jbosscmp-jdbc.xml` (CustomerBean entity)  
**Recipe:** `CmpScalarEntityToJpa` with `targetClassName=CustomerBean`  
**Proof test:** `CustomerBeanCmpToJpaTest`

**Related:** [cmp-scalar-entity-to-jpa-account-bean.md](cmp-scalar-entity-to-jpa-account-bean.md) · [cmp-many-to-many-to-jpa-account-bean.md](cmp-many-to-many-to-jpa-account-bean.md)

---

## Scope

| Item | Detail |
|------|--------|
| Target | `CustomerBean` |
| Fields | 10 scalars — `customerId`, `lastName`, `firstName`, `middleInitial`, `street`, `city`, `state`, `zip`, `phone`, `email` |
| Table | `@Table(name = "CUSTOMER")` |
| PK | `@Id` on `customerId` → column `customer_id` |
| Removal | CMR `accounts` accessors, all `ejb*` / EntityBean lifecycle |

## Recommended sequence

1. `CmpScalarEntityToJpa` (`targetClassName=CustomerBean`) — this doc
2. Inverse `@ManyToMany` on `accounts` — deferred (AccountBean `customers` side covered by v0.4a)

## Verify

```powershell
cd rewrite-recipes
.\scripts\run-test.ps1
# CustomerBeanCmpToJpaTest
```

## Deferred

- `@ManyToMany accounts` inverse collection
- EJB-QL finders (`findByLastName`, …)
- Unused import cleanup
