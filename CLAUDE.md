# CLAUDE.md

[AGENTS.md](./AGENTS.md) is the primary source of Flovira project instructions. Read it and applicable module instructions before editing. This file adds no independent rules.

Flovira is an integrated workflow SDK with Java 8 core compatibility, framework / ORM / JSON independence, separate Vue / React packages and MySQL / PostgreSQL / Oracle schemas.

For releases, follow root `AGENTS.md` and `docs/releasing.md`: use a `release-<VERSION>` branch from `develop`, manual version updates, and an annotated tag on the `main` merge commit. Do not substitute the automatic Lerna release command.

Use the existing engine factories and adapter matrix. Follow root rules for public contracts, licensing, migration, verification and git authorization. Flovira schemas do not use foreign keys; every table keeps `deleted` non-null with default `0`, and indexes follow tenant and logical-deletion query predicates. A workflow uses its definition's form; nodes configure field permissions, not separate forms. Do not rely on obsolete Maven / Yarn commands, dual designer modes, built-in form management or SQL Server support.

Timeout scheduling is host-owned: expose explicit timeout APIs only. Hosts must integrate scheduling; document this requirement in usage guides and designer timeout settings.
