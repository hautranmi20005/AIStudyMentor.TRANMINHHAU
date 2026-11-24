param([switch] $Force)
. (Join-Path $PSScriptRoot "dev-common.ps1")

if (-not $Force) {
    $confirmation = Read-Host "This deletes all local MySQL, Redis, and Mailpit data. Type RESET to continue"
    if ($confirmation -cne "RESET") {
        Write-Host "Reset cancelled."
        exit 1
    }
}

Import-DevEnvironment
Invoke-DevCompose down --volumes
& (Join-Path $PSScriptRoot "dev-up.ps1")
if ($LASTEXITCODE -ne 0) {
    throw "Unable to start development services after reset."
}
Write-Host "Starting the backend now runs Flyway migrations. Press Ctrl+C to stop the backend (services remain running)."
& (Join-Path $PSScriptRoot "dev-backend.ps1")
