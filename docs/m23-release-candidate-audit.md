# M23.1 current-head release-candidate audit

Date: July 16, 2026

Status: local non-secret audit and Linux signing helper complete; protected signing run and Play-delivered installation remain open

## Outcome

Commit `041a811` builds cleanly without signing inputs and has the expected production identity, permissions,
Media3 service declaration, dependency set, license notices, backup exclusions, and 16 KB packaging. The generated AAB
and APK are intentionally unsigned local inspection artifacts. They are not tester candidates and must not be uploaded
or distributed.

M23.1 completes only after the owner regenerates the protected signed artifacts from the selected final commit, verifies
the stable upload certificate, confirms that version code 2 is still eligible in Play Console, and validates a
Play-delivered install/update. The established Windows DPAPI copy remains intact. Routine signing can now use the
existing authenticated recovery package through the Linux helper without persisting a plaintext keystore or credential
file; no production signing material was accessed while implementing or testing that path.

## Release identity and manifest

| Field | Verified value |
| --- | --- |
| Application ID | `com.codeframe78.twentyfourseven.player` |
| App label | `24Seven.FM Player` |
| Version | `0.1.0-alpha01` / code 2 |
| SDK range | minimum 26, target 36 |
| Launcher | density-specific legacy plus adaptive and Android 13 monochrome resources |
| Service | `RadioPlaybackService`, exported Media3 session service, `mediaPlayback` foreground-service type |

The merged release manifest contains only the expected app permissions: Internet, network state, wake lock, Android
13+ notifications, foreground service, and foreground media playback. The build-generated dynamic-receiver permission
is signature-protected. The exported service and accepting `onGetSession` shape follow Media3's documented playback
service contract; Media3's default session callback limits untrusted controllers to read access.

`allowBackup="false"` is now reinforced by explicit Android 11-and-earlier full-backup rules and Android 12+ cloud and
device-transfer rules. Every app-private backup domain, including shared preferences that can hold protected station
session state and local safety preferences, is excluded.

## Local artifact evidence

| Artifact | Size | SHA-256 | Signing state |
| --- | ---: | --- | --- |
| `app-release.aab` | 17,489,217 bytes | `3ddf85453d423642a5b7a511cbaa6a0478a362112306836f05038b2e6fe9c2fd` | Intentionally unsigned |
| `app-release-unsigned.apk` | 17,967,922 bytes | `3ad68fdd18936620cdf4556805325ca488da9acb067e3a1be92f69958abe46c0` | Intentionally unsigned |

`jarsigner -verify -strict -verbose -certs` explicitly classified the AAB as unsigned, which is the expected result when
the four `TWENTYFOURSEVEN_UPLOAD_*` values are absent. The textual classification is authoritative because `jarsigner`
can return exit code 0 for an unsigned JAR. These hashes identify only this local audit snapshot; the final protected
build will have its own hash.

## 16 KB packaging

- Android `zipalign -c -P 16 -v 4` passed for the release APK.
- The AAB contains only `libandroidx.graphics.path.so`, for `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64`.
- Every ELF `LOAD` segment in every ABI reports `0x4000` alignment.
- The established API 35 16 KB runtime suite also remains green; Play-generated split delivery remains an M23.6 gate.

## Dependencies and notices

The resolved release runtime was captured with Gradle. It contains AndroidX/Compose/Media3, Coil, OkHttp/Okio,
Kotlin/kotlinx, jsoup, Guava, Accompanist, JetBrains Compose transitive modules, annotations, jspecify, and Android core
library desugaring. It contains no advertising, analytics, crash-reporting, or developer-backend SDK.

The audited license families are:

- Apache License 2.0 for the AndroidX, Google, JetBrains, Kotlin, Coil, Square, and related support components;
- MIT for jsoup 1.22.2;
- GPLv2 with the Classpath Exception for `desugar_jdk_libs` 2.1.5; and
- Mozilla Public License 2.0 for the Public Suffix List data embedded by OkHttp.

The repository inventory and source links are in [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md). A bounded copy
is packaged as `res/raw/third_party_notices.txt` and is reachable natively from **More → Privacy → Open-source
licenses**. The upstream jsoup license and OkHttp Public Suffix List notice are also retained in the release AAB.

## Verification

- `:app:compileDebugKotlin` and `:app:compileDebugAndroidTestKotlin` — passed.
- `:app:testDebugUnitTest` — 132/132 passed.
- Focused `openSourceLicensesArePackagedAndReachableFromMore` on the API 35 Pixel Tablet — passed after final changes.
- `:app:lintDebug` — passed with 0 errors and 27 non-blocking warnings; the new resource-access and backup-rule
  warnings were resolved.
- `:app:connectedDebugAndroidTest` — 40/40 passed on the API 35 Pixel Tablet, including full 1,500-track Favorites
  traversal.
- Combined `:app:bundleRelease :app:assembleRelease` and release lint-vital — passed from commit `041a811` after the
  Favorites performance and playback-recovery changes.
- Release APK 16 KB ZIP alignment — passed.
- AAB resource inspection — packaged license notice and both backup-rule resources present.
- Merged release manifest inspection — identity, target, permissions, launcher, backup, and service declarations passed.
- `git diff --check` — passed.

The remaining lint warnings are dependency-update advisories, documented MediaSession-service export, legacy/adaptive
launcher heuristics, test-only station playlist fixtures, and optional Kotlin convenience suggestions. None is a build
or Play-readiness error; dependency/toolchain upgrades remain intentionally outside this release audit.

## Protected completion procedure

From Ubuntu, first verify the non-secret prerequisites:

```bash
python3 scripts/validate-protected-play-bundle-linux.py --check-environment
```

Then, after confirming the final commit and version-code availability, run the protected build from an interactive
terminal. Keep the encrypted package outside the repository and enter its passphrase only at the hidden prompt:

```bash
python3 scripts/validate-protected-play-bundle-linux.py \
  --recovery-package /absolute/path/outside-the-repository/24seven-upload.24seven-recovery \
  --build-apk
```

The helper authenticates the AES-256-GCM recovery envelope, verifies the embedded keystore hash and the exact registered
upload certificate, materializes the JKS only in memory-backed `/dev/shm`, launches a non-persistent Gradle process,
removes the temporary material, and verifies the signed AAB certificate. It prints only the resulting AAB hash and
certificate fingerprint. Five synthetic tests cover success, wrong passphrase, wrong registered certificate,
mismatched keystore hash, repository-local package rejection, temporary mode `600`, and cleanup.

The owner-controlled Windows DPAPI path remains a backup:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\scripts\validate-protected-play-bundle.ps1 -BuildApk
```

Record the new AAB/APK hashes and confirm the AAB signer remains upload-certificate SHA-256
`F6E8E81271964FFC3F8A0D548B49B4DB93AEFC48CCB74B8744512670F4279E3F`. Supply no key path, alias, or password to Git,
chat, logs, or Play listing text. Then upload through the authorized test-track workflow and record Play's artifact
inspection, install result, and subsequent same-signing-lineage update result.
