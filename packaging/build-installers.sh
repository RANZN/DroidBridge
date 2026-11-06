#!/usr/bin/env bash
# Builds DroidBridge-<version>-macos-<arch>.pkg (installer) and .dmg in build/dist.
#
# Usage:   packaging/build-installers.sh <version>        e.g. packaging/build-installers.sh v1.2.0
# Needs:   a JDK 17+ with jpackage (set JAVA_HOME, or have jpackage on PATH), python3, macOS.
#
# DroidBridge.app includes its own Java runtime, so users do not need Java installed.
# It is built for the architecture of the machine running the script (arm64 or x64).
set -euo pipefail
cd "$(dirname "$0")/.."

[[ $# -eq 1 ]] || { echo "Usage: $0 <version>   (e.g. v1.2.0)" >&2; exit 1; }
VERSION="${1#v}"
# jpackage on macOS needs 1-3 numbers, the first at least 1: "1.2.0-beta.1" -> "1.2.0".
APP_VERSION="$(sed -E 's/^([0-9]+(\.[0-9]+){0,2}).*/\1/' <<<"$VERSION")"
if [[ ! "$APP_VERSION" =~ ^[1-9][0-9]*(\.[0-9]+){0,2}$ ]]; then
    echo "Version must look like 1.2.3 and start at 1 or higher (macOS requirement), got: $1" >&2
    exit 1
fi

case "$(uname -m)" in
    arm64) ARCH=arm64 ;;
    x86_64) ARCH=x64 ;;
    *) echo "Unsupported architecture: $(uname -m)" >&2; exit 1 ;;
esac

BIN="${JAVA_HOME:+$JAVA_HOME/bin/}"
command -v "${BIN}jpackage" >/dev/null || { echo "jpackage not found: set JAVA_HOME to a JDK 17+" >&2; exit 1; }

OUT=build/dist
INPUT="$OUT/input"
rm -rf "$OUT"
mkdir -p "$INPUT"

echo "==> Building DroidBridge $VERSION ($ARCH)"
./kotlin build   # produces the desktopApp and shared module JARs

echo "==> Collecting runtime classpath"
python3 packaging/runtime_classpath.py > "$OUT/classpath.txt"
# Copy with unique names: different artifacts can share a file name (e.g. runtime-desktop-1.12.1.jar).
while IFS= read -r jar; do
    case "$jar" in
        */.m2.cache/*)
            rel="${jar#*/.m2.cache/}"                 # org/jetbrains/compose/runtime/runtime-desktop/1.12.1/x.jar
            group="$(dirname "$(dirname "$(dirname "$rel")")" | tr / .)"
            cp "$jar" "$INPUT/${group}_$(basename "$jar")"
            ;;
        *) cp "$jar" "$INPUT/" ;;
    esac
done < "$OUT/classpath.txt"
echo "    $(ls "$INPUT" | wc -l | tr -d ' ') JARs"

echo "==> Working out Java modules"
CLASSPATH_ALL="$(paste -sd: "$OUT/classpath.txt")"
DETECTED="$("${BIN}jdeps" --ignore-missing-deps --print-module-deps --multi-release base \
    --recursive --class-path "$CLASSPATH_ALL" "$INPUT/desktopApp-jvm.jar")"
# Loaded by reflection, so jdeps cannot see them: logback XML config, JNDI/JMX lookups, charsets.
MODULES="$DETECTED,java.xml,java.naming,java.management,java.sql,jdk.charsets"
echo "    $MODULES"

echo "==> Building DroidBridge.app"
"${BIN}jpackage" \
    --type app-image \
    --dest "$OUT/image" \
    --name DroidBridge \
    --app-version "$APP_VERSION" \
    --vendor "Ranjan" \
    --description "Drag and drop files between your Mac and Android devices" \
    --copyright "Copyright $(date +%Y) Ranjan. Apache License 2.0." \
    --input "$INPUT" \
    --main-jar desktopApp-jvm.jar \
    --main-class com.ranjan.droidbridge.MainKt \
    --add-modules "$MODULES" \
    --java-options "-Ddroidbridge.version=$VERSION" \
    --java-options "--enable-native-access=ALL-UNNAMED" \
    --icon desktopApp/icons/DroidBridge.icns \
    --mac-package-identifier com.ranjan.droidbridge \
    --mac-package-name DroidBridge

# Two wrappers around the same app:
#   .pkg - the "setup file": double-click, click Install, the app goes into Applications.
#   .dmg - open it and drag DroidBridge into Applications.
for TYPE in pkg dmg; do
    echo "==> Packaging .$TYPE"
    "${BIN}jpackage" \
        --type "$TYPE" \
        --dest "$OUT" \
        --name DroidBridge \
        --app-version "$APP_VERSION" \
        --vendor "Ranjan" \
        --app-image "$OUT/image/DroidBridge.app" \
        --mac-package-identifier com.ranjan.droidbridge
    FILE="$OUT/DroidBridge-$VERSION-macos-$ARCH.$TYPE"
    mv "$OUT/DroidBridge-$APP_VERSION.$TYPE" "$FILE"
    (cd "$OUT" && shasum -a 256 "$(basename "$FILE")" > "$(basename "$FILE").sha256")
    echo "    $FILE"
done
echo "==> Done"
