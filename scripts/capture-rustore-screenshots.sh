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

assert_no_fullscreen_tutorial() {
    local hierarchy
    adb shell uiautomator dump /sdcard/rustore-window.xml >/dev/null 2>&1 || return 0
    hierarchy="$(adb exec-out cat /sdcard/rustore-window.xml 2>/dev/null || true)"
    if grep -Fq -e 'Viewing full screen' -e 'Got it' <<< "$hierarchy"; then
        echo 'Android immersive-mode tutorial is covering the app.' >&2
        exit 1
    fi
}

launch_screen() {
    local screen="$1"
    adb shell am force-stop com.sl.mycycle.debug
    adb shell am start -W \
        -n com.sl.mycycle.debug/com.sl.mycycle.debug.StoreScreenshotActivity \
        --es store_screen "$screen"
    sleep 3
    assert_app_foreground
    assert_no_fullscreen_tutorial
}

capture_screen() {
    local screen="$1"
    local output="$2"
    launch_screen "$screen"
    adb exec-out screencap -p > "$output"
}

capture_scrolled_screen() {
    local screen="$1"
    local output="$2"
    local swipes="$3"
    launch_screen "$screen"
    for _ in $(seq 1 "$swipes"); do
        adb shell input swipe 540 1540 540 620 350
        sleep 1
    done
    assert_app_foreground
    assert_no_fullscreen_tutorial
    adb exec-out screencap -p > "$output"
}

adb wait-for-device
test "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1"
adb shell settings put global hide_error_dialogs 1
adb shell settings put secure immersive_mode_confirmations confirmed
adb shell settings put global policy_control 'immersive.full=*'

adb install -r -t app/build/outputs/apk/debug/app-debug.apk
adb shell cmd locale set-app-locales com.sl.mycycle.debug --user 0 --locales ru-RU
adb shell cmd locale get-app-locales com.sl.mycycle.debug --user 0 | grep -Fq 'ru-RU'

mkdir -p store/rustore/screenshots/phone
rm -f store/rustore/screenshots/phone/*.png

capture_screen today store/rustore/screenshots/phone/01-today.png
capture_screen calendar store/rustore/screenshots/phone/02-calendar.png
capture_screen statistics store/rustore/screenshots/phone/03-statistics.png
capture_scrolled_screen statistics store/rustore/screenshots/phone/04-insights.png 2
capture_scrolled_screen settings store/rustore/screenshots/phone/05-privacy-report.png 3

python3 scripts/validate-rustore-screenshots.py
