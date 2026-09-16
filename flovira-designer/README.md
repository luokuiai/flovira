# Flovira Designer

The frontend workspace for Flovira. Vue and React are published as independent npm packages, with separate runtime dependencies and public APIs. Optional adapter packages integrate the React designer with UI libraries.

| Directory | npm package / purpose |
| --- | --- |
| [`vue`](./vue) | `@luokuiai/flovira-vue-designer` |
| [`react`](./react) | `@luokuiai/flovira-react-designer` |
| [`react-adapters/lumen`](./react-adapters/lumen) | `@luokuiai/flovira-react-adapter-lumen` |
| [`react-adapters/antd`](./react-adapters/antd) | `@luokuiai/flovira-react-adapter-antd` |
| [`examples/vue-element-plus`](./examples/vue-element-plus) | Vue + Element Plus example, port 5180 |
| [`examples/vue-antdv`](./examples/vue-antdv) | Vue + Ant Design Vue example, port 5181 |
| [`examples/vue-naive`](./examples/vue-naive) | Vue + Naive UI example, port 5182 |
| [`examples/react`](./examples/react) | React default UI example, port 5183 |
| [`examples/react-lumen`](./examples/react-lumen) | React + Lumen UI example, port 5184 |
| [`examples/react-antd`](./examples/react-antd) | React + Ant Design example, port 5185 |

The backend `flovira-plugin-ui-*` modules provide designer APIs only. Host applications install the appropriate npm package and handle frontend integration, builds, and deployment.

## Build

From the repository root:

```bash
cd flovira-designer
bun install
bun run build:designer
bun run build:demos
```

Use `bun run build` from this workspace to build all packages and examples, or `bun run check` to run tests and library builds.

## Release

Follow the [release procedure](../docs/releasing.md): create `release-<VERSION>` from `develop`, update and validate versions manually, merge into `main`, create an annotated version tag, and merge back into `develop`. Push `main`, the exact tag, then `develop`.

Do not use `bun run release` for this process. GitHub Actions publishes unpublished manifest versions using `lerna publish from-package`. Prereleases use the npm `next` dist-tag; stable releases use `latest`. The same tag also triggers backend Maven publication.

Configure `.github/workflows/publish-npm.yml` as a Trusted Publisher separately for each of the four public npm packages listed above. Manual npm publication is available from `main` only.
