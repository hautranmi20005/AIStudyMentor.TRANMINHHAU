$script:DevRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$script:DevEnvFile = Join-Path $script:DevRoot "backend\.env"

function Import-DevEnvironment {
    if (-not (Test-Path $script:DevEnvFile)) {
        & (Join-Path $PSScriptRoot "gen-secrets.ps1")
        if (-not (Test-Path -LiteralPath $script:DevEnvFile -PathType Leaf)) {
            throw "Local environment generation did not create backend/.env."
        }
    }

    $assignments = @{}
    $lineNumber = 0
    foreach ($line in [IO.File]::ReadAllLines($script:DevEnvFile)) {
        $lineNumber++
        $line = $line.Trim()
        if ($line -and -not $line.StartsWith("#")) {
            if ($line -notmatch '^([A-Za-z_][A-Za-z0-9_]*)\s*=(.*)$') {
                throw "Malformed backend/.env assignment on line $lineNumber."
            }
            $name = $matches[1]
            if ($assignments.ContainsKey($name)) {
                throw "Duplicate backend/.env key '$name' on line $lineNumber."
            }
            $assignments[$name] = $matches[2].Trim()
        }
    }

    foreach ($name in $assignments.Keys) {
        if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name, "Process"))) {
            Set-Item -Path "Env:$name" -Value $assignments[$name]
        }
    }
}

function Assert-DevEnvironment {
    param([switch]$RequireGeminiApiKey)

    $requiredVariables = @(
        "DB_URL", "DB_DRIVER", "DB_USERNAME", "DB_PASSWORD", "DB_ROOT_PASSWORD",
        "REDIS_HOST", "REDIS_PORT", "REDIS_PASSWORD", "JWT_SECRET", "MFA_ENCRYPTION_KEY",
        "MANAGEMENT_USERNAME", "MANAGEMENT_PASSWORD", "DEV_SEED_PASSWORD", "DEV_SEED_TOTP_SECRET")
    $missingVariables = @($requiredVariables | Where-Object {
        $value = [Environment]::GetEnvironmentVariable($_, "Process")
        [string]::IsNullOrWhiteSpace($value) -or $value.StartsWith("__GENERATED_BY_SCRIPT__")
    })
    if ($RequireGeminiApiKey) {
        $geminiApiKey = [Environment]::GetEnvironmentVariable("GEMINI_API_KEY", "Process")
        if ([string]::IsNullOrWhiteSpace($geminiApiKey) -or $geminiApiKey -eq "<SET_ME>") {
            $missingVariables += "GEMINI_API_KEY"
        }
    }
    if ($missingVariables.Count -gt 0) {
        throw "Missing or placeholder development settings: $($missingVariables -join ', '). Check backend/.env against backend/.env.example."
    }

    $encryptionKey = [Environment]::GetEnvironmentVariable("MFA_ENCRYPTION_KEY", "Process")
    if ([Text.Encoding]::UTF8.GetByteCount($encryptionKey) -ne 32) {
        throw "MFA_ENCRYPTION_KEY must be exactly 32 UTF-8 bytes."
    }
}

function Clear-DevProfileOverrides {
    $productionVariables = @(
        "POSTGRES_DB", "POSTGRES_USER", "POSTGRES_PASSWORD", "APP_PORT",
        "SPRING_DATASOURCE_URL", "SPRING_DATASOURCE_USERNAME", "SPRING_DATASOURCE_PASSWORD",
        "SPRING_DATA_REDIS_HOST", "SPRING_DATA_REDIS_PORT", "SPRING_DATA_REDIS_USERNAME",
        "SPRING_DATA_REDIS_PASSWORD", "SPRING_DATA_REDIS_SSL_ENABLED",
        "SPRING_DATA_REDIS_CONNECT_TIMEOUT", "SPRING_DATA_REDIS_TIMEOUT",
        "MAIL_USERNAME", "SENDGRID_API_KEY", "MAIL_CONNECTION_TIMEOUT_MS",
        "MAIL_TIMEOUT_MS", "MAIL_WRITE_TIMEOUT_MS")
    foreach ($name in $productionVariables) {
        if (Test-Path -LiteralPath "Env:$name") {
            Remove-Item -LiteralPath "Env:$name" -ErrorAction Stop
        }
    }
}

function Stop-DevBackend {
    param([switch] $RemoveLogs)

    $backendDirectory = [System.IO.Path]::GetFullPath((Join-Path $script:DevRoot "backend"))
    $backendPrefix = $backendDirectory.TrimEnd('\') + '\'
    $runtimeLogsDirectory = Join-Path $backendDirectory "logs"
    $legacyLogsDirectory = Join-Path $backendDirectory "target\logs"
    $logDirectories = @($runtimeLogsDirectory, $legacyLogsDirectory)
    $trackedProcessStartTimes = @{}

    foreach ($logsDirectory in $logDirectories) {
        if (Test-Path -LiteralPath $logsDirectory -PathType Container) {
            Get-ChildItem -LiteralPath $logsDirectory -Filter "*.pid" -File | ForEach-Object {
                $pidText = (Get-Content -LiteralPath $_.FullName -Raw).Trim()
                if ($pidText -match '^\d+$') {
                    $trackedProcessStartTimes[[int]$pidText] = $null
                } elseif ($pidText.StartsWith("{")) {
                    $processRecord = ConvertFrom-Json -InputObject $pidText -ErrorAction Stop
                    if ($processRecord.ProcessId -match '^\d+$' -and
                        -not [string]::IsNullOrWhiteSpace($processRecord.StartedAtUtc)) {
                        $trackedProcessStartTimes[[int]$processRecord.ProcessId] =
                            [DateTime]::Parse($processRecord.StartedAtUtc).ToUniversalTime()
                    } else {
                        throw "Invalid backend PID record: $($_.FullName)"
                    }
                } else {
                    throw "Invalid backend PID record: $($_.FullName)"
                }
            }
        }
    }

    $portProcessIds = @(
        Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue |
            Select-Object -ExpandProperty OwningProcess -Unique
    )
    $javaProcesses = @(Get-CimInstance Win32_Process `
        -Filter "Name = 'java.exe' OR Name = 'javaw.exe'" -ErrorAction Stop)
    $backendProcesses = @()
    foreach ($process in $javaProcesses) {
        $commandLine = [string]$process.CommandLine
        $normalizedCommandLine = $commandLine.Replace('/', '\')
        $isBackendPath = $normalizedCommandLine.IndexOf(
            $backendPrefix, [System.StringComparison]::OrdinalIgnoreCase) -ge 0
        $isBackendCommand = $normalizedCommandLine -match '(?i)ai-study-mentor-backend'
        $ownsBackendPort = $portProcessIds -contains [int]$process.ProcessId
        $isTrackedBackend = $false
        $processId = [int]$process.ProcessId
        if ($trackedProcessStartTimes.ContainsKey($processId)) {
            $expectedStartTime = $trackedProcessStartTimes[$processId]
            if ($null -ne $expectedStartTime) {
                $actualStartTime = ([DateTime]$process.CreationDate).ToUniversalTime()
                $isTrackedBackend = [Math]::Abs(($actualStartTime - $expectedStartTime).TotalSeconds) -le 2
            } else {
                $isTrackedBackend = $isBackendCommand
            }
        }

        if ($isBackendPath -or $ownsBackendPort -or $isTrackedBackend -or
            $isBackendCommand) {
            $backendProcesses += $process
        }
    }

    if ($backendProcesses.Count -eq 0) {
        Write-Host "[CLEANUP] No backend Java processes were found."
    }

    foreach ($process in $backendProcesses) {
        $processId = [int]$process.ProcessId
        Write-Host "[CLEANUP] Stopping backend Java process PID $processId"
        Stop-Process -Id $processId -Force -ErrorAction Stop
        for ($attempt = 0; $attempt -lt 50; $attempt++) {
            if (-not (Get-Process -Id $processId -ErrorAction SilentlyContinue)) {
                break
            }
            Start-Sleep -Milliseconds 100
        }
        if (Get-Process -Id $processId -ErrorAction SilentlyContinue) {
            throw "Backend Java process PID $processId did not stop; refusing to remove its log files."
        }
    }

    foreach ($logsDirectory in $logDirectories) {
        if (Test-Path -LiteralPath $logsDirectory -PathType Container) {
            Get-ChildItem -LiteralPath $logsDirectory -Filter "*.pid" -File |
                Remove-Item -Force -ErrorAction Stop
            if ($RemoveLogs) {
                foreach ($logFile in (Get-ChildItem -LiteralPath $logsDirectory -Filter "*.log" -File)) {
                    try {
                        Remove-Item -LiteralPath $logFile.FullName -Force -ErrorAction Stop
                    }
                    catch {
                        if ($logsDirectory -eq $legacyLogsDirectory) {
                            Write-Warning "Preserving locked legacy log '$($logFile.FullName)'; Maven clean excludes backend/target/logs."
                        } else {
                            throw "Could not remove backend runtime log '$($logFile.FullName)': $($_.Exception.Message)"
                        }
                    }
                }
            }
        }
    }
}

function Invoke-DevBackendPreflight {
    Stop-DevBackend -RemoveLogs
}

function Invoke-DevCompose {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]] $Arguments)

    Push-Location $script:DevRoot
    try {
        & docker compose --project-name aistudymentor-dev --env-file $script:DevEnvFile -f docker-compose.dev.yml @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "Docker Compose command failed with exit code $LASTEXITCODE."
        }
    }
    finally {
        Pop-Location
    }
}
