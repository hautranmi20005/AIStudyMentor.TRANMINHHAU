param(
    [string] $BaseUrl = "http://127.0.0.1:8081"
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "dev-common.ps1")
Import-DevEnvironment

if ([string]::IsNullOrWhiteSpace($env:MANAGEMENT_USERNAME) -or
    [string]::IsNullOrWhiteSpace($env:MANAGEMENT_PASSWORD)) {
    throw "Management credentials are required to read the AI cost report."
}

$credentialBytes = [Text.Encoding]::UTF8.GetBytes(
    "$($env:MANAGEMENT_USERNAME):$($env:MANAGEMENT_PASSWORD)")
$authorization = [Convert]::ToBase64String($credentialBytes)
$report = Invoke-RestMethod `
    -Uri "$($BaseUrl.TrimEnd('/'))/actuator/ai-cost" `
    -Headers @{ Authorization = "Basic $authorization"; Accept = "application/json" } `
    -Method Get

$report | Format-List providerCalls, cacheHits, cacheMisses, cacheErrors,
    cacheHitRate, estimatedTokensSaved, estimatedCostUsd, estimatedCostSavedUsd
