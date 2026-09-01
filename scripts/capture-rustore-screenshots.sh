#!/usr/bin/env bash
set -euo pipefail

assert_app_foreground() {
    local focus
    for _ in $(seq 1 20); do
        focus="$(adb shell dumpsys window | grep -E 'mCurrentFocus|mFocusedApp' || true)"
        if grep -Fq 'com.sl.mycycle.debug' <<< "$focus"; then
            return 0
        fi
        sleep 0.5
    done
    echo "My Cycle is not the focused app: $focus" >&2
    exit 1
}

capture_screen() {
    local screen="$1"
    local output="$2"
    adb shell am force-stop com.sl.mycycle.debug
    adb shell am start -W \
        -n com.sl.mycycle.debug/com.sl.mycycle.debug.StoreScreenshotActivity \
        --es store_screen "$screen"
    sleep 3
    assert_app_foreground
    adb exec-out screencap -p > "$output"
}

adb wait-for-device
test "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1"
adb shell settings put global hide_error_dialogs 1

adb install -r -t app/build/outputs/apk/debug/app-debug.apk
adb shell cmd locale set-app-locales com.sl.mycycle.debug --user 0 --locales ru-RU
adb shell cmd locale get-app-locales com.sl.mycycle.debug --user 0 | grep -Fq 'ru-RU'

mkdir -p store/rustore/screenshots/phone
capture_screen today store/rustore/screenshots/phone/01-today.png
capture_screen calendar store/rustore/screenshots/phone/02-calendar.png
capture_screen statistics store/rustore/screenshots/phone/03-statistics.png
capture_screen settings store/rustore/screenshots/phone/04-settings.png

python3 scripts/validate-rustore-screenshots.py
