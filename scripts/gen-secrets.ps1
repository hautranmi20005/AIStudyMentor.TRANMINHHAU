param(
    [switch]$Overwrite
)

$ErrorActionPreference = "Stop"
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$examplePath = Join-Path $repositoryRoot "backend\.env.example"
$environmentPath = Join-Path $repositoryRoot "backend\.env"

if (-not (Test-Path -LiteralPath $examplePath -PathType Leaf)) {
    throw "Required template not found: backend/.env.example"
}

if ((Test-Path -LiteralPath $environmentPath) -and -not $Overwrite) {
    $answer = Read-Host "backend/.env exists. Replace it with newly generated local credentials? Type YES to continue"
    if ($answer -cne "YES") {
        throw "Existing backend/.env was not changed."
    }
}

function New-Base64Secret([int]$ByteCount) {
    $bytes = New-Object byte[] $ByteCount
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    }
    finally {
        $generator.Dispose()
    }
    return [Convert]::ToBase64String($bytes)
}

$secrets = [ordered]@{
    "JWT_SECRET" = New-Base64Secret 64
    "MFA_ENCRYPTION_KEY" = New-Base64Secret 24
    "DB_PASSWORD" = New-Base64Secret 32
    "DB_ROOT_PASSWORD" = New-Base64Secret 32
    "REDIS_PASSWORD" = New-Base64Secret 32
    "MANAGEMENT_PASSWORD" = New-Base64Secret 32
}

$counts = @{}
$lines = [System.Collections.Generic.List[string]]::new()
foreach ($line in [IO.File]::ReadAllLines($examplePath)) {
    if ($line -match '^([A-Z][A-Z0-9_]*)=') {
        $name = $matches[1]
        if ($secrets.Contains($name)) {
            $counts[$name] = 1 + [int]$counts[$name]
            $lines.Add("$name=$($secrets[$name])")
            continue
        }
    }
    $lines.Add($line)
}

foreach ($name in $secrets.Keys) {
    if ($counts[$name] -ne 1) {
        throw "Template must contain exactly one $name entry; no output file was changed."
    }
}

$temporaryPath = Join-Path (Split-Path -Parent $environmentPath) ([IO.Path]::GetRandomFileName())
try {
    $content = [string]::Join([Environment]::NewLine, $lines) + [Environment]::NewLine
    [IO.File]::WriteAllText($temporaryPath, $content, [Text.UTF8Encoding]::new($false))
    Move-Item -LiteralPath $temporaryPath -Destination $environmentPath -Force
}
finally {
    if (Test-Path -LiteralPath $temporaryPath) {
        Remove-Item -LiteralPath $temporaryPath -Force
    }
}

Write-Output "Generated backend/.env with fresh local credentials. Secret values were not displayed."
