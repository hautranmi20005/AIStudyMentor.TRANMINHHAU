. (Join-Path $PSScriptRoot "dev-common.ps1")
Import-DevEnvironment
$failed = $false
$mailUiPort = if ([string]::IsNullOrWhiteSpace($env:DEV_MAIL_UI_PORT)) { 8025 } else { $env:DEV_MAIL_UI_PORT }

function Show-Check {
    param([string] $Name, [scriptblock] $Check)
    try {
        & $Check | Out-Null
        Write-Host "[OK]   $Name"
    }
    catch {
        $script:failed = $true
        Write-Host "[FAIL] $Name"
    }
}

Show-Check "MySQL" {
    Invoke-DevCompose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -h 127.0.0.1 -uroot --silent -N -e SELECT\ 1'
}
Show-Check "Redis" {
    Invoke-DevCompose exec -T redis sh -c 'redis-cli --no-auth-warning -a "$REDIS_PASSWORD" ping'
}
Show-Check "Mailpit" {
    $null = Invoke-WebRequest -Uri "http://127.0.0.1:$mailUiPort/livez" -UseBasicParsing -TimeoutSec 3
}
Show-Check "Backend actuator health" {
    $pair = "$env:MANAGEMENT_USERNAME`:$env:MANAGEMENT_PASSWORD"
    $encoded = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($pair))
    $headers = @{ Authorization = "Basic $encoded" }
    $response = Invoke-RestMethod -Uri "http://127.0.0.1:$env:MANAGEMENT_SERVER_PORT/actuator/health" -Headers $headers -TimeoutSec 5
    if ($response.status -ne "UP") { throw "Backend health status is not UP." }
}
if ($failed) { exit 1 }
