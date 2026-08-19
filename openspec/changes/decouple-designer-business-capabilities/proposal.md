## Why

The designer backend currently exposes business-specific data through several loosely related services, while the Vue and React packages consume different, partly untyped contracts. Flovira needs one host-neutral contract so applications such as Intelliconf can declare supported workflow features and supply domain data without moving workflow behavior into the host application.

## What Changes

- Add a versioned designer capability manifest covering node types, approver strategies, approval modes, return policies, timeout support, operations, and available business data sources.
- Add a unified business data provider contract for users, roles, organizations, form fields, dictionaries, subprocess definitions, and organization relationship queries.
- Remove the legacy business-data UI services and their REST endpoints so 1.0.0 has one integration contract.
- Expose the contract through the optional Spring Web bridge with a configurable API prefix.
- Make both Vue and React designer packages consume the same typed contract and hide unsupported features according to the capability manifest.
- Keep workflow validation, serialization, approver resolution, countersign calculation, empty-approver handling, timeout processing, and state transitions inside Flovira.

## Capabilities

### New Capabilities
- `designer-integration-contract`: Versioned host capability and business-data contracts shared by backend adapters and designer packages.
- `configurable-designer-rest-bridge`: Optional Spring REST bridge whose route prefix is controlled by host configuration.

### Modified Capabilities

## Impact

- `flovira-plugin-ui-core`: new public integration contracts and value objects.
- `flovira-plugin-ui-sb-web`: new contract endpoints and configurable request mapping.
- `flovira-designer/vue` and `flovira-designer/react`: shared contract-shaped TypeScript types, provider methods, capability-driven controls, and regression tests.
- The `/flovira` default prefix remains, but legacy business-data endpoints are not part of the 1.0.0 contract.
