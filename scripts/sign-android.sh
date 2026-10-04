#!/bin/sh
set -eu
if [ "$#" -lt 2 ]; then
  echo 'Usage: sign-android.sh <private-keystore> <password-file> [output-apk]' >&2
  exit 1
fi
SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK_DIR" ]; then echo 'Set ANDROID_HOME to the Android SDK directory.' >&2; exit 1; fi
TASK_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
APK_OUTPUT="${3:-$TASK_ROOT/artifacts/Oak-Ember.apk}"
mkdir -p "$(dirname -- "$APK_OUTPUT")"
APK_ALIGNED=$(mktemp)
trap 'rm -f "$APK_ALIGNED"' EXIT
"$SDK_DIR/build-tools/36.0.0/zipalign" -f -p 4 "$TASK_ROOT/android/app/build/outputs/apk/release/app-release-unsigned.apk" "$APK_ALIGNED"
"$SDK_DIR/build-tools/36.0.0/apksigner" sign --ks "$1" --ks-key-alias oak-ember --ks-pass "file:$2" --out "$APK_OUTPUT" "$APK_ALIGNED"
"$SDK_DIR/build-tools/36.0.0/apksigner" verify "$APK_OUTPUT"
