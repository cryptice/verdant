#!/usr/bin/env bash
#
# Devcontainer post-create hook.
#
# Sanity-check the toolchain (gradle wrapper bootstraps, Node, Android SDK).
#
# Android SDK resolution: the workspace bind-mount is shared with the host,
# so we deliberately do NOT write android/local.properties from the
# container — that would clobber the host's copy on every container start
# and break Android Studio / host gradle builds. Instead:
#
#   * Container builds rely on ANDROID_HOME=/opt/android-sdk (set by
#     .devcontainer/docker-compose.yml).
#   * android/settings.gradle.kts self-heals: if local.properties exists
#     with an sdk.dir that doesn't resolve in this environment, it deletes
#     the file so AGP falls back to ANDROID_HOME.
#   * On the host, Android Studio regenerates local.properties on next
#     project sync; for CLI-only host builds, set
#     ANDROID_HOME=$HOME/Library/Android/sdk in your shell profile.
#
set -euo pipefail

echo "== gradle (backend) =="
backend/gradlew --version

echo
echo "== node =="
node --version

echo
echo "== android sdk =="
if [[ -z "${ANDROID_HOME:-}" || ! -d "$ANDROID_HOME" ]]; then
  echo "ANDROID_HOME not set or missing — Android builds will fail." >&2
  exit 1
fi
sdkmanager --list_installed | sed -n '1,12p'
