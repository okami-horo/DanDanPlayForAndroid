$ErrorActionPreference = "Stop"

param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\\..")).Path,
    [string]$JdkSource = "C:\Users\Administrator.DESKTOP-1KCKBJ1\.jdks\ms-17.0.15",
    [string]$CmdlineToolsSource = "C:\Android\Sdk\cmdline-tools\latest",
    [string]$LicensesSource = "C:\Android\Sdk\licenses"
)

function Invoke-Robocopy {
    param(
        [Parameter(Mandatory = $true)][string]$Source,
        [Parameter(Mandatory = $true)][string]$Destination
    )

    if (!(Test-Path $Source)) {
        throw "Source path not found: $Source"
    }

    New-Item -ItemType Directory -Force -Path $Destination | Out-Null
    robocopy $Source $Destination /E /NFL /NDL /NJH /NJS /NP /R:1 /W:1 | Out-Null
    if ($LASTEXITCODE -gt 7) {
        throw "robocopy failed with exit code $LASTEXITCODE"
    }
}

$devEnvRoot = Join-Path $ProjectRoot ".dev-env"
$jdkRoot = Join-Path $devEnvRoot "jdk"
$jdkHome = Join-Path $jdkRoot "ms-17.0.15"
$sdkRoot = Join-Path $devEnvRoot "android-sdk"
$cmdlineToolsTarget = Join-Path $sdkRoot "cmdline-tools\\latest"
$licensesTarget = Join-Path $sdkRoot "licenses"
$localProperties = Join-Path $ProjectRoot "local.properties"

New-Item -ItemType Directory -Force -Path $jdkRoot, $sdkRoot | Out-Null

Invoke-Robocopy -Source $JdkSource -Destination $jdkHome
Invoke-Robocopy -Source $CmdlineToolsSource -Destination $cmdlineToolsTarget

if (Test-Path $LicensesSource) {
    Invoke-Robocopy -Source $LicensesSource -Destination $licensesTarget
}

$env:JAVA_HOME = $jdkHome
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

$sdkManager = Join-Path $cmdlineToolsTarget "bin\\sdkmanager.bat"
if (!(Test-Path $sdkManager)) {
    throw "sdkmanager not found: $sdkManager"
}

1..200 | ForEach-Object { "y" } | & $sdkManager --sdk_root=$sdkRoot --licenses | Out-Null
& $sdkManager --sdk_root=$sdkRoot --install `
    "platform-tools" `
    "platforms;android-35" `
    "build-tools;34.0.0" `
    "build-tools;35.0.0" `
    "cmake;3.22.1" `
    "ndk;25.2.9519653"

if (Test-Path $localProperties) {
    $content = Get-Content $localProperties
    $updated = $false
    $sdkLine = "sdk.dir=$($sdkRoot.Replace('\', '/'))"
    $next =
        foreach ($line in $content) {
            if ($line -match '^sdk\.dir=') {
                $updated = $true
                $sdkLine
            } else {
                $line
            }
        }
    if (!$updated) {
        $next = @($sdkLine) + $next
    }
    Set-Content -Path $localProperties -Value $next
}

Write-Output "Project local environment is ready."
Write-Output "JAVA_HOME=$jdkHome"
Write-Output "ANDROID_SDK_ROOT=$sdkRoot"
