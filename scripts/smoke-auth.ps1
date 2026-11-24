param(
    [string] $BaseUrl = "http://127.0.0.1:8080/api/v1",
    [string] $DemoEmail = "demo.middle@local.test",
    [string] $DemoPassword = ""
)

. (Join-Path $PSScriptRoot "dev-common.ps1")
Import-DevEnvironment
if (-not $DemoPassword) { $DemoPassword = $env:DEV_SEED_PASSWORD }

$email = "smoke.$([Guid]::NewGuid().ToString('N'))@local.test"
$password = "LocalTest123!"
$registration = @{
    email = $email
    password = $password
    dob = "2000-01-01"
    educationLevel = "University"
} | ConvertTo-Json
$registered = Invoke-WebRequest -Method Post -Uri "$BaseUrl/auth/register" -ContentType "application/json" -Body $registration -UseBasicParsing
if ($registered.StatusCode -ne 201) { throw "Registration returned HTTP $($registered.StatusCode)." }

$loginBody = @{ email = $email; password = $password } | ConvertTo-Json
$newLogin = Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/login" -ContentType "application/json" -Body $loginBody
if ($newLogin.mfaRequired) { throw "Newly registered account unexpectedly requires enrolled MFA." }

$demoLoginBody = @{ email = $DemoEmail; password = $DemoPassword } | ConvertTo-Json
$demoLogin = Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/login" -ContentType "application/json" -Body $demoLoginBody
if (-not $demoLogin.mfaRequired -or -not $demoLogin.mfaSessionToken) {
    throw "Seeded demo login did not return an MFA challenge."
}
$code = (& (Join-Path $PSScriptRoot "totp.ps1") -Secret $env:DEV_SEED_TOTP_SECRET).Trim()
$verifyBody = @{
    mfaSessionToken = $demoLogin.mfaSessionToken
    code = $code
    type = "TOTP"
} | ConvertTo-Json
$verified = Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/mfa/verify" -ContentType "application/json" -Body $verifyBody
if (-not $verified.accessToken) { throw "TOTP verification did not return an access token." }

Write-Host "[OK] Register: HTTP $($registered.StatusCode)"
Write-Host "[OK] Login for new account"
Write-Host "[OK] Demo MFA login and TOTP verification"
