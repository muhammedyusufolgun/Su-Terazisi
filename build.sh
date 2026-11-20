#!/usr/bin/env bash
# Android SDK 35, JDK 17+, zip
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_HOME:?Android SDK yolunu belirtin}"
bt="$ANDROID_HOME/build-tools/35.0.0"
platform="$ANDROID_HOME/platforms/android-35/android.jar"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
mkdir -p "$work/classes" "$work/dex"
"$bt/aapt" package -f -M AndroidManifest.xml -S res -I "$platform" -F "$work/unsigned.apk"
javac --release 8 -encoding UTF-8 -classpath "$platform" -d "$work/classes" src/tr/olgun/suterazisi/*.java
jar cf "$work/app.jar" -C "$work/classes" .
"$bt/d8" --min-api 23 --lib "$platform" --output "$work/dex" "$work/app.jar"
(cd "$work/dex" && zip -q "$work/unsigned.apk" classes.dex)
"$bt/zipalign" -f 4 "$work/unsigned.apk" Su-Terazisi-unsigned.apk
