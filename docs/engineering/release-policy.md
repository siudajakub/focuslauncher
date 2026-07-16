# Release Policy

Last verified: 2026-07-16 on `feature/focus-enhancements-2026-07`.

FocusLauncher targets GitHub Releases as its first public distribution channel. The repository is
configured for a signed `1.0.0` APK, but the GitHub repository has no published release yet. Do not
describe the app as publicly available until a tag-triggered release succeeds.

## Build Identity

- Public application ID: `com.siudajakub.focuslauncher`.
- Debug and nightly suffixes: `.debug` and `.nightly`.
- Android namespace and Kotlin packages remain `de.mm20.launcher2` for source compatibility.
- Version: `versionName = 1.0.0`, default `versionCode = 10000`.
- Release and nightly builds use the `gh-actions` signing configuration.
- R8 and resource shrinking are disabled for 1.0.0 until keep rules and device tests exist.

## CI Coverage

| Acceptance area | Workflow | Current coverage |
| --- | --- | --- |
| Agent/project docs | `.github/workflows/ci.yml` / `agent-docs` | Runs `python3 tools/check_agent_docs.py`. |
| JVM tests and debug APK | `.github/workflows/ci.yml` / `build-and-test` | Runs `./gradlew test :app:app:assembleDefaultDebug` on JDK 21. |
| Room migrations | `.github/workflows/ci.yml` / `migration-tests` | Checks the current exported schema, then runs `:data:database:connectedDebugAndroidTest` on API 35. |
| Signed nightly APK | `.github/workflows/build-nightly.yml` | Runs JVM tests, applies a date-based version code, signs, and uploads the nightly APK. |
| Signed release APK | `.github/workflows/release.yml` | Runs JVM tests, signs `assembleDefaultRelease`, uploads an artifact, and publishes only for `v*` tags. |
| Reference site | `.github/workflows/deploy-docs.yml` | Builds Dokka/VitePress and deploys GitHub Pages; not a release gate. |

The current Room database version is 37 and
`data/database/schemas/de.mm20.launcher2.database.AppDatabase/37.json` is committed. Schemas 25–36
are not backfilled; runtime coverage for the supported migration chain lives in the instrumented
migration tests.

## Signing Contract

Release and nightly workflows require these owner-held secrets:

- `KEYSTORE`: base64-encoded keystore.
- `KEYSTORE_PASSWORD`: keystore password.
- `SIGNING_KEY_ALIAS`: signing alias.
- `SIGNING_KEY_PASSWORD`: key password.

The signing configuration was exercised by a manual release-workflow dry run on 2026-06-30. The
release key and credentials must remain backed up; losing them prevents compatible updates under
the same application ID.

## Release Gate

Before creating a public `v*` tag:

- The final commit has green docs, JVM/debug-build, and migration CI checks.
- A fresh-context review has no unresolved high-severity findings.
- The full Pixel checklist in [pixel-smoke-test.md](pixel-smoke-test.md) passes on the release
  candidate, including focus gating, temporary access, session recovery, usage reflection, search
  scope, battery observation, and crash diagnostics.
- The owner confirms version, release notes, artifact naming, and signing-key backup.
- `PROJECT_STATUS.md` records the exact verified commit and evidence.

## Known CI Gaps

- Lint is non-gating (`abortOnError = false`) and no workflow currently publishes lint results.
- PR CI builds only the debug variant; release signing is exercised by the release workflow, not on
  every pull request.
- Focus launch behavior still requires manual device testing.
- `ci.yml`, `build-nightly.yml`, and `deploy-docs.yml` still use tag-based third-party actions;
  only `release.yml` is pinned to commit SHAs.
- The debug APK built by PR CI is not uploaded as an artifact.

Actionable release and CI work belongs in GitHub Issues, not in this document.
