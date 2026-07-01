# CMP scalar entity to JPA — `AccountBean` (3.3)

**Status:** Accepted (ADR-007 §3.3)  
**Date:** 2026-06-28  
**Primary source:** `C:\github\dukesbank\src\j2eetutorial14\examples\bank\src\com\sun\ebank\ejb\account\AccountBean.java`  
**Column bindings:** `dukesbank/.../dd/ejb/jbosscmp-jdbc.xml` (AccountBean entity)  
**Recipe:** `CmpScalarEntityToJpa`  
**Proof test:** `AccountBeanCmpToJpaTest`

**Related:** [CMP capability matrix (3.1a)](cmp-jpa-capability-matrix.md) · [ADR-007 §3.3](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-007-rewrite-recipes-session-and-cmp-jpa.md)

---

## 1. Scope lock (3.3)

### In scope

| Item | Detail |
|------|--------|
| Target | `AccountBean` only |
| Fields | 7 scalars — `accountId`, `type`, `description`, `balance`, `creditLine`, `beginBalance`, `beginBalanceTimeStamp` |
| Table | `@Table(name = "ACCOUNT")` |
| Columns | From `jbosscmp-jdbc.xml` / crosswalk (`account_id`, `credit_line`, …) |
| PK | `@Id` on `accountId` |
| Class shape | `abstract` + `EntityBean` → concrete `@Entity` |
| Accessors | Abstract CMP get/set → private fields + concrete get/set |
| Removal | CMR `customers` accessors, `addCustomer` / `removeCustomer`, EntityBean lifecycle |

### Out of scope (v0.4+)

- `@ManyToMany` / `customers` collection
- `AccountLocal`, `AccountHome`, remote interfaces
- EJB-QL `findByCustomerId`
- `ejbCreate` / factory migration to JPA `EntityManager.persist`
- Unused import cleanup (`javax.ejb.*`)
- Linked SSOT validation at apply time (optional manual re-crosswalk)

---

## 2. Column map (AccountBean)

| Java field | DB column | JPA |
|------------|-----------|-----|
| `accountId` | `account_id` | `@Id` `@Column(name="account_id")` |
| `type` | `type` | `@Column(name="type")` |
| `description` | `description` | `@Column(name="description")` |
| `balance` | `balance` | `@Column(name="balance")` |
| `creditLine` | `credit_line` | `@Column(name="credit_line")` |
| `beginBalance` | `begin_balance` | `@Column(name="begin_balance")` |
| `beginBalanceTimeStamp` | `begin_balance_time_stamp` | `@Column(name="begin_balance_time_stamp")` |

---

## 3. Recipe behavior

`CmpScalarEntityToJpa` (defaults tuned for Duke's Bank `AccountBean`):

1. Skip if `@Entity` already present (idempotent).
2. Add `@Entity` / `@Table(name="ACCOUNT")`.
3. Remove `implements EntityBean` and `abstract` modifier.
4. Replace each scalar CMP accessor pair with annotated field + getters/setters.
5. Remove CMR accessors (`getCustomers` / `setCustomers`), CMR business methods, EntityBean lifecycle, `EntityContext` field.

Persistence API: **`javax.persistence`** (JPA 2.x target for legacy stack migration).

---

## 4. Verification

| Check | Status |
|-------|--------|
| `CmpScalarEntityToJpa` recipe | ✅ |
| `AccountBeanCmpToJpaTest` | ✅ (`.\scripts\run-test.ps1`) |
| Full on-disk `AccountBean.java` apply | ✅ `ApplyRecipeMain` + `run-e2e-jpa-parity.ps1` |
| Re-export + JPA crosswalk + parity report | ✅ `run-e2e-jpa-parity.ps1` — JSON + HTML + `dukesbank-cmp-jpa` matrix |

---

## 5. Follow-on (v0.4+)

| Recipe | Concern |
|--------|---------|
| `CmpManyToManyToJpa` | `customers` + `CUSTOMER_ACCOUNT_XREF` |
| `CmpForeignKeyToJpa` | `TxBean.account` FK (`account_id`) |
| `CmpScalarEntityToJpa` (TxBean) | v0.4c — 5 scalar fields on `TX` table |
| `CmpScalarEntityToJpa` (CustomerBean) | v0.4c — 10 scalar fields on `CUSTOMER` table |
| `NextIdTableToJpa` | `NextIdBean` table-backed counter (`getNextId` retained) |

---

## Changelog

| Date | Change |
|------|--------|
| 2026-06-28 | Initial 3.3 — `AccountBean` scalars only |
