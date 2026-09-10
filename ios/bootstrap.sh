#!/usr/bin/env bash
# Puts the one Archivo we ship where the Xcode targets expect it.
#
# The font lives once in the repository, under the Android resources, and both
# platforms pin the same variable TTF — copying it in beats carrying a second
# byte-identical copy that can drift.
set -euo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
mkdir -p "$here/Resources"
cp "$here/../app/src/main/res/font/archivo.ttf" "$here/Resources/Archivo.ttf"
echo "Archivo.ttf staged in ios/Resources"
