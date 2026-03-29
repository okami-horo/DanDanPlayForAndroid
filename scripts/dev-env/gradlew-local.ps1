$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = (Resolve-Path (Join-Path $scriptDir "..\\..")).Path

$env:JAVA_HOME = Join-Path $projectRoot ".dev-env\\jdk\\ms-17.0.15"
$env:ANDROID_HOME = Join-Path $projectRoot ".dev-env\\android-sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:GRADLE_USER_HOME = Join-Path $projectRoot ".dev-env\\gradle-home"
$env:PATH = "$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:JAVA_HOME\bin;$env:PATH"

& (Join-Path $projectRoot "gradlew.bat") @args
exit $LASTEXITCODE
