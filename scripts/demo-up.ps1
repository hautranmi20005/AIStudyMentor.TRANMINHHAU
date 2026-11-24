$ErrorActionPreference = "Stop"
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$backendDirectory = Join-Path $repositoryRoot "backend"
$environmentFile = Join-Path $repositoryRoot "backend\.env"
$adbPath = $null
$javaHome = $env:JAVA_HOME
$knownJdk = "C:\Users\PC\AppData\Local\Programs\Eclipse Adoptium\jdk-17.0.21_9"

if (-not (Test-Path -LiteralPath $environmentFile -PathType Leaf)) {
    throw "backend/.env is missing. Create it from backend/.env.example and set GEMINI_API_KEY."
}

. (Join-Path $PSScriptRoot "dev-common.ps1")
Import-DevEnvironment
Assert-DevEnvironment -RequireGeminiApiKey

if ([string]::IsNullOrWhiteSpace($javaHome) -or
    -not (Test-Path -LiteralPath (Join-Path $javaHome "bin\java.exe"))) {
    $javaHome = $knownJdk
}
$javaExe = Join-Path $javaHome "bin\java.exe"
if (-not (Test-Path -LiteralPath $javaExe)) {
    throw "Temurin JDK 17.0.21 was not found. Set JAVA_HOME to its installation directory."
}
$previousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$javaVersion = (& $javaExe -version 2>&1 | Out-String)
$ErrorActionPreference = $previousErrorActionPreference
if ($javaVersion -notmatch 'version "17\.0\.21"') {
    throw "Demo requires Temurin JDK 17.0.21. Set JAVA_HOME to that JDK."
}
$env:JAVA_HOME = $javaHome
$env:PATH = "$(Join-Path $javaHome 'bin');$env:PATH"

$adbCommand = Get-Command adb -ErrorAction SilentlyContinue
if ($adbCommand) {
    $adbPath = $adbCommand.Source
} else {
    $sdkRoots = @($env:ANDROID_SDK_ROOT, $env:ANDROID_HOME, "C:\Users\PC\AppData\Local\Android\Sdk")
    foreach ($sdkRoot in $sdkRoots) {
        if (-not [string]::IsNullOrWhiteSpace($sdkRoot)) {
            $candidate = Join-Path $sdkRoot "platform-tools\adb.exe"
            if (Test-Path -LiteralPath $candidate) {
                $adbPath = $candidate
                break
            }
        }
    }
}
if (-not $adbPath) {
    throw "adb was not found. Set ANDROID_SDK_ROOT or add platform-tools to PATH."
}
$deviceState = & $adbPath -s emulator-5554 get-state 2>$null
if ($LASTEXITCODE -ne 0 -or $deviceState -ne "device") {
    throw "emulator-5554 is not reachable. Start and boot the demo AVD, then retry."
}

$dockerVersion = & docker info --format '{{.ServerVersion}}' 2>$null
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($dockerVersion)) {
    throw "Docker Engine is unavailable. Start Docker Desktop and retry."
}

Write-Host "[OK] Temurin JDK 17.0.21"
Write-Host "[OK] Gemini key is configured (value hidden)"
Write-Host "[OK] Docker Engine $dockerVersion"
Write-Host "[OK] emulator-5554 is reachable"

Invoke-DevBackendPreflight
Clear-DevProfileOverrides

& (Join-Path $PSScriptRoot "dev-up.ps1")
if ($LASTEXITCODE -ne 0) {
    throw "Local MySQL, Redis, and Mailpit startup failed."
}
$redisResult = Invoke-DevCompose exec -T redis sh -c 'redis-cli --no-auth-warning -a "$REDIS_PASSWORD" ping'
if ($LASTEXITCODE -ne 0 -or ($redisResult -join "").Trim() -ne "PONG") {
    throw "Redis did not become healthy after dev-up."
}
Write-Host "[OK] Redis health check"

Push-Location $backendDirectory
try {
    & .\mvnw.cmd --batch-mode --no-transfer-progress -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        throw "Backend package build failed with exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}

$jar = Join-Path $backendDirectory "target\ai-study-mentor-backend-1.0.0-SNAPSHOT.jar"
if (-not (Test-Path -LiteralPath $jar)) {
    throw "Expected packaged backend JAR was not created: backend/target/ai-study-mentor-backend-1.0.0-SNAPSHOT.jar"
}
$logDirectory = Join-Path $backendDirectory "logs"
New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
$stdoutLog = Join-Path $logDirectory "demo-backend.stdout.log"
$stderrLog = Join-Path $logDirectory "demo-backend.stderr.log"
$statusScript = Join-Path $PSScriptRoot "dev-status.ps1"
$null = & $statusScript 2>$null
$backendProcess = $null
if ($LASTEXITCODE -ne 0) {
    $portOwner = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
    if ($portOwner) {
        throw "Port 8080 is already in use by an unverified process. Stop it and retry."
    }
    $backendProcess = Start-Process -FilePath $javaExe `
        -ArgumentList @("-Dspring.profiles.active=dev", "-jar", $jar) `
        -WorkingDirectory $backendDirectory `
        -RedirectStandardOutput $stdoutLog `
        -RedirectStandardError $stderrLog `
        -PassThru

    try {
        $processRecord = [ordered]@{
            ProcessId = $backendProcess.Id
            StartedAtUtc = $backendProcess.StartTime.ToUniversalTime().ToString("o")
        }
        $processRecord | ConvertTo-Json -Compress |
            Set-Content -LiteralPath (Join-Path $logDirectory "demo-backend.pid")

        $ready = $false
        for ($attempt = 0; $attempt -lt 90; $attempt++) {
            if ($backendProcess.HasExited) {
                $exitCode = $backendProcess.ExitCode
                if ($null -eq $exitCode) {
                    $exitCode = "unknown"
                }
                throw "Spring Boot exited before readiness (code $exitCode). See backend/logs/demo-backend.*.log."
            }
            $null = & $statusScript 2>$null
            if ($LASTEXITCODE -eq 0) {
                $ready = $true
                break
            }
            Start-Sleep -Seconds 2
        }
        if (-not $ready) {
            throw "Spring Boot did not become healthy within 180 seconds. See backend/logs/demo-backend.*.log."
        }
    } catch {
        if (-not $backendProcess.HasExited) {
            Stop-Process -Id $backendProcess.Id -Force -ErrorAction Stop
        }
        $pidFile = Join-Path $logDirectory "demo-backend.pid"
        if (Test-Path -LiteralPath $pidFile -PathType Leaf) {
            Remove-Item -LiteralPath $pidFile -Force -ErrorAction Stop
        }
        throw
    }
}
Write-Host "[OK] Spring Boot healthy; Flyway migrations and dev seed completed during startup"

$androidDirectory = Join-Path $repositoryRoot "android"
Push-Location $androidDirectory
try {
    & .\gradlew.bat --no-daemon "-Pkotlin.compiler.execution.strategy=in-process" :app:assembleDebug
    if ($LASTEXITCODE -ne 0) {
        throw "Android debug build failed with exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}

$apk = Join-Path $androidDirectory "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path -LiteralPath $apk)) {
    throw "Android debug APK was not created."
}
& $adbPath -s emulator-5554 install -r $apk
if ($LASTEXITCODE -ne 0) {
    throw "adb install failed with exit code $LASTEXITCODE."
}
& $adbPath -s emulator-5554 shell monkey -p com.aistudymentor 1
if ($LASTEXITCODE -ne 0) {
    throw "The installed app could not be launched."
}

Write-Host ""
Write-Host "Demo environment is ready."
Write-Host "Demo account: demo.middle@local.test / DemoPass123! (local-only public development fixture)"
Write-Host "TOTP helper: .\scripts\totp.ps1 (PowerShell) or bash ./scripts/totp.sh"
Write-Host "Mailpit: http://127.0.0.1:8025"
Write-Host "API base: http://10.0.2.2:8080/api/v1/ (from emulator)"
Write-Host "Core API routes: /auth/register, /auth/login, /auth/mfa/verify, /profile/me, /questions, /quizzes, /dashboard/metrics, /account"
Write-Host "Backend health: http://127.0.0.1:$env:MANAGEMENT_SERVER_PORT/actuator/health (management credentials required)"
Write-Host "Backend logs: backend/logs/demo-backend.stdout.log and demo-backend.stderr.log"
