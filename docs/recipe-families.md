# Recipe families (ADR-007 / ADR-008)

Each recipe declares metadata in `rewrite.yml` and/or Java `@Option` fields.

## Families

| `recipeFamily` | ADR | Purpose |
|----------------|-----|---------|
| `stack-migration` | [ADR-007](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-007-rewrite-recipes-session-and-cmp-jpa.md) | EJB Session→Service (BeanState), CMP→JPA |
| `language-modernization` | [ADR-008](https://github.com/anchor-migration/migration-hub/blob/main/docs/ADR-008-java-language-modernization-and-tuple-lists.md) | L1/L2/L3 — Vector, generics, tuple lists |

## Smoke (3.0)

| Recipe | Family | Tier |
|--------|--------|------|
| `AddAnchorProbeComment` | harness | — |

Future recipes add rows here before implementation.

## Recommended run order

1. **L1** language modernization (when available)
2. **Stack** migration recipes (BeanState, JPA entity)
3. **L2/L3** after list classifier report (ADR-008 M2)
