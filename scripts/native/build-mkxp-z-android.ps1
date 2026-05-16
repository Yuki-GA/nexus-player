param(
    [string]$Workspace = "external/mkxp-z-android-master",
    [string]$SdkDir = "C:\Users\Adji\AppData\Local\Android\Sdk",
    [switch]$FetchSources,
    [switch]$TryWindowsNdkBuild
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$engineRoot = Join-Path $root $Workspace

if (-not (Test-Path $engineRoot)) {
    throw "mkxp-z Android workspace was not found at $engineRoot"
}

$localProperties = Join-Path $engineRoot "local.properties"
"sdk.dir=$($SdkDir -replace '\\', '\\')" | Set-Content -LiteralPath $localProperties -Encoding ASCII

Write-Host "Workspace: $engineRoot"
Write-Host "Android SDK: $SdkDir"
Write-Host ""
Write-Host "This native engine should be built from WSL/Linux, because the upstream port depends on native Unix build tooling."
Write-Host "From WSL2 Ubuntu, run:"
Write-Host ""
Write-Host "  cd /mnt/c/Users/Adji/Downloads/MyApplication/external/mkxp-z-android-master/app/jni"
Write-Host "  bash get_deps.sh"
Write-Host "  cd ../.."
Write-Host "  ./gradlew :app:assembleDebug --console=plain"
Write-Host ""
Write-Host "After a successful build, copy the produced native .so files into app/src/main/jniLibs/ in the main Nexus project."

if ($FetchSources) {
    & (Join-Path $root "scripts\native\fetch-mkxp-z-deps.ps1")
}

if ($TryWindowsNdkBuild) {
    $ndkBuild = Join-Path $SdkDir "ndk\26.1.10909125\ndk-build.cmd"
    if (-not (Test-Path $ndkBuild)) {
        throw "ndk-build was not found at $ndkBuild"
    }

    Push-Location (Join-Path $engineRoot "app\jni")
    try {
        & $ndkBuild NDK_PROJECT_PATH=. APP_BUILD_SCRIPT=Android.mk NDK_APPLICATION_MK=Application.mk
    } finally {
        Pop-Location
    }
}
