# CLAUDE.md

[AGENTS.md](./AGENTS.md) is the primary source of Flovira project instructions. Read it and applicable module instructions before editing. This file adds no independent rules.

Flovira is an integrated workflow SDK with Java 8 core compatibility, framework / ORM / JSON independence, separate Vue / React packages and MySQL / PostgreSQL / Oracle schemas.

Use the existing engine factories and adapter matrix. Follow root rules for public contracts, licensing, migration, verification and git authorization. Flovira schemas do not use foreign keys; every table keeps `deleted` non-null with default `0`, and indexes follow tenant and logical-deletion query predicates. Do not rely on obsolete Maven / Yarn commands, dual designer modes, built-in form management or SQL Server support.
