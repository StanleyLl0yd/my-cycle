#!/usr/bin/env bash
set -euo pipefail

APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"
OUT_DIR="${1:-store/rustore/screenshots}"
PACKAGE="com.silverlightning.mycycle.debug"
ACTIVITY="com.silverlightning.mycycle.ui.MainActivity"

mkdir -p "$OUT_DIR"

wait_for_boot() {
    adb wait-for-device
    for _ in $(seq 1 120); do
        if [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; then
            return 0
        fi
        sleep 2
    done
    return 1
}

dump_ui() {
    adb shell uiautomator dump /sdcard/window.xml >/dev/null
    adb pull /sdcard/window.xml /tmp/window.xml >/dev/null
}

tap_any() {
    dump_ui
    local point
    point="$(python3 - /tmp/window.xml "$@" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

path, *labels = sys.argv[1:]
root = ET.parse(path).getroot()
for label in labels:
    for node in root.iter("node"):
        text = node.attrib.get("text", "").strip()
        desc = node.attrib.get("content-desc", "").strip()
        if text == label or desc == label:
            match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
            if match:
                x1, y1, x2, y2 = map(int, match.groups())
                print(f"{(x1 + x2) // 2} {(y1 + y2) // 2}")
                raise SystemExit(0)
raise SystemExit(1)
PY
)" || {
        cat /tmp/window.xml
        return 1
    }
    adb shell input tap $point
    sleep 1
}

capture() {
    adb exec-out screencap -p > "$OUT_DIR/$1"
    test -s "$OUT_DIR/$1"
}

wait_for_boot
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb install -r "$APK_PATH"
adb shell cmd locale set-app-locales --user 0 "$PACKAGE" ru-RU >/dev/null 2>&1 || true
adb shell am force-stop "$PACKAGE"
adb shell am start -n "$PACKAGE/$ACTIVITY" >/dev/null
sleep 3

tap_any "Начать" "Get started"
tap_any "Больше 3 лет, промежутки похожи" "More than 3 years, usually similar"
tap_any "Продолжить" "Continue"
tap_any "Продолжить" "Continue"
tap_any "Готово" "Done"
sleep 2

capture "01-today.png"
tap_any "Календарь" "Calendar"
capture "02-calendar.png"
tap_any "Добавить прошлые месячные" "Add past period"
capture "03-add-past-period.png"
adb shell input keyevent 4
sleep 1
tap_any "Настройки" "Settings"
capture "04-settings.png"
