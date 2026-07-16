# Ubuntu Codex CLI development setup

The migrated project includes an idempotent Ubuntu bootstrap at
`scripts/bootstrap-ubuntu.sh`. It supports x86_64 Ubuntu-family distributions
and installs the complete local development environment for this repository:

- OpenJDK 17, Git, Git LFS, GitHub CLI, Node.js, Python with AES-GCM support, and common build tools;
- Android SDK command-line tools revision 14742923;
- Android SDK Platforms 35 and 36, Build Tools 36.1.0, and Platform Tools;
- Google APIs x86_64 emulator images and AVDs for API 35 and API 36;
- Ubuntu USB `udev` rules plus `plugdev` membership for physical-device ADB;
- KVM/libvirt packages and group membership for accelerated emulators;
- Android Studio Quail 2 (2026.1.2.10) under `/opt/android-studio`;
- the ChatGPT Codex CLI from OpenAI's official standalone installer;
- persistent `JAVA_HOME`, `ANDROID_HOME`, `STUDIO_GRADLE_JDK`, and tool paths;
- a final `:app:compileDebugKotlin` validation without running `clean`.

Run it from the repository root as your normal user:

```bash
bash scripts/bootstrap-ubuntu.sh --accept-licenses
```

`--accept-licenses` confirms acceptance of the Android SDK licenses. Omit it to
review and accept the licenses interactively. Run the script with `--help` for
options that skip Android Studio, Codex, AVD creation, or build verification.

After installation, open a new terminal. If the script added `plugdev`, `kvm`,
or `libvirt` membership, log out and back in once before using USB ADB or the
emulator.

Authenticate Codex with the ChatGPT subscription attached to the account:

```bash
codex login
codex login status
```

On a remote or headless Ubuntu host, use device-code authentication instead:

```bash
codex login --device-auth
```

Do not copy `~/.codex/auth.json` into this repository. It can contain reusable
access tokens. API-key login is not required for ChatGPT subscription access.

Useful verification commands:

```bash
java -version
sdkmanager --version
adb devices -l
emulator -accel-check
codex --version
android-studio
bash ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon
```

The installer deliberately does not create `local.properties`; Gradle inherits
the persistent `ANDROID_HOME` configuration. It also does not migrate signing
keys, recovery passphrases, station credentials, cookies, or Codex credentials.

## Protected Play signing on Ubuntu

Routine Play upload signing can use the existing encrypted recovery package without restoring a permanent plaintext
keystore to the unencrypted Ubuntu filesystems. Check the non-secret prerequisites with:

```bash
python3 scripts/validate-protected-play-bundle-linux.py --check-environment
```

For an authorized release build, keep the `.24seven-recovery` package outside the repository and run:

```bash
python3 scripts/validate-protected-play-bundle-linux.py \
  --recovery-package /absolute/path/outside-the-repository/24seven-upload.24seven-recovery \
  --build-apk
```

Enter the owner-held passphrase only at the hidden terminal prompt. The helper enforces the registered upload
certificate, keeps the recovered keystore under memory-backed `/dev/shm` for the duration of a single non-daemon Gradle
build, and removes it before reporting the AAB hash. It never stores or prints the passphrase, passwords, alias, or
keystore path. The encrypted off-PC recovery package and Windows DPAPI copy remain independent backups.

Official references:

- [Android Studio downloads and command-line tools](https://developer.android.com/studio)
- [Android SDK Manager](https://developer.android.com/tools/sdkmanager)
- [Android emulator acceleration on Linux](https://developer.android.com/studio/run/emulator-acceleration)
- [Android hardware-device setup on Ubuntu](https://developer.android.com/studio/run/device)
- [Codex CLI](https://developers.openai.com/codex/cli)
- [Codex authentication](https://developers.openai.com/codex/auth)
