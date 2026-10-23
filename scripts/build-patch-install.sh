#!/bin/bash
# Rebuild the pixkk bundle, patch TikTok 47.4.3 as com.test.tt.pixkk and install it on the phone.
# Usage: build-patch-install.sh <output apk name without extension>
# ORIGINAL=<path to the original Hushfeed .mpp> adds its patches ("Hide video overlays" etc.).
set -u
OUT="${1:-tiktok-pixkk}"
export JAVA_HOME="/c/Users/User/.jdks/openjdk-21.0.2"
export PATH="$JAVA_HOME/bin:$PATH"
export GITHUB_ACTOR=pixkk
export GITHUB_TOKEN="$(gh auth token)"
cd /d/Projects/pixkk-patches || exit 1

echo "== build"
./gradlew --no-daemon -q :patches:buildAndroid > /d/Projects/tools/build-pixkk.log 2>&1
MPP="$(ls -t patches/build/libs/*.mpp 2>/dev/null | head -1)"
[ -n "$MPP" ] || { echo "BUILD FAILED"; tail -20 /d/Projects/tools/build-pixkk.log; echo ALLDONE; exit 1; }

echo "== patch"
cd /d/Projects/tools || exit 1
rm -rf tmp-patch && mkdir -p tmp-patch
ORIG=()
[ -n "${ORIGINAL:-}" ] && ORIG=(-p "$ORIGINAL" -e "Hide video overlays" -e "Run beside the store app")
java -Xmx6g -jar morphe-desktop/morphe-desktop-1.18.1-all.jar patch --exclusive \
  -p "/d/Projects/pixkk-patches/$MPP" \
  -e "Expand description in place" -e "Sound name under description" -e "Scroll sound name" -e "Right column spacing" -e "Comment menu rows" -e "Centered comment menu" -e "Don't open comments on icon swipe" -e "Animated profile covers" -e "Old-style seeking" -e "Old-style counts" -e "Discover tab" -e "pixkk settings row" \
  "${ORIG[@]}" \
  -p morphe-patches/patches-1.47.0.mpp -e "Clone app" -O packageName=com.test.tt.pixkk \
  --keystore="$HOME/.android/debug.keystore" --keystore-entry-alias=androiddebugkey \
  --keystore-entry-password=android --keystore-password=android \
  -t /d/Projects/tools/tmp-patch -o "/d/Projects/tools/out/$OUT.apk" \
  "/d/Projects/hushfeed/com.zhiliaoapp.musically_47.4.3-2024704030_minAPI23(arm64-v8a,armeabi-v7a)(nodpi)_apkmirror.com.apk" \
  > /d/Projects/tools/patch-pixkk.log 2>&1
grep -i "applied\|Saved\|error\|exception" /d/Projects/tools/patch-pixkk.log | tail -12
[ -f "/d/Projects/tools/out/$OUT.apk" ] || { echo "PATCH FAILED"; echo ALLDONE; exit 1; }

echo "== install"
adb -s 7dd8304f install -r "/d/Projects/tools/out/$OUT.apk" 2>&1 | tail -2
[ -n "${NOLAUNCH:-}" ] || adb -s 7dd8304f shell monkey -p com.test.tt.pixkk -c android.intent.category.LAUNCHER 1 2>&1 | tail -1
echo ALLDONE
