# UserCA Bridge prototype

User requested immediate implementation: no further design gates, no local Android builds, push to `takattowo/UserCABridge` and build with GitHub Actions.

## Scope

- Modern libxposed API `102.0.0`; Android 8–16 (API 26–36), provisional until device tests.
- Hook Android framework `NetworkSecurityConfig.getDefaultBuilder` inside every LSPosed-scoped process, including app processes. API 102 cannot inject globally into zygote.
- Append the existing Android user certificate source to the original builder. Keep system sources, cleartext policy, pins, and certificate transparency unchanged.
- Added reference uses `overridesPins=false`; Android 16 also uses `disableCT=false`.
- Discover runtime members and check their shapes against inspected AOSP source. Unknown implementations fail safely with diagnostics.
- No certificate writes, system-root classification spoofing, trust-manager replacement, Chrome hooks, AdGuard status-check hooks, native hooks, or settings UI.

## Approaches

1. Extend framework defaults: selected for smallest prototype; respects explicit app trust-anchor lists and pinning.
2. Merge user certificates into system sources: rejected because it also changes explicit app policies and can inherit pin exemptions.
3. Replace TLS verification or spoof system-root status: rejected because it changes validation/security policy.

## Delivery and limits

GitHub Actions installs SDK 37 (required by the published API AAR), uses AGP 9.2.1 / Gradle 9.4.1 / JDK 17, runs JVM tests and lint, and uploads signed debug APK. No signing secret required; default debug key changes on fresh runners, so subsequent artifacts may require uninstalling the old module before installing.

Explicit XML anchor lists and custom/native trust stores remain unchanged. Selecting only System Framework in LSPosed does not cover apps. AdGuard's system-store status is unchanged. Runtime tests must verify HTTPS success with a user CA, invalid chain/hostname rejection, applicable pin rejection, and certificate-update behavior.
