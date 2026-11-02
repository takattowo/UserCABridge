# UserCA Bridge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Minimal fileless framework-default user-CA trust extension and remote debug APK build.

**Architecture:** An API-102 entry point installs framework hooks at process module load. A package-private reflection helper validates runtime members and appends a non-pin-overriding user reference to the original builder.

**Tech Stack:** Java 17, libxposed 102.0.0, AGP 9.2.1, Gradle 9.4.1, SDK 37, JUnit 4, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-11-03-userca-bridge-design.md`

## Global Constraints

- No local Android build or JVM compilation; build and compiled tests run remotely only.
- No certificate-file modifications or verification/pinning bypasses.
- Android runtime API 26–36; skip unknown versions safely.
- No automatic scope grants; user chooses app processes in LSPosed.

## Review Focus

- Existing user reference: do not duplicate or change its flags.
- Unknown constructor: skip rather than guess boolean semantics.
- Original factory failure: propagate the original failure, not success.
- User-store read failure: preserve application behavior and log failure.
- ART inlining: deoptimize source-verified direct callers and log failures.

### Task 1: Framework bridge and remote checks

**Files:** `app/src/main/java/io/github/takattowo/usercabridge/{UserCABridge,FrameworkTrust}.java`, matching `app/src/test/.../FrameworkTrustTest.java`, Gradle files, manifest, `META-INF/xposed` entry files, `.github/workflows/build.yml`, `README.md`.

**Interfaces:** `FrameworkTrust.resolve(Class<?> config, Class<?> builder, Class<?> user, Class<?> source, Class<?> entry, int sdk)` validates factory/member shapes. `addUserSource(Object builder)` returns whether it appended a reference. `getUserCertificateCount()` reads the platform source without modifying it.

- [ ] Write behavioral reflection tests first: append user source alongside system; preserve pins/cleartext/CT; reject unknown constructor; avoid duplicates; identify changed factory parameter lists without fixed signatures; report store-read failure.
- [ ] Add build configuration and API-102 process entry point; call original factory exactly once, then modify its builder inside protective error handling.
- [ ] Discover and log hook signatures; deoptimize known AOSP direct callers without parameter assumptions.
- [ ] Run remote `gradle --no-daemon testDebugUnitTest lintDebug assembleDebug`; expected tests/lint pass and signed APK artifact appears. If GitHub authentication blocks this, record verification as pending, never passing.
- [ ] Review, commit, and push without force. Observe remote workflow and publish artifact link if available.
