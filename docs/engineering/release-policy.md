# Release Policy

Build, artifact, signing, and release-gate policy for public distribution. Grounded in the
workflows under `.github/workflows/` as of branch `stabilize/focus-launcher`. This file makes
the current state explicit; it does not change CI. Proposed changes are isolated in the final
section.

## Current CI Coverage

Each acceptance area mapped to the real workflow and job that covers it.

| Acceptance area | Covered by | Notes |
| --- | --- | --- |
| Docs (agent docs / skills) | `.github/workflows/ci.yml` job `agent-docs` ("Agent documentation and skills") | Runs `python3 tools/check_agent_docs.py`. Triggers on push/PR to `main` and `workflow_dispatch`. |
| JVM tests | `.github/workflows/ci.yml` job `build-and-test` ("JVM tests and debug build") | Single step runs `./gradlew test :app:app:assembleDefaultDebug --stacktrace` on JDK 21. Also run by `build-nightly.yml` (`./gradlew test`). |
| Debug build | `.github/workflows/ci.yml` job `build-and-test` ("JVM tests and debug build") | Same step assembles `:app:app:assembleDefaultDebug`; the APK is built but **not** uploaded as an artifact. |
| Migration tests | `.github/workflows/ci.yml` job `migration-tests` ("Room migration tests") | Runs `:data:database:connectedDebugAndroidTest` on an API 35 x86_64 emulator (KVM enabled). Runtime migration validation runs; exported-schema validation is partial — see gap below. |
| Site/reference docs deploy | `.github/workflows/deploy-docs.yml` job `deploy` | Not an acceptance gate. Builds Dokka + VitePress and deploys GitHub Pages on push to `main` under `docs/**`, `plugins/sdk/**`, `core/shared/**`. Uses JDK 17, not 21. |

Coverage summary: docs, JVM tests, debug build, and migration tests are **all covered** by `ci.yml`.

Known gaps inside the covered areas:

- **Exported-schema validation is incomplete.** `migration-tests` checks for
  `data/database/schemas/de.mm20.launcher2.database.AppDatabase/36.json` and emits only a
  non-failing `::warning` if it is missing. In the current tree the exported schemas jump from
  `24.json` to `37.json` — versions 25–36 are not exported, and the database is at version 37
  while the job references 36/35. The job never fails on this; runtime migration tests still run,
  but exported-schema drift is not gated.
- **No release/nightly build is exercised by `ci.yml`.** Only the debug variant is assembled on
  PR/push. The signed `nightly` path is exercised only by the scheduled `build-nightly.yml`.
- **Lint is not gated.** `app/app` sets `lint { abortOnError = false }`; no workflow runs a lint
  task as a gate.
- **No Pixel/instrumented smoke beyond migration tests.** Launch-interception and focus-policy
  behavior is validated only by the manual Pixel 8 smoke test (`docs/engineering/pixel-smoke-test.md`),
  not by CI.

## Nightly Artifacts & Signing

What `.github/workflows/build-nightly.yml` does today:

- Trigger: scheduled daily at `0 4 * * *` (cron) and `workflow_dispatch`. Concurrency group
  `nightly`, cancel-in-progress.
- Runs `./gradlew test --stacktrace` (JVM tests) before building.
- Decodes a base64 keystore from `secrets.KEYSTORE` into
  `${RUNNER_TEMP}/keystore/keystore.jks`.
- Sets `VERSION_CODE_OVERRIDE=$(date +%Y%m%d00)`, consumed by `app/app/build.gradle.kts`
  (`versionCode = System.getenv("VERSION_CODE_OVERRIDE")?.toIntOrNull() ?: 2026012400`).
- Builds and signs with `./gradlew assembleDefaultNightly`.
- Uploads the artifact `de.mm20.launcher2.nightly.apk` from
  `app/app/build/outputs/apk/default/nightly/app-default-nightly.apk` with `if-no-files-found: error`.

Build/signing config wiring (`app/app/build.gradle.kts`):

- The `nightly` build type `initWith(release)`, adds `applicationIdSuffix = ".nightly"` and a
  `-yyyyMMdd-nightly` version-name suffix, and uses the `gh-actions` signing config.
- `isMinifyEnabled = false` and `isShrinkResources = false` for both `release` and `nightly`.
- The `gh-actions` signing config reads:
  - `storeFile` = `${RUNNER_TEMP}/keystore/keystore.jks` (the decoded keystore).
  - `storePassword` = env `KEYSTORE_PASSWORD`.
  - `keyAlias` = env `SIGNING_KEY_ALIAS`.
  - `keyPassword` = env `SIGNING_KEY_PASSWORD`.

Secrets the nightly workflow requires (names taken directly from the workflow — values are
owner-held and not in the repo):

- `secrets.KEYSTORE` — base64-encoded JKS keystore.
- `secrets.KEYSTORE_PASSWORD` — keystore (store) password.
- `secrets.SIGNING_KEY_ALIAS` — signing key alias.
- `secrets.SIGNING_KEY_PASSWORD` — signing key password.

If any of these secrets are absent the signed build will fail at `assembleDefaultNightly`.

Needs owner input (do not assume — these are owner decisions):

- The actual keystore, its passwords, and the key alias. Whether the existing `gh-actions`
  keystore is intended for public release signing or only for nightlies.
- The signing identity and key rotation/escrow policy for a public release.
- Whether public distribution ships the unsigned/owner-signed `release` variant, the `nightly`
  variant, or a separate F-Droid (`fdroid`) flavor build (the `fdroid` flavor exists but no
  workflow builds it).
- The distribution channel(s) (GitHub Releases, Play, F-Droid, direct APK) and any per-channel
  signing requirements.
- The public version scheme. Current `versionName` is `1.39.3` (inherited from upstream) with a
  default `versionCode` fallback of `2026012400`; nightly overrides the version code by date.

## Release Readiness

Gate before any public distribution build. Plain bullets; do not convert to status checkboxes.

- All required CI checks green on the release commit: `agent-docs`, `build-and-test`,
  `migration-tests` (see `docs/engineering/code-review.md` merge gate).
- Exported Room schema for the current database version is committed and reviewed; resolve the
  schema-export gap (versions 25–37) before a release that touches persistence. See
  `docs/engineering/verification.md`.
- Version bump applied deliberately: confirm `versionName` and `versionCode` in
  `app/app/build.gradle.kts` are correct for the release channel (not left at the nightly
  date-based override or the upstream `1.39.3` default).
- Signing confirmed by the owner: correct keystore and secrets configured for the chosen release
  channel (see Nightly Artifacts & Signing). Owner input required.
- Fresh-context review of the final diff per `docs/engineering/code-review.md`: an independent
  reviewer (separate thread/worktree) checks acceptance criteria, focus policy, launch routing,
  persistence/migrations, and entry-point consistency against the final diff — not the writer's
  transcript. No unresolved high-severity findings.
- Pixel 8 smoke test passed on a fresh build per `docs/engineering/pixel-smoke-test.md`: install
  and first launch, essential launch, distracting gate, temporary unlock, active session lock,
  session expiry and restart recovery, time reminders, and crash diagnostics. Record which pass
  completed as release evidence.
- `PROJECT_STATUS.md` updated with the verified release state and the date; durable follow-ups
  pushed to GitHub Issues (see `docs/engineering/work-management.md`).

## Proposed CI Changes

Recommendations only. Not applied. Each is optional and an owner decision.

- **Gate exported-schema drift.** Have `migration-tests` fail (not warn) when the exported schema
  for the current `AppDatabase` version is missing, and export versions 25–37 so the check is
  meaningful. Today the missing-schema branch only emits `::warning`.
- **Upload the debug APK artifact.** `build-and-test` builds `app-default-debug.apk` but discards
  it; uploading it (as `build-nightly.yml` does for the nightly APK) would give reviewers an
  installable build per PR.
- **Add a release-build dry run.** A `workflow_dispatch` job that assembles the release/nightly
  variant on demand (using the existing secrets) to validate signing wiring before a real release,
  without waiting for the 4am schedule.
- **Make lint a non-blocking CI step.** Run a lint task and surface results (kept non-fatal while
  `abortOnError = false`) so regressions are visible without breaking the build.
- **Align JDK versions.** `deploy-docs.yml` uses JDK 17 while every Gradle gate uses JDK 21;
  pin docs to 21 for consistency unless Dokka requires 17.
- **Pin third-party actions to commit SHAs.** Public-distribution hardening:
  `ReactiveCircus/android-emulator-runner` and the `gradle/actions`/`actions/*` uses are pinned to
  tags, not SHAs.
- **Surface a release workflow.** Add a tagged-release workflow (on `v*` tags) that builds the
  signed release artifact, attaches it to a GitHub Release, and records the smoke-test evidence
  link — once the owner decides the channel and signing identity.
