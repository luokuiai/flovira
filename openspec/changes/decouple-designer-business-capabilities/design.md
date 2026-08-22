## Context

`flovira-plugin-ui-core` currently discovers several optional Spring beans through `FrameInvoker`, and the Vue package mirrors those endpoints with a permissive `any`-based provider. React only knows about subprocess definitions. This makes feature availability implicit, duplicates integration logic, and encourages a host application to leak its own user, role, and organization API shapes into a designer.

Flovira is a Java 8 SDK with optional framework adapters. The core engine must remain framework, ORM, and JSON independent, and existing UI extension interfaces must remain usable during migration.

## Goals / Non-Goals

**Goals:**

- Define a versioned, serializable capability manifest owned by Flovira.
- Define one host-facing business data provider with stable query and result models.
- Let hosts expose only the nodes, approver strategies, modes, policies, and operations they support.
- Give Vue and React the same integration shape and capability filtering behavior.
- Keep the Spring module an optional transport adapter with a configurable prefix.
- Remove the pre-1.0 business-data extension services rather than carrying two integration models.

**Non-Goals:**

- Prescribing Intelliconf's persistence model or organization identifiers.
- Moving approver resolution or workflow state transitions into the UI plugin.
- Providing SSR or bundled designer pages.

## Decisions

1. **Separate capability declaration from data queries.** `DesignerCapabilityProvider` returns a `DesignerCapabilities` manifest; `DesignerDataProvider` handles paged selectable resources and relationship resolution. Capability loading is small and cacheable, while data queries remain demand-driven.

2. **Use semantic strategy descriptors.** Node types use Flovira node codes. Each approver strategy declares a semantic code, display name, selection type (`RESOURCE`, `RELATION`, or `EXPRESSION`), optional resource and relation types, and multiplicity. This avoids Java enum ordinals and lets both designers render host-defined strategies without host-specific UI code.

3. **Use a generic resource query/result envelope.** Users, roles, organizations, form fields, dictionaries, and subprocess definitions share stable item fields plus metadata. A resource type and optional scope/query parameters preserve domain flexibility without exposing Intelliconf DTOs.

4. **Model relationship resolution separately.** Runtime-oriented queries such as department leader, supervising leader, role members, organization members, and organization chain use a semantic relation code and a context object. The core engine invokes the host provider during execution and converts the returned subjects into final task permission identifiers.

5. **Single 1.0 contract.** The fork starts at 1.0.0, so `HandlerSelectService`, `HandlerDictService`, `CategoryService`, `FormPathService`, `NodeExtService`, and `ListenerListService` plus their REST operations are removed. All business-provided designer data is queried through `DesignerDataProvider`; no fallback branch is retained.

6. **Configurable Spring mapping via placeholder.** The controller uses `${flovira.ui-api-prefix:/flovira}`. A flat property is required because `flovira.ui` is already the public boolean enable switch and cannot also be a YAML object. Configuration and integration operations use this single prefix.

7. **Frontend providers receive defaults, then capabilities narrow them.** Both npm packages export identical contract types. Missing `capabilities()` means the complete built-in Flovira feature set for backward compatibility; an explicit manifest filters palette entries and option sets.

8. **Persist one versioned approver rule in node extensions.** Vue and React serialize the same `approverRule` object containing a semantic strategy, selection type, optional relation type, typed subject references, and an optional expression. The existing node `ext` column carries this configuration, so no task or node table migration is required. Runtime keeps `permissionFlag` as a read fallback for definitions created before this contract.

9. **Resolve rules before task persistence.** `USER` subjects resolve directly. `ROLE` and `ORGANIZATION` subjects delegate to `ROLE_MEMBERS` and `ORGANIZATION_MEMBERS`. `EXPRESSION` is passed into the existing handler-expression pipeline. Flovira validates provider output, removes duplicate identifiers, and rejects an empty resolved approver set so a task cannot be created with no eligible handler.

10. **Use semantic strategy values everywhere.** Java, Vue, and React use `USER`, `ROLE`, `ORGANIZATION`, and `EXPRESSION`; numeric designer-only aliases are not part of the 1.0 contract. Both designers query selectable resources through the same provider methods and store the same JSON shape.

11. **Make the Spring controller replaceable by subclass.** The Spring Web configuration creates the built-in `FloviraController` only when no Bean of that type or a subtype exists. A host can therefore add arbitrary class annotations, override selected endpoints for method annotations or relationship authorization, and inherit the rest without copying the complete bridge.

## Risks / Trade-offs

- [Generic resource metadata can become weakly typed] -> Keep required identity/display fields typed and isolate host-specific values in `metadata`.
- [Capability codes can drift between Java and TypeScript] -> Add matching constants/default manifests and contract-focused tests in both packages.
- [Removing prototype APIs requires all in-repo clients to move together] -> Update both backend bridge and designer packages in the same change and verify no legacy symbols remain.
- [Configurable mappings can surprise clients] -> Keep `/flovira` as the default and document the host override.
- [A host relation provider can return invalid or empty subjects] -> Validate results in core and fail task creation explicitly instead of producing an unclaimable task.

## Migration Plan

1. Implement the capability and data providers in the host application.
2. Configure `flovira.ui-api-prefix` when the host needs a route such as `/admin/v1/flovira`.
3. Point Vue or React at the host bridge, or inject an in-process/custom HTTP provider.
4. Existing definitions without `approverRule` continue to use `permissionFlag`; newly saved definitions use the versioned rule.

Rollback consists of reverting the 1.0 integration as a unit; there is no legacy facade.

## Open Questions

- Host-defined custom node renderers remain a later extension; this change only declares whether a node code is available.
- Fine-grained authorization of each resource query remains the host application's responsibility at its Spring security boundary.
