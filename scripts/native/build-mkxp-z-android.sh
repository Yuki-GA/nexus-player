#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ENGINE_ROOT="$REPO_ROOT/external/mkxp-z-android-master"
JNI_DIR="$ENGINE_ROOT/app/jni"

if [[ ! -d "$ENGINE_ROOT" ]]; then
  echo "mkxp-z Android workspace not found: $ENGINE_ROOT" >&2
  exit 1
fi

if [[ -z "${ANDROID_HOME:-}" ]]; then
  export ANDROID_HOME="$HOME/Android/Sdk"
fi

if [[ -z "${ANDROID_NDK_HOME:-}" ]]; then
  if [[ -d "$ANDROID_HOME/ndk/26.1.10909125" ]]; then
    export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/26.1.10909125"
  else
    export ANDROID_NDK_HOME="$(find "$ANDROID_HOME/ndk" -maxdepth 1 -mindepth 1 -type d | sort -V | tail -n 1)"
  fi
fi

cat > "$ENGINE_ROOT/local.properties" <<EOF
sdk.dir=$ANDROID_HOME
EOF

echo "ANDROID_HOME=$ANDROID_HOME"
echo "ANDROID_NDK_HOME=$ANDROID_NDK_HOME"

cd "$JNI_DIR"

if [[ ! -d SDL2 || ! -d ruby || ! -d openal ]]; then
  bash get_deps.sh
fi

for abi in armeabi-v7a arm64-v8a; do
  case "$abi" in
    armeabi-v7a)
      host="armv7a-linux-androideabi"
      target="arm-linux-androideabi"
      ;;
    arm64-v8a)
      host="aarch64-linux-android"
      target="aarch64-linux-android"
      ;;
  esac

  echo "Building native dependency prefix for $abi"
  make -j"$(nproc)" ABI="$abi" HOST="$host" TARGET="$target" ARCH="linux-x86_64" all
done

cd "$ENGINE_ROOT"
./gradlew :app:assembleDebug --console=plain

echo "mkxp-z Android output:"
find "$ENGINE_ROOT/app/build/outputs" -type f -maxdepth 5 -print
