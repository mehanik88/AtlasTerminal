# AtlasTerminal Repository Guide

## Scope

These instructions apply to the entire repository.

## Project purpose and safety

AtlasTerminal is an Android terminal for automotive head units. It can execute commands through
the device's local or remote `adbd`, a discovered Telnet shell, or a local `su`/`sh` process.
Commands are intentionally powerful and may change system state. Treat every execution-path,
root-target, transport, and timeout change as safety-sensitive.

- The package name is `com.mmwtl.atlasterminal`; do not change it without an explicit migration
  request.
- Keep ADB, Telnet, and local-process behavior behind the existing core adapters. Keep UI state in
  `TerminalViewModel` and keep pure parsing/policy logic testable without a device.
- Preserve finite connection and command timeouts. A timeout must close the underlying socket or
  process; coroutine cancellation alone is not sufficient for blocking I/O.
- Keep generated ADB keys in private no-backup storage. Never log, bundle, or commit private key
  material.
- Do not add one-tap presets for destructive `mount`/`remount` operations. Read-only inspection
  commands are acceptable; arbitrary commands entered by the user remain the user's responsibility.
- Treat command output as data. Do not infer transport success by searching for human-readable
  error phrases when a structured result is available.

## Versioning and artifact naming

- `main` is the only release branch.
- Increase the base `versionCode` and the semantic patch component of the base `versionName` only
  when building a completed release from `main`, unless the user explicitly requests another
  release number.
- Builds from any other branch must leave the base `versionCode` and `versionName` unchanged. The
  visible effective version must append a sanitized branch suffix, for example `1.0.0-seek`.
- Sanitize branch names by replacing runs outside `[A-Za-z0-9._-]` with `-`, trimming leading and
  trailing `-`, lowercasing, and limiting the suffix to 32 characters. Use `detached` when no
  branch is available.
- Never include the branch name or suffix in `versionCode`.
- Keep the effective version in the APK/archive name:
  `<effectiveVersionName>[<versionCode>]AtlasTerminal`.
- A single user-requested batch is one version increment, even if it changes several files.

## Build and verification

Use the repository Gradle Wrapper with JDK 17 and Android SDK 36:

```bash
ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}" sh gradlew \
  verify :app:assembleDebug
```

Always build the release variant before handing off a completed change and verify its signature:

```bash
ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}" sh gradlew \
  -Pbranch.name=main verify :app:assembleDebug :app:assembleRelease
apksigner verify --verbose --print-certs app/build/outputs/apk/release/*.apk
```

Release packaging must fail closed without private signing material. Configure the ignored
`secure.signing.gradle` at the repository root and its referenced keystore; never assign the
debug signing config to `release`, weaken the signing requirement, or commit credentials,
keystores, SDK paths, generated APKs, or Gradle caches.

Before handoff:

- run JVM unit tests and Android lint through `verify`;
- build both debug and release variants;
- inspect release package/version metadata and verify the APK certificate;
- validate XML/resources and check that only intended files changed;
- state explicitly when real-device ADB, root, or head-unit behavior was not exercised.

## Source and UI guidelines

- Keep Android framework and transport code at the edges; keep command parsing, endpoint policy,
  colorization, and state transitions small and testable.
- Keep user-visible text in Android string resources where practical.
- Maintain the Atlas graphite palette: `#171717` background, `#262626` cards, `#333333` nested
  surfaces, `#F5F5F5` primary text, `#D4D4D4` secondary text, and `#7893A0` accent.
- Keep adaptive, round, and Android 13 monochrome launcher resources in sync.
- Prefer accessibility semantics and keyboard/input affordances that work on the tested head-unit
  form factor. Do not claim that emulator behavior proves OEM head-unit behavior.

## Tests

Add regression coverage for changed pure behavior. At minimum preserve tests for endpoint parsing,
ADB marker/timeout handling, Telnet discovery/response parsing, command colorization, preset
composition, and history/persistence behavior. Unit tests must not require a running `adbd`, root,
the emulator, or private signing files.

## Repository hygiene

- Keep `gradlew` executable; source, XML, and image files are not executable.
- Preserve unrelated user changes in a dirty worktree.
- Do not amend, rebase, push, or rewrite history unless explicitly requested.
- If this checkout is under Git, create a clear commit after a verified improvement only when that
  repository's workflow requests automatic commits; never stage unrelated changes.
