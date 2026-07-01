# NextId table to JPA (NextIdBean)

**Recipe:** `NextIdTableToJpa`  
**ADR:** [ADR-007 v0.4d](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-007-rewrite-recipes-session-and-cmp-jpa.md)  
**Pattern:** `cmp-next-id-table-to-jpa-next-id-bean` in [pattern-catalog](https://github.com/anchor-migration/pattern-catalog)

## Scope

Migrates Duke's Bank table-backed ID counter (`NEXT_ID` rows keyed by `beanName`) to JPA:

- `@Entity` + `@Table(name = "NEXT_ID")`
- `@Id beanName`, `@Column id` (int counter)
- **Retains** `getNextId()` increment logic (same semantics as CMP)

This is **not** `@GeneratedValue` / database sequence — that replacement is a separate human-reviewed step.

## Verify

```powershell
cd rewrite-recipes
.\scripts\run-test.ps1
# NextIdBeanCmpToJpaTest
```

## Deferred

- `@GeneratedValue` / `@TableGenerator` / dedicated ID service
- `LocalNextId` / `LocalNextIdHome` removal
- Call-site migration in `AccountControllerBean` (still uses `LocalNextIdHome`)
