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
adb shell cmd locale set-app-locales "$PACKAGE" --user 0 --locales ru-RU
adb shell settings put global policy_control immersive.full=*
adb shell am force-stop "$PACKAGE"
adb shell am start -n "$PACKAGE/$ACTIVITY" >/dev/null
sleep 3

dump_ui
if ! grep -q 'Начать' /tmp/window.xml; then
    cat /tmp/window.xml
    echo 'Russian app locale was not applied.' >&2
    exit 1
fi

tap_any "Начать"
tap_any "Больше 3 лет, промежутки похожи"
tap_any "Продолжить"
tap_any "Продолжить"
tap_any "Готово"
sleep 2

tap_any "Календарь"
tap_any "Добавить прошлые месячные"
tap_any "Сохранить"
tap_any "Добавить ещё"
tap_any "Сохранить"
tap_any "Добавить ещё"
tap_any "Сохранить"
tap_any "Готово"
sleep 1

capture "02-calendar.png"
tap_any "Сегодня"
capture "01-today.png"
tap_any "Календарь"
tap_any "Добавить прошлые месячные"
capture "03-add-past-period.png"
adb shell input keyevent 4
sleep 1
tap_any "Статистика"
capture "04-statistics.png"
tap_any "Настройки"
capture "05-settings.png"
