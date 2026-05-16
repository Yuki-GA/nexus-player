$ErrorActionPreference = "Stop"

$checks = @(
    @{ Name = "Android SDK"; Command = { Test-Path "C:\Users\Adji\AppData\Local\Android\Sdk" } },
    @{ Name = "Android NDK"; Command = { Test-Path "C:\Users\Adji\AppData\Local\Android\Sdk\ndk\26.1.10909125\ndk-build.cmd" } },
    @{ Name = "Android CMake"; Command = { Test-Path "C:\Users\Adji\AppData\Local\Android\Sdk\cmake\3.22.1\bin\cmake.exe" } },
    @{ Name = "Android Ninja"; Command = { Test-Path "C:\Users\Adji\AppData\Local\Android\Sdk\cmake\3.22.1\bin\ninja.exe" } },
    @{ Name = "Android Studio Java"; Command = { Test-Path "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" } },
    @{ Name = "mkxp-z workspace"; Command = { Test-Path "external\mkxp-z-android-master" } },
    @{ Name = "GNU make"; Command = { [bool](Get-Command make -ErrorAction SilentlyContinue) } },
    @{ Name = "Perl"; Command = { [bool](Get-Command perl -ErrorAction SilentlyContinue) } },
    @{ Name = "Autoreconf"; Command = { [bool](Get-Command autoreconf -ErrorAction SilentlyContinue) } },
    @{ Name = "Configured WSL distro"; Command = {
        $output = & wsl.exe -l -q 2>$null
        $LASTEXITCODE -eq 0 -and ($output | Where-Object { $_.Trim().Length -gt 0 }).Count -gt 0
    } }
)

foreach ($check in $checks) {
    $ok = & $check.Command
    $status = if ($ok) { "OK" } else { "Missing" }
    Write-Host "$($check.Name): $status"
}

Write-Host ""
Write-Host "mkxp-z Android needs the Android toolchain plus Unix build tooling for iconv/OpenAL/OpenSSL/Ruby."
Write-Host "If GNU make, Perl, Autoreconf, or a WSL distro are missing, use WSL2/Ubuntu for the native dependency build."
