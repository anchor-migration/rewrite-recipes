# Session → Spring @Service — `AccountControllerBean` (3.2)

**Status:** Accepted (ADR-007 §3.2)  
**Date:** 2026-06-27  
**Primary source:** `C:\github\dukesbank\src\j2eetutorial14\examples\bank\src\com\sun\ebank\ejb\account\AccountControllerBean.java`  
**Composite recipe:** `SessionBeanToSpringService`  
**Proof test:** `AccountControllerBeanSessionMigrationTest`

**Related:** [3.1b spike](session-beanstate-spike-account-controller.md) · [ADR-007 §3.2](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-007-rewrite-recipes-session-and-cmp-jpa.md)

---

## 1. Recipe chain

| Step | Recipe | Role |
|------|--------|------|
| A2a | `ExtractSessionBeanState` | Move `accountHome`, `customerHome`, `nextIdHome` into `static class BeanState` |
| A2b | `ThreadBeanStateThroughMethods` | Auto-detect business methods using homes; add `BeanState state` first param; rewrite `home` → `state.home` |
| A2c | `DeclareSpringService` | Add `@org.springframework.stereotype.Service`; drop `implements SessionBean` |
| A2d | `RemoveSessionBeanLifecycle` | Remove `ejbCreate`, `ejbActivate`, `ejbPassivate`, `ejbRemove`, `setSessionContext` |

Run all four via **`SessionBeanToSpringService`** (registered in `META-INF/services/org.openrewrite.Recipe`).

---

## 2. Scope

### In scope (3.2)

- All seven business methods on `AccountControllerBean` that reference cached homes
- `@Service` declaration (FQN annotation; Spring on classpath at apply time)
- SessionBean lifecycle removal

### Out of scope (3.4+)

- Caller / web tier: `AccountControllerHome.create()` → `BeanState` factory
- `@Inject` / JPA repository replacement for homes
- `accountId` instance field (unused in method bodies — left on class)
- Unused import cleanup (`EJBGetter`, `javax.ejb.*`)

---

## 3. Caller contract (document only)

```java
BeanState state = new BeanState();
state.accountHome = ...;  // today: EJBGetter; later: @Inject / repos
state.customerHome = ...;
state.nextIdHome = ...;
service.removeAccount(state, accountId);
```

Home initialization moves out of `ejbCreate` / `ejbActivate` into caller or a future `BeanState` factory recipe.

---

## 4. Verification

| Check | Status |
|-------|--------|
| `ExtractSessionBeanState` | ✅ |
| `ThreadBeanStateThroughMethods` (field access rewrite) | ✅ |
| `DeclareSpringService` | ✅ |
| `RemoveSessionBeanLifecycle` | ✅ |
| `SessionBeanToSpringService` composite | ✅ |
| `AccountControllerBeanSessionMigrationTest` | ✅ (`.\scripts\run-test.ps1`) |
| Full on-disk `AccountControllerBean.java` apply | 📋 manual / CI follow-up |
| `java-ast-ssot export` diff | 📋 optional post-apply |

---

## 5. Known limitations

| Topic | Notes |
|-------|-------|
| Formatting | `autoFormat` on `@Service` may expand inner `BeanState` layout |
| Imports | Lifecycle removal does not prune unused imports |
| Spike regression | `ExtractSessionBeanStateSpike` (3.1b) unchanged — param only, no field rewrite |

---

## Changelog

| Date | Change |
|------|--------|
| 2026-06-27 | Initial 3.2 chain + composite test |
