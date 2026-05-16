param(
    [string]$JniDir = "external/mkxp-z-android-master/app/jni"
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$targetRoot = Resolve-Path (Join-Path $repoRoot $JniDir)
$downloadRoot = Join-Path $targetRoot "_downloads"
New-Item -ItemType Directory -Force -Path $downloadRoot | Out-Null

function Download-File {
    param(
        [string]$Url,
        [string]$OutFile
    )

    if (Test-Path $OutFile) {
        return
    }

    Write-Host "Downloading $Url"
    & curl.exe -L --fail --retry 3 --retry-delay 2 -o $OutFile $Url
    if ($LASTEXITCODE -ne 0) {
        throw "Download failed: $Url"
    }
}

function Expand-ZipDependency {
    param(
        [string]$Name,
        [string[]]$Urls
    )

    $destination = Join-Path $targetRoot $Name
    if (Test-Path $destination) {
        Write-Host "$Name already exists"
        return
    }

    $archive = Join-Path $downloadRoot "$Name.zip"
    $downloaded = $false
    foreach ($url in $Urls) {
        try {
            Download-File -Url $url -OutFile $archive
            $downloaded = $true
            break
        } catch {
            Remove-Item -LiteralPath $archive -Force -ErrorAction SilentlyContinue
            Write-Host "Failed source: $url"
        }
    }

    if (-not $downloaded) {
        throw "No download source worked for $Name"
    }

    $extractDir = Join-Path $downloadRoot "$Name-extract"
    Remove-Item -LiteralPath $extractDir -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $extractDir | Out-Null
    Expand-Archive -Path $archive -DestinationPath $extractDir -Force
    $inner = Get-ChildItem -LiteralPath $extractDir -Directory | Select-Object -First 1
    if (-not $inner) {
        throw "Archive did not contain a directory: $archive"
    }

    Move-Item -LiteralPath $inner.FullName -Destination $destination
    Remove-Item -LiteralPath $extractDir -Recurse -Force
}

function Expand-TarDependency {
    param(
        [string]$Name,
        [string]$Url
    )

    $destination = Join-Path $targetRoot $Name
    if (Test-Path $destination) {
        Write-Host "$Name already exists"
        return
    }

    $archive = Join-Path $downloadRoot "$Name.tar"
    Download-File -Url $Url -OutFile $archive

    $extractDir = Join-Path $downloadRoot "$Name-extract"
    Remove-Item -LiteralPath $extractDir -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $extractDir | Out-Null
    & tar.exe -xf $archive -C $extractDir
    if ($LASTEXITCODE -ne 0) {
        throw "Extraction failed: $archive"
    }

    $inner = Get-ChildItem -LiteralPath $extractDir -Directory | Select-Object -First 1
    if (-not $inner) {
        throw "Archive did not contain a directory: $archive"
    }

    Move-Item -LiteralPath $inner.FullName -Destination $destination
    Remove-Item -LiteralPath $extractDir -Recurse -Force
}

Expand-ZipDependency -Name "libogg" -Urls @(
    "https://github.com/xiph/ogg/archive/refs/tags/v1.3.5.zip"
)

Expand-ZipDependency -Name "libvorbis" -Urls @(
    "https://github.com/xiph/vorbis/archive/refs/tags/v1.3.7.zip"
)

Expand-TarDependency -Name "libtheora" -Url "https://ftp.osuosl.org/pub/xiph/releases/theora/libtheora-1.1.1.tar.gz"

Expand-TarDependency -Name "libiconv" -Url "https://ftp.gnu.org/pub/gnu/libiconv/libiconv-1.17.tar.gz"

Expand-TarDependency -Name "uchardet" -Url "https://gitlab.freedesktop.org/uchardet/uchardet/-/archive/v0.0.8/uchardet-v0.0.8.tar.gz"

Expand-TarDependency -Name "pixman" -Url "https://www.cairographics.org/releases/pixman-0.42.2.tar.gz"

Expand-ZipDependency -Name "physfs" -Urls @(
    "https://github.com/icculus/physfs/archive/refs/tags/release-3.2.0.zip"
)

Expand-ZipDependency -Name "openal" -Urls @(
    "https://github.com/kcat/openal-soft/archive/refs/tags/1.23.0.zip"
)

Expand-ZipDependency -Name "SDL2" -Urls @(
    "https://github.com/libsdl-org/SDL/archive/refs/tags/release-2.26.3.zip"
)

Expand-ZipDependency -Name "SDL2_image" -Urls @(
    "https://github.com/libsdl-org/SDL_image/archive/refs/tags/release-2.6.3.zip"
)

Expand-ZipDependency -Name "SDL2_ttf" -Urls @(
    "https://github.com/libsdl-org/SDL_ttf/archive/refs/tags/release-2.20.2.zip"
)

Expand-ZipDependency -Name "SDL2_sound" -Urls @(
    "https://github.com/icculus/SDL_sound/archive/refs/tags/v2.0.1.zip"
)

Expand-ZipDependency -Name "openssl" -Urls @(
    "https://github.com/openssl/openssl/archive/refs/tags/OpenSSL_1_1_1t.zip"
)

Expand-ZipDependency -Name "ruby" -Urls @(
    "https://github.com/mkxp-z/ruby/archive/refs/heads/mkxp-z-3.1.zip"
)

Write-Host "mkxp-z dependency sources are present under $targetRoot"
