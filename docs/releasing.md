# Releasing

Backend and frontend releases share the `v` tag prefix. Do not use a separate frontend tag prefix.

| Trigger | Maven version | npm channel |
| --- | --- | --- |
| Push to `develop` | `1.0.0-<first seven commit characters>-SNAPSHOT` | No publication |
| Push `v1.0.0-alpha.1` | `1.0.0-alpha.1` | Same version on `next` |
| Push `v1.0.0` | `1.0.0` | Same version on `latest` |

The backend accepts `vX.Y.Z` and `vX.Y.Z-alpha.N`, where N is a nonnegative integer without leading zeros.
Alpha releases use the non-Snapshot Maven Central publishing flow, without a `-SNAPSHOT` suffix.
Beta, RC, and other tag formats are not currently supported by the backend.

## Authorization and prerequisites

Follow the release-branch process used by lumen-design. Start only after the user explicitly requests publication and provides the target version. A complete release request authorizes all steps below without repeated confirmation. Respect narrower boundaries, such as preparation without pushing.

Pause for a dirty worktree, merge conflicts, failed validation, an existing target version or tag, publishing failures requiring code or metadata changes, or any required deviation from this process. Never overwrite published versions or move published tags.

Before releasing, check local and remote tags and the target version in npm / Maven Central. Confirm that Trusted Publishing is configured for every npm package and that Maven Central credentials and signing are ready.

The npm workflow uses `lerna publish from-package` to publish manifest versions not yet present in the registry, matching lumen-design's release-merge tagging process. It also supports manual dispatch from `main` only. Do not treat a green workflow that published no packages as proof that the target versions are available; verify the registry results.

## 1. Create the release branch

The examples use `1.0.0-alpha.1`. Replace it consistently with the target version. Start with a clean worktree:

```bash
rtk git switch develop
rtk git pull --ff-only origin develop
rtk git switch -c release-1.0.0-alpha.1
```

The release branch must be named `release-<VERSION>`, an exception to ordinary development branch naming.

## 2. Update versions manually and validate

Align project versions in these files:

- `flovira-designer/package.json` and `flovira-designer/lerna.json`
- `flovira-designer/vue/package.json` and `flovira-designer/react/package.json`
- `flovira-designer/react-adapters/lumen/package.json`
- `flovira-designer/react-adapters/antd/package.json`
- `flovira-designer/examples/*/package.json`

Update internal Flovira dependency ranges, including adapter peer dependencies on the React designer. Preserve `workspace:*` references. Do not change third-party dependency versions such as Lumen or React. Private workspaces and examples are not published to npm.

From `flovira-designer`, regenerate the lockfile and validate:

```bash
rtk bun install
rtk bun install --frozen-lockfile
rtk bun run check
rtk bun run build:demos
```

Check these manifests and `bun.lock` for stale project versions and internal dependency ranges. Check Flovira-owned versions only; matching third-party version numbers are not stale project versions.

Backend versions are injected through `-PreleaseVersion`. Do not update each Java module separately or rename SQL baselines for alpha releases. From the repository root, run:

```bash
rtk proxy ./gradlew clean check -PreleaseVersion=1.0.0-alpha.1 --no-daemon --no-parallel --max-workers=2
```

Do not use `bun run release`: the current Lerna configuration permits only `main` and automatically commits, tags, and pushes, which does not match this release-branch process.

## 3. Commit the version bump

Stage only the version-related manifests and `bun.lock`, then inspect and commit the staged changes:

```bash
rtk git diff --cached
rtk git diff --cached --check
rtk git commit -m "Bumped version number to 1.0.0-alpha.1"
```

This message format is an exception to ordinary commit conventions. Do not push the release branch unless explicitly requested.

## 4. Merge and tag main

```bash
rtk git switch main
rtk git pull --ff-only origin main
rtk git merge --no-ff release-1.0.0-alpha.1 -m "Merge branch 'release-1.0.0-alpha.1' into main"
rtk git tag -a v1.0.0-alpha.1 -m "v1.0.0-alpha.1"
rtk git show --no-patch --decorate v1.0.0-alpha.1
```

Use an annotated tag. Verify that it points to this `main` merge commit and that the Lerna version and all four publishable npm package versions match the tag.

## 5. Merge back to develop

```bash
rtk git switch develop
rtk git pull --ff-only origin develop
rtk git merge --no-ff release-1.0.0-alpha.1 -m "Merge branch 'release-1.0.0-alpha.1' into develop"
```

## 6. Push in release order

Push `main` first, then the exact release tag, and finally `develop`:

```bash
rtk git push origin main
rtk git push origin v1.0.0-alpha.1
rtk git push origin develop
```

Never use `git push --tags`, force-push published tags, or move them. Pushing `develop` also triggers a backend Snapshot publication.

## 7. Verify publication

Find the npm and Maven runs triggered by this tag, verify their tag and commit, and wait for completion:

```bash
rtk proxy gh run list --workflow publish-npm.yml --limit 5
rtk proxy gh run list --workflow publish.yml --limit 5
rtk proxy gh run watch <NPM_RUN_ID> --exit-status
rtk proxy gh run watch <MAVEN_RUN_ID> --exit-status
```

The workflows run independently; publication is not atomic. Completion requires all npm package versions and their `next` / `latest` dist-tags to be correct, and the target Maven Central artifacts to be available. Do not mistake a Snapshot build for the tag-triggered release. The current process does not create a GitHub Release.

Rerun failed workflows for transient failures without recreating or moving tags. If an earlier npm run used `from-git` and skipped packages, the updated npm workflow can be dispatched from `main` after verifying its package versions:

```bash
rtk proxy gh workflow run publish-npm.yml --ref main
```

Manual npm dispatch does not trigger Maven publication. Changes to published code or package metadata require a new version and tag.

## 8. Delete the local release branch

Only after all three pushes and publication verification succeed:

```bash
rtk git switch develop
rtk git branch -d release-1.0.0-alpha.1
```

If the release branch was explicitly pushed, deleting the remote branch still requires separate authorization.
