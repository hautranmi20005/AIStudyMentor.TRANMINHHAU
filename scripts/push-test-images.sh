#!/usr/bin/env sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repo_root=$(CDPATH= cd -- "$script_dir/.." && pwd)
asset_dir="$repo_root/.p09-test-images"
device=${1:-}
python_bin=${PYTHON:-python3}
adb_bin=${ADB:-adb}

command -v "$python_bin" >/dev/null 2>&1 || {
    echo "Python 3 is required to generate the P09 photo test images." >&2
    exit 1
}
if ! command -v "$adb_bin" >/dev/null 2>&1; then
    for candidate in "${ANDROID_SDK_ROOT:-}/platform-tools/adb" "${ANDROID_HOME:-}/platform-tools/adb"; do
        if [ -x "$candidate" ]; then
            adb_bin=$candidate
            break
        fi
    done
fi
command -v "$adb_bin" >/dev/null 2>&1 || {
    echo "adb was not found; add Android platform-tools to PATH or set ANDROID_SDK_ROOT." >&2
    exit 1
}

"$python_bin" "$script_dir/generate-p09-test-images.py" "$asset_dir"
remote_directory=/sdcard/Pictures/P09
if [ -n "$device" ]; then
    "$adb_bin" -s "$device" shell mkdir -p "$remote_directory"
else
    "$adb_bin" shell mkdir -p "$remote_directory"
fi

for image in "$asset_dir"/*; do
    [ -f "$image" ] || continue
    name=$(basename "$image")
    remote_path="$remote_directory/$name"
    if [ -n "$device" ]; then
        "$adb_bin" -s "$device" push "$image" "$remote_path"
        "$adb_bin" -s "$device" shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d "file://$remote_path"
    else
        "$adb_bin" push "$image" "$remote_path"
        "$adb_bin" shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d "file://$remote_path"
    fi
done

echo "P09 images are available in the Android Photo Picker under Pictures/P09."
