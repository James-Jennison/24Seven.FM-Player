[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$defaultSdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$sdkRoot = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { $defaultSdk }
$requiredPaths = @(
    (Join-Path $sdkRoot 'platforms\android-36'),
    (Join-Path $sdkRoot 'build-tools\36.1.0'),
    (Join-Path $sdkRoot 'platform-tools\adb.exe')
)

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'JDK 17 is required, but java was not found on PATH.'
}

$ErrorActionPreference = 'Continue'
$javaOutput = & java -version 2>&1
$javaExitCode = $LASTEXITCODE
$ErrorActionPreference = 'Stop'
if ($javaExitCode -ne 0) {
    throw "Unable to query Java. Exit code: $javaExitCode"
}

$javaVersion = ($javaOutput | Select-Object -First 1).ToString()
if ($javaVersion -notmatch 'version "17[\.]') {
    throw "JDK 17 is required. Found: $javaVersion"
}

$missing = $requiredPaths | Where-Object { -not (Test-Path -LiteralPath $_) }
if ($missing) {
    $formatted = $missing -join [Environment]::NewLine
    throw "Android SDK prerequisites are missing:$([Environment]::NewLine)$formatted$([Environment]::NewLine)Install Platform 36, Build Tools 36.1.0, and Platform Tools."
}

$env:ANDROID_HOME = $sdkRoot
$env:TWENTYFOURSEVEN_ANDROID_BUILD_DIR = Join-Path $env:TEMP '24seven-android-build'

Write-Host "JDK: $javaVersion"
Write-Host "Android SDK: $sdkRoot"
Write-Host "Build output: $env:TWENTYFOURSEVEN_ANDROID_BUILD_DIR"

Push-Location $repositoryRoot
try {
    & .\gradlew.bat test lint assembleDebug --console=plain --no-problems-report
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle validation failed with exit code $LASTEXITCODE."
    }

    & (Join-Path $sdkRoot 'platform-tools\adb.exe') devices -l
}
finally {
    Pop-Location
}
