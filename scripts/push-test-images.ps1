param(
    [string]$Device
)

$ErrorActionPreference = "Stop"
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$assetDirectory = Join-Path $repositoryRoot ".p09-test-images"
$generator = Join-Path $PSScriptRoot "generate-p09-test-images.py"

$python = Get-Command python -ErrorAction SilentlyContinue
if (-not $python) {
    throw "Python 3 is required to generate the P09 photo test images."
}

$adbCommand = Get-Command adb -ErrorAction SilentlyContinue
if ($adbCommand) {
    $adb = $adbCommand.Source
} else {
    $sdkRoots = @($env:ANDROID_SDK_ROOT, $env:ANDROID_HOME)
    if ($env:LOCALAPPDATA) {
        $sdkRoots += (Join-Path $env:LOCALAPPDATA "Android\Sdk")
    }
    $adb = $null
    foreach ($sdkRoot in $sdkRoots) {
        if ($sdkRoot) {
            $candidate = Join-Path $sdkRoot "platform-tools\adb.exe"
            if (Test-Path $candidate) {
                $adb = $candidate
                break
            }
        }
    }
    if (-not $adb) {
        throw "adb was not found. Add Android platform-tools to PATH or set ANDROID_SDK_ROOT."
    }
}

& $python.Source $generator $assetDirectory
if ($LASTEXITCODE -ne 0) { throw "P09 test-image generation failed." }

$deviceArguments = @()
if ($Device) { $deviceArguments = @("-s", $Device) }
$remoteDirectory = "/sdcard/Pictures/P09"
& $adb @deviceArguments shell mkdir -p $remoteDirectory
if ($LASTEXITCODE -ne 0) { throw "Could not create the P09 image directory on the device." }

Get-ChildItem $assetDirectory -File | ForEach-Object {
    $remotePath = "$remoteDirectory/$($_.Name)"
    & $adb @deviceArguments push $_.FullName $remotePath
    if ($LASTEXITCODE -ne 0) { throw "Could not push $($_.Name) to the device." }
    & $adb @deviceArguments shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d "file://$remotePath"
    if ($LASTEXITCODE -ne 0) { throw "Could not scan $($_.Name) into Android media storage." }
}

Write-Host "P09 images are available in the Android Photo Picker under Pictures/P09."
