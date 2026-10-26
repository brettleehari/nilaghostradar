#!/bin/sh
# Fetches the MediaPipe Tasks Vision 1.0.0 iOS frameworks (Apache-2.0) into Vendor/. These are the
# exact archives the MediaPipeTasksVision and MediaPipeTasksCommon pods point at; CocoaPods is not
# needed. ~370 MB unpacked, so Vendor/ is gitignored.
set -eu
cd "$(dirname "$0")"
mkdir -p Vendor
fetch() {
  name=$1; url=$2
  [ -d "Vendor/$name/frameworks" ] && { echo "$name already present"; return; }
  echo "fetching $name"
  curl -sS -L -o "Vendor/$name.tar.gz" "$url"
  mkdir -p "Vendor/$name" && tar -xzf "Vendor/$name.tar.gz" -C "Vendor/$name" && rm "Vendor/$name.tar.gz"
}
fetch MediaPipeTasksVision "https://dl.google.com/cpdc/20260727-225049/MediaPipeTasksVision-1.0.0.tar.gz"
fetch MediaPipeTasksCommon "https://dl.google.com/cpdc/20260727-225047/MediaPipeTasksCommon-1.0.0.tar.gz"
echo "done"
