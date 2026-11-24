param(
    [string]$BaseUrl = $(if ($env:AI_SMOKE_BASE_URL) { $env:AI_SMOKE_BASE_URL } else { "http://127.0.0.1:8080" })
)

$token = $env:AI_SMOKE_BEARER_TOKEN
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "Set AI_SMOKE_BEARER_TOKEN to a short-lived access token before running this smoke test."
}

$headers = @{
    Authorization = "Bearer $token"
    Accept = "application/json"
}
$body = @{
    subject = "Mathematics"
    questionText = "Solve 2 + 2. Return a clear step-by-step explanation."
} | ConvertTo-Json -Compress

try {
    $result = Invoke-RestMethod `
        -Uri "$($BaseUrl.TrimEnd('/'))/api/v1/questions" `
        -Method Post `
        -Headers $headers `
        -ContentType "application/json" `
        -Body $body
} catch {
    $code = "AI_REQUEST_FAILED"
    $response = $_.Exception.Response
    if ($response -and $response.GetResponseStream()) {
        try {
            $reader = [System.IO.StreamReader]::new($response.GetResponseStream())
            $errorBody = $reader.ReadToEnd() | ConvertFrom-Json
            if ($errorBody.code) {
                $code = [string]$errorBody.code
            }
        } catch {
            $code = "AI_REQUEST_FAILED"
        }
    }
    throw "AI smoke test failed with $code. Response content and credentials were not printed."
}

$answer = $result.answer.structuredExplanation
if (-not $answer -or -not $answer.subject -or -not $answer.difficulty -or
    -not $answer.steps -or $answer.steps.Count -lt 1 -or
    -not $answer.finalAnswer -or -not $answer.keyConcepts -or
    -not $answer.commonMistakes -or -not $answer.followUpQuestions) {
    throw "AI smoke test returned an incomplete structured answer."
}
if ($answer.modelId -eq "stub" -or $result.answer.modelId -eq "stub") {
    throw "AI smoke test received Demo AI output; configure the Gemini provider for this smoke test."
}

Write-Output "AI smoke test passed."
Write-Output "Model: $($result.answer.modelId)"
Write-Output "Structured fields: valid"
Write-Output "Remaining AI calls today: $($answer.remainingCallsToday)"
