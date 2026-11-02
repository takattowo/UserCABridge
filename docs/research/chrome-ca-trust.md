# Chrome / AdGuard user-CA trust

Research date: 2026-11-03. Primary-source analysis only; no device, APK, hook, build, or installation tested. Chromium links pin source snapshot `87bf8aae70d601864397fb3daed45d02a6c8f0a5`; Android 16 links pin `99b01a65cc4c104933788b3143285ab6bae65827`. Source snapshots are not proof of the user's installed binary or runtime configuration.

## Bounded answer

**For ordinary current Android Chrome HTTPS over TCP, an already-installed AdGuard user CA should be trusted without any module. Adding user certificates to `NetworkSecurityConfig.getDefaultBuilder` is neither necessary nor the operative trust path for Chrome's current default verifier.** Chrome Root Store / Chrome Certificate Verifier became default on Android in Chrome 115. Its Android local-trust adapter imports user-installed certificates directly. [1][2][3]

Even Chrome's platform-backed path already opts into both `user` and `system` anchors in its own XML `<base-config>`. A default-builder change does not override that explicit anchor list. [4][5]

Therefore, for the clarified Chrome-only goal, recommended minimal hook scope is **none until a reproducible CA-trust failure establishes otherwise**. Trust admission and successful AdGuard interception are separate questions.

## Actual trust paths

- **Current default Chrome:** `CertVerifyProc::ImplParams` defaults `use_chrome_root_store=true`; verifier factory selects the built-in verifier. `SystemTrustStore` combines Chrome public roots with `TrustStoreAndroid`, whose implementation calls `net::android::GetUserAddedRoots()` and adds each returned certificate as a trust anchor. Java `X509Util.getUserAddedRoots()` enumerates `AndroidCAStore`, selects aliases beginning `user:`, and returns DER certificates. This path does not consult Android Network Security Config for local anchor admission. [2][3][6][7][21]
- **Platform-backed Android verifier:** native `CertVerifyProcAndroid` calls Android chain verification through Java `X509Util`; Java initializes the default `TrustManagerFactory` with `null`, invokes `X509TrustManagerExtensions.checkServerTrusted`, and returns the validated chain. Chrome's XML already includes user anchors. Common native `CertVerifyProc::Verify` separately checks `VerifyNameMatch(hostname)` and signature/key constraints. No accept-all verifier is needed. [4][7][8][6]
- **Other Android apps:** `getDefaultBuilder(ApplicationInfo)` supplies platform defaults; modern-target apps normally get system anchors only. Adding a separate non-pin-overriding user reference can affect apps/configurations inheriting those defaults, but XML lists explicitly replace inherited anchor lists. It does not cover private trust managers, bundled stores, arbitrary native verifiers, or every WebView/Cronet configuration. Chromium explicitly retains Android platform-verifier support for WebView and excludes Cronet from Chrome Root Store builds. [5][9][10]

## Known-root classification, CT, and pinning

**Trusted** is not synonymous with **publicly known/system root**.

- On the built-in Chrome Root Store path, `IsKnownRoot` means membership in Chrome Root Store, not membership in Android's system-source object. An AdGuard-only user root is locally trusted but not a known public root. [3]
- On the platform-backed path, Java `isKnownRoot` checks system certificate files plus `system:` aliases, matching subject/public key; native Android verification also recognizes built-in known-root SPKI hashes. Merely adding `UserCertificateSource` to a Network Security Config does not turn AdGuard into such a root. [7][8]
- **CT:** `RequireCTDelegate::CheckCTRequirements` returns `CT_NOT_REQUIRED` when `is_issued_by_known_root` is false; built-in verification calls this checker for candidate paths. Public-root CT enforcement remains. AdGuard's official rooted-device instructions warn that system-store certificates can cause Chromium CT failures and prescribe a user-installed Intermediate CA alongside its system-installed Personal CA. That is first-party operational advice, but the blanket wording about system roots must not be generalized to the newer Chrome Root Store path: current classification comes from Chrome Root Store membership. [11][12][22]
- **Chrome pinning:** Chromium already exempts local roots from its built-in public-key pin violations: when pins fail, `!is_issued_by_known_root && enable_pkp_bypass_for_local_trust_anchors_` yields `BYPASSED`; that setting defaults true. This is existing Chrome behavior, not a proposed module bypass. Consequently, “leave pinning code/policy untouched” is achievable; “every Chrome pin must still reject interception through a user CA” is not Chrome's stock policy. Do not spoof classification or hook pin checks to change this. [13][23]
- **Android XML pinning:** `NetworkSecurityTrustManager` validates the chain through Conscrypt, then checks pins, unless the selected anchor has `overridesPins=true`. `CertificatesEntryRef` attaches that flag to every certificate its source returns. Android sorts pin-overriding refs first; duplicate anchors prefer the overriding ref. [9][14][15]
- **Broad system-source merging risk:** merging user certificates into `SystemCertificateSource` is not inherently pin-bypassing when every relevant reference has `overridesPins=false`. But it expands any existing system-source reference with `overridePins=true`—including debug overrides—to these new user anchors. Those anchors can then acquire pin exemptions. It also changes explicit `src="system"` trust policy, unlike a default-only hook. Avoid global source merging. [5][9][14][15]

## Recommendation and limits

1. **Chrome-only:** retain genuine user-store installation; no trust hook, no certificate-file copying, no fake `system:` aliases, no known-root/result/hostname/pin hooks. AdGuard explicitly says its Personal CA in user storage enables browser filtering. [12][16]
2. **Only if broader platform-default support is separately requested:** after original `getDefaultBuilder(ApplicationInfo)` returns, append a separate user-source reference with `overridesPins=false`, only inside opted-in app processes. Leave explicit XML policies and existing validation untouched. Android 16's constructor also has `disableCT`; do not blanket-disable CT policy. Android 15 source uses a two-argument constructor, so hidden-API signatures need version checks. This is a limited default-trust extension, not universal “system CA equivalence.” [9][17]
3. **No end-to-end certificate-preservation claim:** AdGuard terminates two TLS connections. Chrome validates the AdGuard-issued chain; AdGuard, not Chrome, validates the original server chain. Official AdGuard docs describe that validation, but it was not audited or tested here. Strict application pinning can correctly prevent interception and should remain a failure or filtering exclusion. [18][14]

Remaining unknowns requiring the user's environment:

- Installed Chrome version/fork, Android/OEM version, actual verifier selection, enterprise policy, and trust-store cache state.
- Correct AdGuard Personal CA installed in the same Android user/profile as Chrome, and whether AdGuard regenerated it. AdGuard's official troubleshooting suggests restarting AdGuard and reinstalling the correct CA when installation is ignored. [16]
- Whether AdGuard HTTPS filtering is enabled, Chrome traffic is routed through its VPN/proxy, and filtering exclusions apply. A default-trust hook neither enables filtering nor routes packets; exclusions are documented. [18]
- TCP versus QUIC/HTTP3. Chromium's official FAQ says TCP considers local trust additions, whereas QUIC only considers local trust removals. User-root trust over TCP does not establish QUIC interception support; exact AdGuard handling/fallback remains untested. [1]
- Exact Chrome error / NetLog and AdGuard request log. Missing filtering alone does not identify an unknown-CA failure. Positive trust, expired/bad-signature/wrong-host rejection, pin-policy behavior, and certificate-change reload require runtime verification.

## LSPosed API note

For any later, separately authorized module work, Maven Central reports latest/release `io.github.libxposed:api:102.0.0`; official libxposed README identifies API 102, `minSdk 26`, and `compileOnly` integration. This is modern libxposed API, not a reason to implement an unnecessary Chrome hook. Framework/API-102 runtime compatibility still needs verification against the user's installed LSPosed version. [19][20]

## Primary sources

[1]: https://chromium.googlesource.com/chromium/src/+/87bf8aae70d601864397fb3daed45d02a6c8f0a5/net/data/ssl/chrome_root_store/faq.md
[2]: https://chromium.googlesource.com/chromium/src/+/87bf8aae70d601864397fb3daed45d02a6c8f0a5/net/cert/internal/trust_store_android.cc
[3]: https://chromium.googlesource.com/chromium/src/+/87bf8aae70d601864397fb3daed45d02a6c8f0a5/net/cert/internal/system_trust_store.cc
[4]: https://chromium.googlesource.com/chromium/src/+/87bf8aae70d601864397fb3daed45d02a6c8f0a5/chrome/android/java/res_base/xml/network_security_config.xml
[5]: https://github.com/aosp-mirror/platform_frameworks_base/blob/99b01a65cc4c104933788b3143285ab6bae65827/core/java/android/security/net/config/XmlConfigSource.java
[6]: https://chromium.googlesource.com/chromium/src/+/87bf8aae70d601864397fb3daed45d02a6c8f0a5/net/cert/cert_verify_proc.cc
[7]: https://chromium.googlesource.com/chromium/src/+/87bf8aae70d601864397fb3daed45d02a6c8f0a5/net/android/java/src/org/chromium/net/X509Util.java
[8]: https://chromium.googlesource.com/chromium/src/+/87bf8aae70d601864397fb3daed45d02a6c8f0a5/net/cert/cert_verify_proc_android.cc
[9]: https://github.com/aosp-mirror/platform_frameworks_base/blob/99b01a65cc4c104933788b3143285ab6bae65827/core/java/android/security/net/config/NetworkSecurityConfig.java
[10]: https://chromium.googlesource.com/chromium/src/+/87bf8aae70d601864397fb3daed45d02a6c8f0a5/net/features.gni
[11]: https://chromium.googlesource.com/chromium/src/+/87bf8aae70d601864397fb3daed45d02a6c8f0a5/net/cert/require_ct_delegate.cc
[12]: https://adguard.com/kb/adguard-for-android/solving-problems/https-certificate-for-rooted/
[13]: https://chromium.googlesource.com/chromium/src/+/87bf8aae70d601864397fb3daed45d02a6c8f0a5/net/cert/key_pinning_state.cc
[14]: https://github.com/aosp-mirror/platform_frameworks_base/blob/99b01a65cc4c104933788b3143285ab6bae65827/core/java/android/security/net/config/NetworkSecurityTrustManager.java
[15]: https://github.com/aosp-mirror/platform_frameworks_base/blob/99b01a65cc4c104933788b3143285ab6bae65827/core/java/android/security/net/config/CertificatesEntryRef.java
[16]: https://adguard.com/kb/adguard-for-android/solving-problems/manual-certificate/
[17]: https://github.com/aosp-mirror/platform_frameworks_base/blob/android15-release/core/java/android/security/net/config/NetworkSecurityConfig.java
[18]: https://adguard.com/kb/general/https-filtering/what-is-https-filtering/
[19]: https://repo.maven.apache.org/maven2/io/github/libxposed/api/maven-metadata.xml
[20]: https://github.com/libxposed/api/blob/master/README.md
[21]: https://chromium.googlesource.com/chromium/src/+/87bf8aae70d601864397fb3daed45d02a6c8f0a5/net/cert/cert_verifier.cc
[22]: https://chromium.googlesource.com/chromium/src/+/87bf8aae70d601864397fb3daed45d02a6c8f0a5/net/cert/cert_verify_proc_builtin.cc
[23]: https://chromium.googlesource.com/chromium/src/+/87bf8aae70d601864397fb3daed45d02a6c8f0a5/net/cert/key_pinning_state.h
