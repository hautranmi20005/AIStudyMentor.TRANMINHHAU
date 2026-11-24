. (Join-Path $PSScriptRoot "dev-common.ps1")
Import-DevEnvironment
Assert-DevEnvironment
Invoke-DevCompose up -d --wait
