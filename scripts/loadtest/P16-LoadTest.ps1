[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ConfigPath,
    [string]$BaseUrl = "http://127.0.0.1:8080/api/v1",
    [int]$ConcurrentUsers = 50,
    [int]$DurationSeconds = 120,
    [int]$RequestTimeoutSeconds = 60,
    [string]$OutputDirectory = (Join-Path $PSScriptRoot "..\..\docs\evidence\P16"),
    [switch]$ValidateOnly
)

$ErrorActionPreference = "Stop"

if ($ConcurrentUsers -lt 1 -or $DurationSeconds -lt 1 -or $RequestTimeoutSeconds -lt 1) {
    throw "ConcurrentUsers, DurationSeconds, and RequestTimeoutSeconds must be positive."
}

$configFile = (Resolve-Path -LiteralPath $ConfigPath).Path
$config = Get-Content -LiteralPath $configFile -Raw | ConvertFrom-Json
if ($null -eq $config.users -or $config.users.Count -eq 0) {
    throw "Config must contain at least one test user."
}
if ($null -eq $config.askBody -or [string]::IsNullOrWhiteSpace($config.askBody.subject) `
    -or [string]::IsNullOrWhiteSpace($config.askBody.questionText)) {
    throw "Config must contain askBody.subject and askBody.questionText."
}
if ($ValidateOnly) {
    Write-Host "Config is valid: $($config.users.Count) user record(s); credentials were not printed."
    return
}

if ($config.users.Count -lt $ConcurrentUsers) {
    throw "Provide at least one distinct user record per concurrent worker."
}
if ([string]::IsNullOrWhiteSpace($config.idempotencyKeyPrefix)) {
    throw "Config must contain a non-empty idempotencyKeyPrefix."
}
foreach ($user in $config.users) {
    if ([string]::IsNullOrWhiteSpace($user.email) -or [string]::IsNullOrWhiteSpace($user.password) -or
        [string]::IsNullOrWhiteSpace($user.accessToken)) {
        throw "Every user record must contain email, password, and accessToken."
    }
    if ([string]::IsNullOrWhiteSpace($user.quizId) -or $null -eq $user.quizResponses -or
        $user.quizResponses.Count -eq 0) {
        throw "Every user record must contain quizId and quizResponses."
    }
}

$resolvedOutputDirectory = [System.IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Path $resolvedOutputDirectory -Force | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$samplePath = Join-Path $resolvedOutputDirectory "backend-load-$stamp.csv"
$summaryPath = Join-Path $resolvedOutputDirectory "backend-load-summary-$stamp.json"
$configJson = $config | ConvertTo-Json -Depth 30 -Compress
$scenarioNames = @("login", "ask-text", "cache-hit-ask", "history-list", "dashboard", "quiz-grading")
$samples = [System.Collections.Concurrent.ConcurrentBag[object]]::new()
$ready = [System.Threading.CountdownEvent]::new($ConcurrentUsers)
$startGate = [System.Threading.ManualResetEventSlim]::new($false)
$pool = [System.Management.Automation.Runspaces.RunspaceFactory]::CreateRunspacePool(1, $ConcurrentUsers)
$pool.Open()
$workers = [System.Collections.Generic.List[object]]::new()
$workerScript = {
    param(
        [int]$WorkerId,
        [string]$Scenario,
        [string]$ConfigJson,
        [string]$ServiceBaseUrl,
        [int]$Duration,
        [int]$TimeoutSeconds,
        [System.Collections.Concurrent.ConcurrentBag[object]]$Samples,
        [System.Threading.CountdownEvent]$Ready,
        [System.Threading.ManualResetEventSlim]$StartGate
    )

    Add-Type -AssemblyName System.Net.Http
    $settings = $ConfigJson | ConvertFrom-Json
    $user = $settings.users[$WorkerId % $settings.users.Count]
    $client = [System.Net.Http.HttpClient]::new()
    $client.Timeout = [TimeSpan]::FromSeconds($TimeoutSeconds)

    function Send-LoadRequest {
        param(
            [string]$Method,
            [string]$Path,
            [string]$Body,
            [string]$Token,
            [string]$IdempotencyKey,
            [System.Net.Http.HttpClient]$Client,
            [string]$RootUrl
        )

        $request = [System.Net.Http.HttpRequestMessage]::new(
            [System.Net.Http.HttpMethod]::new($Method),
            "$RootUrl$Path"
        )
        [void]$request.Headers.TryAddWithoutValidation("X-Correlation-Id", [guid]::NewGuid().ToString())
        if (-not [string]::IsNullOrWhiteSpace($Token)) {
            [void]$request.Headers.TryAddWithoutValidation("Authorization", "Bearer $Token")
        }
        if (-not [string]::IsNullOrWhiteSpace($IdempotencyKey)) {
            [void]$request.Headers.TryAddWithoutValidation("Idempotency-Key", $IdempotencyKey)
        }
        if (-not [string]::IsNullOrWhiteSpace($Body)) {
            $request.Content = [System.Net.Http.StringContent]::new(
                $Body,
                [System.Text.Encoding]::UTF8,
                "application/json"
            )
        }

        $timer = [System.Diagnostics.Stopwatch]::StartNew()
        $response = $null
        try {
            $response = $Client.SendAsync($request).GetAwaiter().GetResult()
            $responseBody = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            $timer.Stop()
            $cacheSignal = $false
            try {
                $parsed = $responseBody | ConvertFrom-Json
                $cacheSignal = ($parsed.cacheHit -eq $true) -or ($parsed.servedFromCache -eq $true)
            } catch {
                $cacheSignal = $false
            }
            return [pscustomobject]@{
                StatusCode = [int]$response.StatusCode
                ElapsedMs = [math]::Round($timer.Elapsed.TotalMilliseconds, 2)
                CacheSignal = $cacheSignal
                Body = $responseBody
                ErrorType = ""
            }
        } catch {
            $timer.Stop()
            return [pscustomobject]@{
                StatusCode = 0
                ElapsedMs = [math]::Round($timer.Elapsed.TotalMilliseconds, 2)
                CacheSignal = $false
                Body = ""
                ErrorType = $_.Exception.GetType().Name
            }
        } finally {
            if ($null -ne $response) {
                $response.Dispose()
            }
            $request.Dispose()
        }
    }

    function Add-Sample {
        param([string]$Stage, [int]$Iteration, $Result)
        $Samples.Add([pscustomobject]@{
            timestamp_utc = [DateTimeOffset]::UtcNow.ToString("o")
            stage = $Stage
            scenario = $Scenario
            worker = $WorkerId
            iteration = $Iteration
            latency_ms = $Result.ElapsedMs
            status_code = $Result.StatusCode
            success = ($Result.StatusCode -ge 200 -and $Result.StatusCode -lt 300)
            cache_hit_signal = $Result.CacheSignal
            error_type = $Result.ErrorType
        })
    }

    $signaledReady = $false
    try {
        if ($Scenario -eq "cache-hit-ask") {
            $warmBody = $settings.askBody | ConvertTo-Json -Depth 20 -Compress
            $warmKey = "$($settings.idempotencyKeyPrefix)-$WorkerId"
            $warm = Send-LoadRequest -Method "POST" -Path "/questions" -Body $warmBody `
                -Token $user.accessToken -IdempotencyKey $warmKey -Client $client -RootUrl $ServiceBaseUrl
            Add-Sample -Stage "warmup" -Iteration 0 -Result $warm
        }

        $Ready.Signal()
        $signaledReady = $true
        $StartGate.Wait()
        $deadline = [DateTimeOffset]::UtcNow.AddSeconds($Duration)
        $iteration = 0
        while ([DateTimeOffset]::UtcNow -lt $deadline) {
            $iteration++
            $result = $null
            switch ($Scenario) {
                "login" {
                    $body = @{ email = $user.email; password = $user.password } | ConvertTo-Json -Compress
                    $result = Send-LoadRequest -Method "POST" -Path "/auth/login" -Body $body `
                        -Token "" -IdempotencyKey "" -Client $client -RootUrl $ServiceBaseUrl
                    Add-Sample -Stage "measured" -Iteration $iteration -Result $result
                }
                "ask-text" {
                    $bodyObject = [ordered]@{}
                    foreach ($property in $settings.askBody.PSObject.Properties) {
                        $bodyObject[$property.Name] = $property.Value
                    }
                    $bodyObject.questionText = "$($settings.askBody.questionText) Reference $WorkerId-$iteration"
                    $body = $bodyObject | ConvertTo-Json -Depth 20 -Compress
                    $result = Send-LoadRequest -Method "POST" -Path "/questions" -Body $body `
                        -Token $user.accessToken -IdempotencyKey "$($settings.idempotencyKeyPrefix)-$WorkerId-$iteration" `
                        -Client $client -RootUrl $ServiceBaseUrl
                    Add-Sample -Stage "measured" -Iteration $iteration -Result $result
                }
                "cache-hit-ask" {
                    $body = $settings.askBody | ConvertTo-Json -Depth 20 -Compress
                    $key = "$($settings.idempotencyKeyPrefix)-$WorkerId"
                    $result = Send-LoadRequest -Method "POST" -Path "/questions" -Body $body `
                        -Token $user.accessToken -IdempotencyKey $key -Client $client -RootUrl $ServiceBaseUrl
                    Add-Sample -Stage "measured" -Iteration $iteration -Result $result
                }
                "history-list" {
                    $result = Send-LoadRequest -Method "GET" -Path "/questions?page=0&size=20" -Body "" `
                        -Token $user.accessToken -IdempotencyKey "" -Client $client -RootUrl $ServiceBaseUrl
                    Add-Sample -Stage "measured" -Iteration $iteration -Result $result
                }
                "dashboard" {
                    $result = Send-LoadRequest -Method "GET" -Path "/dashboard/metrics?range=7d&tz=UTC" -Body "" `
                        -Token $user.accessToken -IdempotencyKey "" -Client $client -RootUrl $ServiceBaseUrl
                    Add-Sample -Stage "measured" -Iteration $iteration -Result $result
                }
                "quiz-grading" {
                    $quizId = [uri]::EscapeDataString([string]$user.quizId)
                    $start = Send-LoadRequest -Method "POST" -Path "/quizzes/$quizId/attempts" -Body "" `
                        -Token $user.accessToken -IdempotencyKey "" -Client $client -RootUrl $ServiceBaseUrl
                    try {
                        $attemptId = ($start.Body | ConvertFrom-Json).attemptId
                    } catch {
                        $attemptId = $null
                    }
                    if ($start.StatusCode -lt 200 -or $start.StatusCode -ge 300 -or [string]::IsNullOrWhiteSpace($attemptId)) {
                        $start.ErrorType = "QuizAttemptStartFailed"
                        Add-Sample -Stage "measured" -Iteration $iteration -Result $start
                    } else {
                        $responses = ConvertTo-Json -InputObject @($user.quizResponses) -Depth 20 -Compress
                        $submitBody = '{"responses":' + $responses + '}'
                        $submitPath = "/quizzes/$quizId/attempts/$([uri]::EscapeDataString([string]$attemptId))/submit"
                        $graded = Send-LoadRequest -Method "POST" -Path $submitPath -Body $submitBody `
                            -Token $user.accessToken -IdempotencyKey "" -Client $client -RootUrl $ServiceBaseUrl
                        Add-Sample -Stage "measured" -Iteration $iteration -Result $graded
                    }
                }
            }
        }
    } finally {
        if (-not $signaledReady) {
            $Ready.Signal()
        }
        $client.Dispose()
    }
}.ToString()

try {
    for ($workerId = 0; $workerId -lt $ConcurrentUsers; $workerId++) {
        $powershell = [System.Management.Automation.PowerShell]::Create()
        $powershell.RunspacePool = $pool
        $scenario = $scenarioNames[$workerId % $scenarioNames.Count]
        [void]$powershell.AddScript($workerScript).
            AddArgument($workerId).
            AddArgument($scenario).
            AddArgument($configJson).
            AddArgument($BaseUrl.TrimEnd('/')).
            AddArgument($DurationSeconds).
            AddArgument($RequestTimeoutSeconds).
            AddArgument($samples).
            AddArgument($ready).
            AddArgument($startGate)
        $workers.Add([pscustomobject]@{ PowerShell = $powershell; Handle = $powershell.BeginInvoke() })
    }

    if (-not $ready.Wait([TimeSpan]::FromSeconds(120))) {
        $startGate.Set()
        throw "Load workers did not reach the start barrier within 120 seconds."
    }

    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $startGate.Set()
    foreach ($worker in $workers) {
        [void]$worker.PowerShell.EndInvoke($worker.Handle)
        if ($worker.PowerShell.Streams.Error.Count -gt 0) {
            throw "A load worker failed: $($worker.PowerShell.Streams.Error[0].Exception.GetType().Name)"
        }
        $worker.PowerShell.Dispose()
    }
    $stopwatch.Stop()
} finally {
    $startGate.Set()
    foreach ($worker in $workers) {
        if ($null -ne $worker.PowerShell) {
            $worker.PowerShell.Dispose()
        }
    }
    $pool.Close()
    $pool.Dispose()
    $startGate.Dispose()
    $ready.Dispose()
}

$measured = @($samples | Where-Object stage -eq "measured")
$measured | Sort-Object timestamp_utc, worker, iteration |
    Export-Csv -LiteralPath $samplePath -NoTypeInformation -Encoding UTF8

function Get-NearestRankPercentile {
    param([double[]]$Values, [double]$Percentile)
    if ($Values.Count -eq 0) { return $null }
    $ordered = @($Values | Sort-Object)
    $index = [math]::Max(0, [math]::Ceiling($Percentile * $ordered.Count) - 1)
    return [math]::Round($ordered[$index], 2)
}

$summary = @($measured | Group-Object scenario | ForEach-Object {
    $group = @($_.Group)
    $latencies = [double[]]@($group | ForEach-Object { [double]$_.latency_ms })
    $errors = @($group | Where-Object { -not $_.success }).Count
    [pscustomobject]@{
        scenario = $_.Name
        requests = $group.Count
        errors = $errors
        error_rate = [math]::Round($errors / [double]$group.Count, 4)
        p50_ms = Get-NearestRankPercentile -Values $latencies -Percentile 0.50
        p95_ms = Get-NearestRankPercentile -Values $latencies -Percentile 0.95
        p99_ms = Get-NearestRankPercentile -Values $latencies -Percentile 0.99
        throughput_requests_per_second = [math]::Round($group.Count / $stopwatch.Elapsed.TotalSeconds, 3)
        cache_hit_signals = @($group | Where-Object cache_hit_signal).Count
    }
})

$report = [pscustomobject]@{
    started_at_utc = [DateTimeOffset]::UtcNow.Subtract($stopwatch.Elapsed).ToString("o")
    base_url = $BaseUrl
    concurrent_users = $ConcurrentUsers
    configured_user_records = $config.users.Count
    target_duration_seconds = $DurationSeconds
    actual_duration_seconds = [math]::Round($stopwatch.Elapsed.TotalSeconds, 2)
    backend_provider = $config.provider
    stub_latency_ms = $config.stubLatencyMs
    samples_file = [System.IO.Path]::GetFileName($samplePath)
    scenarios = $summary
}
$report | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $summaryPath -Encoding UTF8

$summary | Format-Table -AutoSize
Write-Host "Raw samples: $samplePath"
Write-Host "Summary: $summaryPath"
