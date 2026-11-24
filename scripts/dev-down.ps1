. (Join-Path $PSScriptRoot "dev-common.ps1")
Import-DevEnvironment
Stop-DevBackend
Invoke-DevCompose down
