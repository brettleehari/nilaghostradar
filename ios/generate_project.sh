#!/bin/sh
# Generates GhostRadar.xcodeproj from project.yml. Needs xcodegen (brew install xcodegen).
set -eu
cd "$(dirname "$0")"
command -v xcodegen >/dev/null || { echo "xcodegen not found: brew install xcodegen" >&2; exit 1; }
[ -d Vendor/MediaPipeTasksCommon/frameworks ] || ./fetch_mediapipe.sh
xcodegen generate
