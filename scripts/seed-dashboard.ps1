param(
    [Parameter(Mandatory = $true)][string]$ProfileId,
    [Parameter(Mandatory = $true)][string]$StorageTimeZone,
    [string]$DbHost = $(if ($env:DB_HOST) { $env:DB_HOST } else { "localhost" }),
    [int]$DbPort = $(if ($env:DB_PORT) { [int]$env:DB_PORT } else { 3306 }),
    [string]$DbName = $(if ($env:DB_NAME) { $env:DB_NAME } else { "ai_study_mentor" }),
    [string]$DbUser = $(if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "root" })
)

$ErrorActionPreference = "Stop"
$mysqlPassword = $env:DB_PASSWORD
if ([string]::IsNullOrWhiteSpace($mysqlPassword)) {
    throw "Set DB_PASSWORD before seeding. The password is not written to output."
}
$parsedProfileId = [guid]::Empty
if (-not [guid]::TryParse($ProfileId, [ref]$parsedProfileId)) {
    throw "ProfileId must be a UUID."
}
$mysql = Get-Command mysql.exe -ErrorAction SilentlyContinue
if ($null -eq $mysql) {
    throw "mysql.exe was not found on PATH."
}

$storageZone = [TimeZoneInfo]::FindSystemTimeZoneById($StorageTimeZone)
$storageNow = [TimeZoneInfo]::ConvertTime([DateTimeOffset]::UtcNow, $storageZone).DateTime
$subjects = @(
    @{ Subject = "Mathematics"; Topic = "Algebra" },
    @{ Subject = "Physics"; Topic = "Mechanics" },
    @{ Subject = "Biology"; Topic = "Cell Biology" }
)
$statements = [System.Collections.Generic.List[string]]::new()
$questionCount = 0
$attemptCount = 0
$responseCount = 0
$profileSqlId = $parsedProfileId.ToString()

for ($day = 0; $day -lt 42; $day++) {
    $activityTime = $storageNow.AddDays(-$day).AddHours(-($day % 9))
    $subject = $subjects[$day % $subjects.Count]
    $questionId = [guid]::NewGuid().ToString()
    $created = $activityTime.ToString("yyyy-MM-dd HH:mm:ss.ffffff", [Globalization.CultureInfo]::InvariantCulture)
    $statements.Add(
        "INSERT INTO questions (question_id,profile_id,subject,question_text,status,created_at,updated_at) " +
        "VALUES ('$questionId','$profileSqlId','$($subject.Subject)','Dashboard seed question $day','ANSWERED','$created','$created');"
    )
    $questionCount++

    if (($day % 3) -eq 0) {
        $quizId = [guid]::NewGuid().ToString()
        $attemptId = [guid]::NewGuid().ToString()
        $start = $activityTime.AddMinutes(-10).ToString(
            "yyyy-MM-dd HH:mm:ss.ffffff",
            [Globalization.CultureInfo]::InvariantCulture
        )
        $score = if ($subject.Subject -eq "Physics") { 50 } else { 75 }
        $statements.Add(
            "INSERT INTO practice_quizzes (quiz_id,profile_id,subject,topic,source_type,status,created_at,updated_at) " +
            "VALUES ('$quizId','$profileSqlId','$($subject.Subject)','$($subject.Topic)','SUBJECT_TOPIC','COMPLETED','$start','$start');"
        )
        $statements.Add(
            "INSERT INTO quiz_attempts (attempt_id,quiz_id,profile_id,start_time,end_time,score,created_at,updated_at) " +
            "VALUES ('$attemptId','$quizId','$profileSqlId','$start','$created',$score,'$start','$created');"
        )
        $correctCount = 0
        for ($item = 0; $item -lt 4; $item++) {
            $itemId = [guid]::NewGuid().ToString()
            $responseId = [guid]::NewGuid().ToString()
            $isCorrect = if ($subject.Subject -eq "Physics") { $item -lt 2 } else { $item -lt 3 }
            if ($isCorrect) { $correctCount++ }
            $statements.Add(
                "INSERT INTO quiz_questions (question_id,quiz_id,type,prompt,correct_answer,topic,created_at,updated_at) " +
                "VALUES ('$itemId','$quizId','MCQ','Seed quiz question $item','correct','$($subject.Topic)','$start','$start');"
            )
            $correctSql = if ($isCorrect) { "TRUE" } else { "FALSE" }
            $answerText = if ($isCorrect) { "correct" } else { "incorrect" }
            $statements.Add(
                "INSERT INTO quiz_responses (response_id,attempt_id,question_id,subjective_text,is_correct,created_at,updated_at) " +
                "VALUES ('$responseId','$attemptId','$itemId','$answerText',$correctSql,'$created','$created');"
            )
            $responseCount++
        }
        $attemptCount++
    }
}

$sqlPath = Join-Path ([IO.Path]::GetTempPath()) ("dashboard-seed-" + [guid]::NewGuid().ToString("N") + ".sql")
$env:MYSQL_PWD = $mysqlPassword
try {
    $sql = "START TRANSACTION;`r`n" + ($statements -join "`r`n") + "`r`nCOMMIT;`r`n"
    [IO.File]::WriteAllText($sqlPath, $sql, [Text.Encoding]::ASCII)
    $result = & $mysql.Source --host=$DbHost --port=$DbPort --user=$DbUser --database=$DbName `
        --batch --execute "source $sqlPath" 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Dashboard seed SQL failed: $($result -join ' ')"
    }
    Write-Host "Seeded $questionCount questions, $attemptCount completed attempts, and $responseCount graded responses for profile $profileSqlId."
}
finally {
    Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $sqlPath -Force -ErrorAction SilentlyContinue
}
