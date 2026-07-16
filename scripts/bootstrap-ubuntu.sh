#!/usr/bin/env bash

set -Eeuo pipefail
IFS=$'\n\t'

readonly SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly REPO_ROOT="$(cd -- "$SCRIPT_DIR/.." && pwd)"
readonly CMDLINE_TOOLS_REVISION="14742923"
readonly CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_REVISION}_latest.zip"
readonly CMDLINE_TOOLS_SHA256="04453066b540409d975c676d781da1477479dde3761310f1a7eb92a1dfb15af7"
readonly STUDIO_VERSION="2026.1.2.10"
readonly STUDIO_URL="https://edgedl.me.gvt1.com/android/studio/ide-zips/${STUDIO_VERSION}/android-studio-quail2-linux.tar.gz"
readonly STUDIO_SHA256="64445a54092e7056c6eb7f1a89ad116d0feec2ef5f965b8e594d62abdb58590f"

ANDROID_SDK_DIR="${ANDROID_HOME:-$HOME/Android/Sdk}"
ACCEPT_LICENSES=false
INSTALL_ANDROID_STUDIO=true
INSTALL_CODEX=true
CREATE_AVDS=true
VERIFY_BUILD=true
TEMP_DIR=""

usage() {
    cat <<'EOF'
Install the Ubuntu dependencies for 24Seven.FM Player.

Usage:
  bash scripts/bootstrap-ubuntu.sh [options]

Options:
  --accept-licenses       Non-interactively accept Android SDK licenses.
                          By using this option, you confirm that you accept them.
  --sdk-root PATH         Android SDK location (default: $HOME/Android/Sdk).
  --skip-android-studio   Install the CLI toolchain without Android Studio.
  --skip-codex            Do not install the ChatGPT Codex CLI.
  --skip-avds             Install emulator images without creating AVDs.
  --skip-build            Do not run the final Gradle compile verification.
  -h, --help              Show this help.

Without --accept-licenses, sdkmanager opens its normal interactive license flow.
Do not run this script with sudo; it requests sudo only for system packages.
EOF
}

log() {
    printf '\n==> %s\n' "$*"
}

warn() {
    printf '\nWARNING: %s\n' "$*" >&2
}

die() {
    printf '\nERROR: %s\n' "$*" >&2
    exit 1
}

cleanup() {
    if [[ -n "$TEMP_DIR" && -d "$TEMP_DIR" ]]; then
        rm -rf -- "$TEMP_DIR"
    fi
}
trap cleanup EXIT

while (($# > 0)); do
    case "$1" in
        --accept-licenses)
            ACCEPT_LICENSES=true
            ;;
        --sdk-root)
            (($# >= 2)) || die "--sdk-root requires a path"
            ANDROID_SDK_DIR="$2"
            shift
            ;;
        --skip-android-studio)
            INSTALL_ANDROID_STUDIO=false
            ;;
        --skip-codex)
            INSTALL_CODEX=false
            ;;
        --skip-avds)
            CREATE_AVDS=false
            ;;
        --skip-build)
            VERIFY_BUILD=false
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            die "Unknown option: $1"
            ;;
    esac
    shift
done

[[ $EUID -ne 0 ]] || die "Run this script as your normal user, not with sudo."
[[ "$(uname -s)" == "Linux" ]] || die "This script supports Ubuntu Linux only."
[[ "$(uname -m)" == "x86_64" ]] || die "Android Studio and these emulator images require x86_64 Linux."

if [[ -r /etc/os-release ]]; then
    # shellcheck disable=SC1091
    source /etc/os-release
    case "${ID:-}" in
        ubuntu|pop|linuxmint) ;;
        *)
            warn "Detected ${PRETTY_NAME:-an unknown distribution}. The apt package list is tested on Ubuntu-family systems."
            ;;
    esac
fi

command -v sudo >/dev/null || die "sudo is required for system package installation."
sudo -v

TEMP_DIR="$(mktemp -d -t 24seven-ubuntu-bootstrap.XXXXXX)"

available_kb="$(df -Pk "$HOME" | awk 'NR == 2 {print $4}')"
if [[ "$available_kb" =~ ^[0-9]+$ ]] && ((available_kb < 33554432)); then
    warn "Less than 32 GiB is free under $HOME. Android Studio plus two AVDs may exhaust the disk."
fi

log "Installing Ubuntu packages"
sudo dpkg --add-architecture i386
sudo apt-get update

required_packages=(
    android-sdk-platform-tools-common
    bridge-utils
    build-essential
    ca-certificates
    cpu-checker
    curl
    gh
    git
    git-lfs
    gnome-keyring
    jq
    libgl1
    libnss3
    libsecret-tools
    libvirt-clients
    libvirt-daemon-system
    nodejs
    openjdk-17-jdk
    openssh-client
    python3
    python3-cryptography
    python3-venv
    # Ubuntu 26.04 exposes qemu-kvm only as a virtual package. This explicit
    # provider also exists on older supported Ubuntu releases.
    qemu-system-x86
    rsync
    shellcheck
    unzip
    usbutils
    xdg-utils
    zip
)
sudo env DEBIAN_FRONTEND=noninteractive apt-get install -y "${required_packages[@]}"

# NodeSource bundles npm with nodejs, while Ubuntu's native nodejs package may
# keep npm separate. Request npm only when the selected nodejs provider did not
# install it, avoiding a NodeSource package conflict.
if ! command -v npm >/dev/null; then
    sudo env DEBIAN_FRONTEND=noninteractive apt-get install -y npm
fi

# Google still documents these compatibility libraries for 64-bit Ubuntu. Some
# newer Ubuntu releases rename or retire individual packages, so install every
# package available in the active release instead of failing the whole setup.
compatibility_candidates=(
    libc6:i386
    libncurses5:i386
    libstdc++6:i386
    lib32z1
    libbz2-1.0:i386
)
compatibility_packages=()
for package in "${compatibility_candidates[@]}"; do
    if apt-cache show "$package" >/dev/null 2>&1; then
        compatibility_packages+=("$package")
    else
        warn "Ubuntu package $package is unavailable in this release; continuing."
    fi
done
if ((${#compatibility_packages[@]} > 0)); then
    sudo env DEBIAN_FRONTEND=noninteractive apt-get install -y "${compatibility_packages[@]}"
fi

git lfs install --skip-repo

git_error="$TEMP_DIR/git-repository-check.txt"
if ! git -C "$REPO_ROOT" rev-parse --git-dir >/dev/null 2>"$git_error"; then
    if grep -qi "dubious ownership" "$git_error"; then
        log "Trusting the exact migrated repository path for Git"
        git config --global --add safe.directory "$REPO_ROOT"
        git -C "$REPO_ROOT" rev-parse --git-dir >/dev/null
    else
        cat "$git_error" >&2
        die "$REPO_ROOT is not a usable Git repository."
    fi
fi

for group in plugdev kvm libvirt; do
    if getent group "$group" >/dev/null && ! id -nG "$USER" | tr ' ' '\n' | grep -Fxq "$group"; then
        sudo usermod -aG "$group" "$USER"
    fi
done

JAVA_HOME_DIR="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
CMDLINE_TOOLS_DIR="$ANDROID_SDK_DIR/cmdline-tools/$CMDLINE_TOOLS_REVISION"
SDKMANAGER="$CMDLINE_TOOLS_DIR/bin/sdkmanager"
AVDMANAGER="$CMDLINE_TOOLS_DIR/bin/avdmanager"

download_and_verify() {
    local url="$1"
    local expected_sha256="$2"
    local output="$3"
    local label="$4"

    log "Downloading $label"
    curl --fail --location --retry 3 --retry-delay 2 --output "$output" "$url"
    printf '%s  %s\n' "$expected_sha256" "$output" | sha256sum --check --status ||
        die "$label failed SHA-256 verification."
}

if [[ ! -x "$SDKMANAGER" ]]; then
    tools_archive="$TEMP_DIR/command-line-tools.zip"
    tools_extract="$TEMP_DIR/command-line-tools"
    download_and_verify "$CMDLINE_TOOLS_URL" "$CMDLINE_TOOLS_SHA256" "$tools_archive" "Android command-line tools"
    mkdir -p "$tools_extract"
    unzip -q "$tools_archive" -d "$tools_extract"
    [[ -x "$tools_extract/cmdline-tools/bin/sdkmanager" ]] || die "Unexpected command-line-tools archive layout."
    mkdir -p "$ANDROID_SDK_DIR/cmdline-tools"
    [[ ! -e "$CMDLINE_TOOLS_DIR" ]] || die "$CMDLINE_TOOLS_DIR exists but is incomplete; move it aside and rerun."
    mv "$tools_extract/cmdline-tools" "$CMDLINE_TOOLS_DIR"
fi

mkdir -p "$HOME/.config/24seven-fm"
environment_file="$HOME/.config/24seven-fm/android-env.sh"
cat >"$environment_file" <<EOF
# Generated by 24Seven.FM Player's Ubuntu bootstrap.
export JAVA_HOME="$JAVA_HOME_DIR"
export ANDROID_HOME="$ANDROID_SDK_DIR"
export STUDIO_GRADLE_JDK="$JAVA_HOME_DIR"
export PATH="\$HOME/.local/bin:\$ANDROID_HOME/cmdline-tools/$CMDLINE_TOOLS_REVISION/bin:\$ANDROID_HOME/platform-tools:\$ANDROID_HOME/emulator:\$PATH"
EOF
chmod 600 "$environment_file"

source_line='[ -f "$HOME/.config/24seven-fm/android-env.sh" ] && . "$HOME/.config/24seven-fm/android-env.sh"'
for shell_file in "$HOME/.profile" "$HOME/.bashrc"; do
    touch "$shell_file"
    if ! grep -Fqx "$source_line" "$shell_file"; then
        printf '\n# 24Seven.FM Android/Codex development environment\n%s\n' "$source_line" >>"$shell_file"
    fi
done

# shellcheck disable=SC1090
source "$environment_file"

log "Installing Android SDK packages"
if $ACCEPT_LICENSES; then
    set +o pipefail
    yes | "$SDKMANAGER" --sdk_root="$ANDROID_HOME" --licenses >/dev/null
    license_status="${PIPESTATUS[1]}"
    set -o pipefail
    ((license_status == 0)) || die "Android SDK license acceptance failed."
elif [[ -t 0 ]]; then
    "$SDKMANAGER" --sdk_root="$ANDROID_HOME" --licenses
else
    die "A non-interactive terminal requires --accept-licenses."
fi

sdk_packages=(
    "platform-tools"
    "emulator"
    "platforms;android-35"
    "platforms;android-36"
    "build-tools;36.1.0"
    "system-images;android-35;google_apis;x86_64"
    "system-images;android-36;google_apis;x86_64"
)
"$SDKMANAGER" --sdk_root="$ANDROID_HOME" --channel=0 "${sdk_packages[@]}"

create_avd() {
    local name="$1"
    local package="$2"

    if "$AVDMANAGER" list avd | grep -Eq "^[[:space:]]*Name:[[:space:]]*$name$"; then
        log "AVD $name already exists"
        return
    fi

    log "Creating AVD $name"
    if ! printf 'no\n' | "$AVDMANAGER" create avd --name "$name" --package "$package" --device "pixel_7"; then
        warn "Pixel 7 hardware profile was unavailable; creating $name with the default profile."
        printf 'no\n' | "$AVDMANAGER" create avd --name "$name" --package "$package"
    fi
}

if $CREATE_AVDS; then
    create_avd "24Seven_API_35" "system-images;android-35;google_apis;x86_64"
    create_avd "24Seven_API_36" "system-images;android-36;google_apis;x86_64"
fi

if $INSTALL_ANDROID_STUDIO; then
    studio_install_dir="/opt/android-studio-$STUDIO_VERSION"
    if [[ ! -x "$studio_install_dir/bin/studio" ]]; then
        studio_archive="$TEMP_DIR/android-studio.tar.gz"
        studio_extract="$TEMP_DIR/android-studio"
        download_and_verify "$STUDIO_URL" "$STUDIO_SHA256" "$studio_archive" "Android Studio Quail 2"
        mkdir -p "$studio_extract"
        tar -xzf "$studio_archive" -C "$studio_extract"
        [[ -x "$studio_extract/android-studio/bin/studio" ]] || die "Unexpected Android Studio archive layout."
        [[ ! -e "$studio_install_dir" ]] || die "$studio_install_dir exists but is incomplete; move it aside and rerun."
        sudo mv "$studio_extract/android-studio" "$studio_install_dir"
    fi

    sudo ln -sfn "$studio_install_dir" /opt/android-studio
    sudo ln -sfn /opt/android-studio/bin/studio /usr/local/bin/android-studio
    sudo install -d /usr/local/share/applications
    sudo tee /usr/local/share/applications/android-studio.desktop >/dev/null <<'EOF'
[Desktop Entry]
Version=1.0
Type=Application
Name=Android Studio
Comment=Android development environment
Exec=/opt/android-studio/bin/studio %f
Icon=/opt/android-studio/bin/studio.svg
Terminal=false
Categories=Development;IDE;
StartupWMClass=jetbrains-android-studio
EOF
fi

if $INSTALL_CODEX; then
    codex_installer="$TEMP_DIR/install-codex.sh"
    log "Installing or updating ChatGPT Codex CLI"
    curl --fail --silent --show-error --location --retry 3 --output "$codex_installer" \
        "https://chatgpt.com/codex/install.sh"
    sh "$codex_installer"
fi

log "Verifying installed tools"
java -version
"$SDKMANAGER" --version
adb version
emulator -version | head -n 3
git --version
gh --version | head -n 1
node --version
npm --version
if $INSTALL_CODEX; then
    command -v codex >/dev/null || die "Codex installed, but its executable is not on PATH. Open a new shell and rerun codex --version."
    codex --version
fi
if $INSTALL_ANDROID_STUDIO; then
    [[ -x /opt/android-studio/bin/studio ]] || die "Android Studio verification failed."
fi

if [[ -x "$ANDROID_HOME/emulator/emulator" ]]; then
    if ! "$ANDROID_HOME/emulator/emulator" -accel-check; then
        warn "KVM acceleration is not currently usable. Confirm nested virtualization and log out/in for group changes."
    fi
fi

if $VERIFY_BUILD; then
    log "Running the focused Gradle compile verification"
    chmod +x "$REPO_ROOT/gradlew" 2>/dev/null || true
    (
        cd "$REPO_ROOT"
        bash ./gradlew :app:compileDebugKotlin --no-daemon
    )
fi

cat <<EOF

Ubuntu setup is complete.

Repository:       $REPO_ROOT
JAVA_HOME:        $JAVA_HOME
ANDROID_HOME:     $ANDROID_HOME
Android Studio:   /opt/android-studio/bin/studio
API 35 AVD:       24Seven_API_35
API 36 AVD:       24Seven_API_36

Open a new terminal (or run: source "$environment_file").
If group membership changed, log out and back in before using USB ADB or KVM.

Authenticate Codex with your ChatGPT subscription:
  codex login

For a headless Ubuntu host:
  codex login --device-auth

Then continue the project:
  cd "$REPO_ROOT"
  codex login status
  codex
EOF
