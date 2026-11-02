# UserCA Bridge

Small LSPosed module that includes Android's existing user-installed CA source alongside system CAs in **framework default trust configurations**. Certificates stay where Android installed them. No certificate files are modified, copied, or moved.

## Requirements

- Android 8–16 (API 26–36). Runtime compatibility is provisional until tested on your device; unknown versions/implementations are skipped with a warning.
- LSPosed-compatible framework implementing **modern libxposed API 102**. Older legacy/API-100 builds are not supported.
- User CA installed through Android Settings in the same Android user/profile as the target app.

## Install and scope

1. Install the debug APK from GitHub Actions.
2. Enable **UserCA Bridge** in LSPosed.
3. Select every app you want affected. You can additionally select **System Framework** for supported framework-process configurations.
4. Restart selected apps; reboot if System Framework is selected.

**Selecting only System Framework does not affect all apps.** API 102 loads modules in scoped processes, not zygote. This module hooks the same Android framework API independently in each scoped process, without app-specific/Chrome hooks. It has no launcher/settings screen.

## What changes

After Android's original `NetworkSecurityConfig.getDefaultBuilder` runs, the module adds `UserCertificateSource` if absent. Method parameters are discovered at runtime, not assumed. Known AOSP constructors are checked against the Android version:

- API 26–35: `(CertificateSource, boolean overridesPins)`.
- API 36: additionally `boolean disableCT`.

New references use `overridesPins=false` and, when present, `disableCT=false`. System sources, existing user references, hostname verification, chain validation, application pins, cleartext policy, and certificate-transparency policy are left unchanged. Source-verified callers are deoptimized to address ART inlining. Hook failures retain the original factory result; original factory exceptions are not swallowed.

## Limits

- This extends **defaults**, not every explicit app trust policy. Apps with explicit XML trust-anchor lists, private trust managers, bundled/native trust stores, or inaccessible user stores may remain unaffected.
- No pinning bypass. Existing app/browser local-CA exemptions remain as provided by that app/browser.
- Trusting a user CA permits its holder to issue certificates accepted by affected apps. Only install CAs you control/trust, and limit LSPosed scope accordingly.
- AdGuard's “installed in system store” status remains unchanged. This module does not hook AdGuard's checks, activate its HTTPS filtering, route traffic, or guarantee interception. Modern Chrome already has a separate user-root trust path.
- User-store certificates are all included, not only AdGuard's certificate. Android's usual per-profile access and trust-storage updates apply.

## Remote build and download

No local build required. Open [Build debug APK](https://github.com/takattowo/UserCABridge/actions/workflows/build.yml), select **Run workflow**, then download **UserCA-Bridge-debug** from the successful run's artifacts. Pushes to `main` and pull requests also run checks.

GitHub Actions installs JDK 17, Gradle 9.4.1, SDK 37 and build tools 36.0.0, then runs:

```sh
gradle --no-daemon --stacktrace testDebugUnitTest lintDebug assembleDebug
```

The published libxposed 102.0.0 AAR requires `compileSdk=37`; that is a build dependency, not a claim of Android 17 runtime support. AGP is pinned to 9.2.1.

**No signing secret needed.** Android's debug signing key is generated on the runner. Fresh runners can generate different keys: uninstall the previous debug APK before installing a new artifact if Android reports incompatible signatures. This also resets LSPosed module scope; select it again. APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Verify on device

In LSPosed logs, search **UserCA Bridge**. Expected messages identify process/API/SDK, discovered factory and certificate-reference signatures, installed hooks, executed defaults, and user-certificate count. Store-read failures and unsupported implementations produce warnings without changing TLS verification outcomes. Certificates/keys are not logged.

Test a scoped app using framework defaults against controlled TLS endpoints:

- Valid chain signed by installed user CA succeeds; ordinary system-CA HTTPS still succeeds.
- Wrong hostname, expired leaf, invalid signature, and untrusted CA still fail.
- Applicable non-overridden pin mismatch still fails.
- Installing/removing the user CA updates trust after Android's trust-storage notification; restart the app if it cached trust.
- An unscoped app and an app with explicit restrictive anchors remain unchanged.

JVM tests exercise the reflection bridge against source-shaped fixtures; they do not prove device hooks, actual TLS handshakes, AdGuard behavior, or OEM compatibility.

## Source references

- [AOSP Android 8.1 default trust](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-8.1.0_r1/core/java/android/security/net/config/NetworkSecurityConfig.java)
- [AOSP Android 9 default trust](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-9.0.0_r1/core/java/android/security/net/config/NetworkSecurityConfig.java)
- [AOSP Android 15 certificate references](https://github.com/aosp-mirror/platform_frameworks_base/blob/android15-release/core/java/android/security/net/config/CertificatesEntryRef.java)
- [AOSP Android 16 default trust and CT](https://github.com/aosp-mirror/platform_frameworks_base/blob/android16-release/core/java/android/security/net/config/NetworkSecurityConfig.java)
- [AOSP Android 16 XML inheritance](https://github.com/aosp-mirror/platform_frameworks_base/blob/android16-release/core/java/android/security/net/config/XmlConfigSource.java)
- [libxposed API 102 source](https://github.com/libxposed/api/tree/102.0.0)
