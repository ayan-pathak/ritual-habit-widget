#!/usr/bin/env bash
# Puts the one Archivo we ship where the Xcode targets expect it.
#
# The font lives once in the repository, under the Android resources, and both
# platforms pin the same variable TTF — copying it in beats carrying a second
# byte-identical copy that can drift.
#
# Mochi needs nothing here: his paths are generated straight into
# Shared/MochiArt.swift by tools/mochi.py and compile in like any other source.
set -euo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
mkdir -p "$here/Resources"
cp "$here/../app/src/main/res/font/archivo.ttf" "$here/Resources/Archivo.ttf"

# Google hands the sign-in back on a URL scheme named after the project's
# reversed client id, which has to be in Info.plist and so cannot be read at
# runtime. It is written into a build setting here instead of into project.yml,
# which is checked in and must stay free of anyone's project configuration.
mkdir -p "$here/Config"
plist="$here/Resources/GoogleService-Info.plist"
reversed="com.googleusercontent.apps.unconfigured"
if [ -f "$plist" ]; then
  found=$(/usr/libexec/PlistBuddy -c "Print :REVERSED_CLIENT_ID" "$plist" 2>/dev/null || true)
  [ -n "$found" ] && reversed="$found"
fi
printf 'GOOGLE_REVERSED_CLIENT_ID = %s\n' "$reversed" > "$here/Config/Google.xcconfig"

echo "Archivo.ttf staged, Google URL scheme set to $reversed"
