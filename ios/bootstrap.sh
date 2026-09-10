#!/usr/bin/env bash
# Puts the assets both platforms share where the Xcode targets expect them.
#
# Archivo and the four Mochi portraits live once in the repository, under the
# Android resources, and both platforms use those exact bytes — copying them
# in beats carrying a second byte-identical copy that can drift.
set -euo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
mkdir -p "$here/Resources"
cp "$here/../app/src/main/res/font/archivo.ttf" "$here/Resources/Archivo.ttf"
for mood in awake pleased resting let_down; do
  cp "$here/../app/src/main/res/drawable-nodpi/mochi_$mood.png" "$here/Resources/mochi_$mood.png"
done
echo "Archivo.ttf and the four Mochi portraits staged in ios/Resources"
