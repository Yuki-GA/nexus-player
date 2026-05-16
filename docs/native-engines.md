# Native Engine Integration

Nexus can already run browser-style exports through WebView. RPG Maker XP/VX/VX Ace and desktop Ren'Py are different: they need native runtimes bundled into the APK.

## Current Target

The first native target is RPG Maker XP/VX/VX Ace through mkxp-z.

The downloaded engine workspace is:

```text
external/mkxp-z-android-master
```

That upstream project is a full Android app built around SDLActivity and ndk-build. It is not a drop-in Android library yet. The correct integration order is:

1. Build mkxp-z Android standalone.
2. Extract the produced native libraries and Java SDL support layer.
3. Convert the mkxp-z launch path into a Nexus runtime module.
4. Route `RPG_MAKER_XP_VX_ACE` folders from Nexus into that runtime.

Do not wire the engine into the main app until step 1 succeeds. A broken native engine dependency will break every APK build.

## Why WSL/Linux Is Required

The mkxp-z Android port depends on SDL2, Ruby, OpenAL, PhysFS, Pixman, OpenSSL, and other native dependencies. Its own README marks Windows/MSYS2 Ruby extension compilation as unfinished. Build it under Linux or WSL2 first.

## Build From WSL2

Install Ubuntu through WSL2, then from the repository root:

```bash
cd /mnt/c/Users/Adji/Downloads/MyApplication
bash scripts/native/build-mkxp-z-android.sh
```

Expected output if the engine builds:

```text
external/mkxp-z-android-master/app/build/outputs/apk/debug/
external/mkxp-z-android-master/app/build/intermediates/cxx/
```

After that, copy the generated `.so` files into the Nexus app under ABI folders:

```text
app/src/main/jniLibs/arm64-v8a/
app/src/main/jniLibs/armeabi-v7a/
```

Then the native bridge in Nexus can be changed from lifecycle logging to loading mkxp-z directly.

## Current Local Status

On the Windows host, the mkxp-z source dependencies have been fetched successfully with:

```powershell
.\scripts\native\fetch-mkxp-z-deps.ps1
```

The direct NDK build currently stops because mkxp-z expects native dependency outputs such as:

```text
external/mkxp-z-android-master/app/jni/openal/build-armeabi-v7a/lib/libopenal.so
```

Those outputs are produced by the mkxp-z `app/jni/Makefile`, which needs GNU make, Perl, Autoreconf, and Unix shell behavior. This Windows shell has the Android SDK/NDK/CMake/Ninja, but not the full Unix toolchain or a configured WSL distro.

## Ren'Py Desktop Runtime

Desktop Ren'Py folders need a Python/Ren'Py Android runtime. Ren'Py Web exports are already supported by Nexus through WebView. For desktop Ren'Py, build the Android runtime separately before attempting integration.

Preferred order:

1. Keep using Ren'Py Web exports in Nexus.
2. Finish mkxp-z Android integration for RPG Maker XP/VX/VX Ace.
3. Add desktop Ren'Py only after the native engine pattern is proven.
