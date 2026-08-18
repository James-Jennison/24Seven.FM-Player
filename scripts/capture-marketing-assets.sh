#!/usr/bin/env bash
set -euo pipefail

# Captures only into the gitignored captures/ directory. It never publishes or
# stages an asset: inspect each derived file before promoting it deliberately.
repository_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
label="${1:?usage: scripts/capture-marketing-assets.sh <label>}"
display_id="${ANDROID_DISPLAY_ID:?set ANDROID_DISPLAY_ID to the active physical display ID}"
capture_root="${MARKETING_CAPTURE_DIR:-${repository_root}/captures/marketing-${label}-$(date +%Y%m%d-%H%M%S)}"
raw_png="${capture_root}/${label}-raw.png"
derived_png="${capture_root}/${label}-cropped.png"

if [[ "${label}" == "chat" && "${MARKETING_CHAT_MODE:-}" != "synthetic" ]]; then
  printf '%s\n' "Refusing Chat capture: set MARKETING_CHAT_MODE=synthetic only after an isolated, fictional native scene is visible." >&2
  exit 2
fi

mkdir -p -- "${capture_root}"

demo_exit() {
  adb shell am broadcast -a com.android.systemui.demo -e command exit >/dev/null 2>&1 || true
}
trap demo_exit EXIT INT TERM

adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941 >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command network -e mobile show -e level 4 -e datatype none >/dev/null

printf '%s\n' "Navigate the device to the reviewed ${label} screen, then press Enter to capture."
read -r
adb exec-out screencap -d "${display_id}" -p > "${raw_png}"
ffmpeg -y -v error -i "${raw_png}" -vf 'crop=1080:2370:0:130,scale=648:-2' -compression_level 9 "${derived_png}"
printf 'Raw capture: %s\nDerived review file: %s\n' "${raw_png}" "${derived_png}"
