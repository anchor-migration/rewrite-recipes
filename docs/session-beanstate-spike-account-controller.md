# Session → BeanState spike — `AccountControllerBean` (3.1b)

**Status:** Accepted (ADR-007 §3.1b)  
**Date:** 2026-06-27  
**Primary source:** `C:\github\dukesbank\src\j2eetutorial14\examples\bank\src\com\sun\ebank\ejb\account\AccountControllerBean.java`  
**Spike recipe:** `ExtractSessionBeanStateSpike`  
**Proof test:** `AccountControllerBeanStateSpikeTest`

**Related:** [ADR-007 §2](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-007-rewrite-recipes-session-and-cmp-jpa.md) · [CMP matrix (3.1a)](cmp-jpa-capability-matrix.md)

---

## 1. Problem

`AccountControllerBean` is a **Stateful** session bean (`ejb-jar.xml`). It caches **EJB home** references in instance fields, populated in `ejbCreate` / `ejbActivate` and cleared in `ejbPassivate`.

Replacing `implements SessionBean` with `@Service` without changing structure would make those fields **singleton-shared** — incorrect under Spring’s default scope (see ADR-007 primer).

**Accepted direction (3.2):** extract conversational / cached state into **`BeanState`**, pass **`BeanState state`** into methods that need it, keep `@Service` stateless.

**3.1b goal:** validate the pattern on **paper + one method subset** with a **failing→passing rewrite-test** before full 3.2 recipe chain.

---

## 2. Field analysis (full bean)

From live source at `C:\github\dukesbank\.../AccountControllerBean.java`:

| Field | Type | Stateful? | Used in method bodies? | 3.2 treatment |
|-------|------|-----------|------------------------|---------------|
| `accountId` | `String` | Declared on instance | **No** — all APIs take `accountId` parameter | Drop or move to `BeanState` for compat only |
| `accountHome` | `LocalAccountHome` | **Yes** — JNDI cache | Yes — all account operations | **`BeanState`** |
| `customerHome` | `LocalCustomerHome` | **Yes** | Yes — customer / xref ops | **`BeanState`** |
| `nextIdHome` | `LocalNextIdHome` | **Yes** | Yes — `createAccount` only | **`BeanState`** |

**Lifecycle (out of 3.1b spike, in 3.2):**

| Method | Role today | Post-migration |
|--------|------------|----------------|
| `ejbCreate` / `ejbActivate` | Populate homes via `EJBGetter` | Factory / `@PostConstruct` on **`BeanState`**, or caller init |
| `ejbPassivate` | Null homes | N/A — `BeanState` serialized or discarded per session policy |
| `setSessionContext` | No-op | Remove with `SessionBean` |

---

## 3. Method inventory

| Method | Uses `accountHome` | Uses `customerHome` | Uses `nextIdHome` | 3.1b spike? |
|--------|-------------------|---------------------|-------------------|-------------|
| `createAccount` | ✓ | ✓ | ✓ | No — 3.2 |
| `removeAccount` | ✓ | | | **Yes — spike subset** |
| `addCustomerToAccount` | ✓ | ✓ | | No |
| `removeCustomerFromAccount` | ✓ | ✓ | | No |
| `getAccountsOfCustomer` | via collection | ✓ | | No |
| `getCustomerIds` | ✓ | | | No |
| `getDetails` | ✓ | | | No |
| `copyAccountsToDetails` | | | | No — private, no homes |
| `copyCustomerIdsToArrayList` | | | | No — private, no homes |

**Why `removeAccount` for the spike:**

- Single home dependency (`accountHome` only) — smallest vertical slice.
- Still requires **field extraction** (all three homes move to `BeanState` together — they share one passivation unit in the real bean).
- Clear before/after semantics without `createAccount` / NextId complexity.

---

## 4. Target transformation (spike scope)

### Before (fixture subset)

```java
public class AccountControllerBean implements SessionBean {
    private LocalAccountHome accountHome;
    private LocalCustomerHome customerHome;
    private LocalNextIdHome nextIdHome;

    public void removeAccount(String accountId) throws ... {
        account = accountHome.findByPrimaryKey(accountId);
        account.remove();
    }
}
```

### After (3.1b spike output)

Introduces `BeanState`, moves home fields, adds `BeanState state` parameter. **Does not yet rewrite** `accountHome` → `state.accountHome` in the method body — that is **`ThreadBeanStateThroughMethods` (3.2a)**.

```java
public class AccountControllerBean implements SessionBean {
    static class BeanState {
        LocalAccountHome accountHome;
        LocalCustomerHome customerHome;
        LocalNextIdHome nextIdHome;
    }

    public void removeAccount(BeanState state, String accountId) throws ... {
        account = accountHome.findByPrimaryKey(accountId);
        account.remove();
    }
}
```

### After (full target — 3.2a+)

```java
public class AccountControllerBean implements SessionBean {
    static class BeanState {
        LocalAccountHome accountHome;
        LocalCustomerHome customerHome;
        LocalNextIdHome nextIdHome;
    }

    public void removeAccount(BeanState state, String accountId) throws ... {
        account = state.accountHome.findByPrimaryKey(accountId);
        account.remove();
    }
}
```

**Caller contract (document only — no recipe in 3.1b):**

```java
BeanState state = new BeanState();
// populate homes (today: EJBGetter; later: @Inject)
state.accountHome = ...;
service.removeAccount(state, accountId);
```

Web tier / `AccountControllerHome.create()` replacement is **out of scope** until a separate call-site recipe.

---

## 5. Planned 3.2 recipe chain (post-spike)

| Step | Recipe | Scope |
|------|--------|-------|
| A2a | `ExtractSessionBeanState` | Generalize spike — all home fields |
| A2b | `ThreadBeanStateThroughMethods` | All methods using homes (+ private helpers if needed) |
| A2c | `DeclareSpringService` | `@Service`; drop `implements SessionBean` |
| A2d | `RemoveSessionBeanLifecycle` | Remove `ejbCreate`, `ejbPassivate`, … |
| 3.4+ | `InjectEjbHomesOrRepositories` | Replace homes with Spring/JPA deps |

**Rejected:** `@Stateless` / `@Service` annotation-only rename.

---

## 6. Verification

| Check | Status |
|-------|--------|
| Design doc (this file) | ✅ |
| `ExtractSessionBeanStateSpike` recipe | ✅ (structure + parameter) |
| `AccountControllerBeanStateSpikeTest` rewrite-test | ✅ (`.\scripts\run-test.ps1`) |
| Field access `accountHome` → `state.accountHome` in method body | 📋 **3.2a** (`ThreadBeanStateThroughMethods`) |
| Full `AccountControllerBean` on disk | Manual review — source at `C:\github\dukesbank` |
| `java-ast-ssot export` diff | Deferred to 3.2 |
| `parity-verify` | Not available |

---

## 7. Limitations (Duke's Bank vs production)

| Topic | Duke's Bank | Broader legacy |
|-------|-------------|----------------|
| Conversational field state | Minimal (`accountId` unused) | Wizards, carts — same `BeanState` pattern |
| Stateless → stateful delegate | `TellerBean` → `home.create()` | Caller must hold `BeanState` — separate recipe |
| Raw `ArrayList` return types | Yes | ADR-008 L1/L2 after stack migration |

---

## 8. Sign-off

| Role | Assessment |
|------|------------|
| **Pragmatist** | Spike proves OpenRewrite can thread `BeanState` on real EJB idioms; ready to plan 3.2 |
| **Critic** | Only one method — must not ship as “migration complete” |
| **Suggester** | Next: thread remaining methods, then `@Service` |

**Next gate:** **3.2** — production recipe chain on full `AccountControllerBean` ([ADR-007](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-007-rewrite-recipes-session-and-cmp-jpa.md)).

---

## Changelog

| Date | Change |
|------|--------|
| 2026-06-27 | Initial 3.1b spike — `removeAccount` subset + rewrite-test |
