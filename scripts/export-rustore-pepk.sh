#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 4 || $# -gt 5 ]]; then
    echo "Usage: $0 <pepk.jar> <keystore> <alias> <encryption-key> [output-dir]" >&2
    exit 2
fi

PEPK_JAR="$1"
KEYSTORE="$2"
ALIAS="$3"
ENCRYPTION_KEY="$4"
OUT_DIR="${5:-rustore-signing}"

for path in "$PEPK_JAR" "$KEYSTORE"; do
    if [[ ! -f "$path" ]]; then
        echo "File not found: $path" >&2
        exit 1
    fi
done

mkdir -p "$OUT_DIR"
PEPK_KEYSTORE="$KEYSTORE"
TEMP_KEYSTORE=""

cleanup() {
    if [[ -n "$TEMP_KEYSTORE" ]]; then
        rm -f "$TEMP_KEYSTORE"
    fi
}
trap cleanup EXIT

if [[ "${KEYSTORE,,}" == *.jks ]]; then
    TEMP_KEYSTORE="$(mktemp "${TMPDIR:-/tmp}/my-cycle-rustore-XXXXXX.keystore")"
    rm -f "$TEMP_KEYSTORE"
    keytool -importkeystore \
        -srckeystore "$KEYSTORE" \
        -destkeystore "$TEMP_KEYSTORE"
    PEPK_KEYSTORE="$TEMP_KEYSTORE"
fi

java -jar "$PEPK_JAR" \
    --keystore="$PEPK_KEYSTORE" \
    --alias="$ALIAS" \
    --output="$OUT_DIR/pepk_out.zip" \
    --encryptionkey="$ENCRYPTION_KEY" \
    --include-cert

keytool -exportcert \
    -alias "$ALIAS" \
    -keystore "$KEYSTORE" \
    -rfc \
    -file "$OUT_DIR/uploadcert.pem"

echo "Created:"
echo "  $OUT_DIR/pepk_out.zip"
echo "  $OUT_DIR/uploadcert.pem"
