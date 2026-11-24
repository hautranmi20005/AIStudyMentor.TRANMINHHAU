param(
    [Parameter(Mandatory = $true)][string]$ProfileId,
    [ValidateSet("7d", "30d", "all")][string]$Range = "30d",
    [string]$TimeZone = "UTC",
    [Parameter(Mandatory = $true)][string]$StorageTimeZone,
    [string]$ApiBaseUrl = "http://localhost:8080/api/v1",
    [string]$DbHost = $(if ($env:DB_HOST) { $env:DB_HOST } else { "localhost" }),
    [int]$DbPort = $(if ($env:DB_PORT) { [int]$env:DB_PORT } else { 3306 }),
    [string]$DbName = $(if ($env:DB_NAME) { $env:DB_NAME } else { "ai_study_mentor" }),
    [string]$DbUser = $(if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "root" }),
    [int]$TopSubjects = 5,
    [int]$WeakTopicMinimum = 3,
    [double]$WeakTopicMaximumAccuracy = 60.0
)

$ErrorActionPreference = "Stop"
$accessToken = $env:DASHBOARD_ACCESS_TOKEN
$mysqlPassword = $env:DB_PASSWORD
if ([string]::IsNullOrWhiteSpace($accessToken)) {
    throw "Set DASHBOARD_ACCESS_TOKEN to a valid bearer token. The token is not written to output."
}
if ([string]::IsNullOrWhiteSpace($mysqlPassword)) {
    throw "Set DB_PASSWORD for the independent SQL checks. The password is not written to output."
}
$parsedProfileId = [guid]::Empty
if (-not [guid]::TryParse($ProfileId, [ref]$parsedProfileId)) {
    throw "ProfileId must be a UUID."
}

$mysql = Get-Command mysql.exe -ErrorAction SilentlyContinue
if ($null -eq $mysql) {
    throw "mysql.exe was not found on PATH."
}
$requestedZone = [TimeZoneInfo]::FindSystemTimeZoneById($TimeZone)
$storageZone = [TimeZoneInfo]::FindSystemTimeZoneById($StorageTimeZone)
$env:MYSQL_PWD = $mysqlPassword

function Invoke-DashboardSql([string]$Query) {
    $result = & $mysql.Source --host=$DbHost --port=$DbPort --user=$DbUser --database=$DbName `
        --batch --skip-column-names --raw --execute $Query 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Independent dashboard SQL query failed: $($result -join ' ')"
    }
    return @($result | ForEach-Object { "$_".TrimEnd("`r") } | Where-Object { $_ -ne "" })
}

function Convert-ToStorageDateTime([DateTime]$LocalDate, [TimeZoneInfo]$SourceZone, [TimeZoneInfo]$TargetZone) {
    $unspecified = [DateTime]::SpecifyKind($LocalDate, [DateTimeKind]::Unspecified)
    $utc = [TimeZoneInfo]::ConvertTimeToUtc($unspecified, $SourceZone)
    $storage = [TimeZoneInfo]::ConvertTimeFromUtc($utc, $TargetZone)
    return $storage.ToString("yyyy-MM-dd HH:mm:ss.ffffff", [Globalization.CultureInfo]::InvariantCulture)
}

function Assert-Same([string]$Name, $Expected, $Actual) {
    if ([string]$Expected -ne [string]$Actual) {
        throw "$Name mismatch: API=$Expected, SQL=$Actual"
    }
    Write-Host "PASS $Name"
}

function Get-DateWindowClause([string]$Column, [string]$Start, [string]$End) {
    $clause = " $Column < '$End'"
    if (-not [string]::IsNullOrWhiteSpace($Start)) {
        $clause += " AND $Column >= '$Start'"
    }
    return $clause
}

function Get-BucketStorageBounds([string]$Label, [bool]$Weekly, [bool]$FirstBucket) {
    $period = [DateTime]::ParseExact(
        $Label,
        "yyyy-MM-dd",
        [Globalization.CultureInfo]::InvariantCulture
    )
    $bucketStart = $period
    if ($Weekly -and $FirstBucket) {
        if ($Range -eq "all") {
            $firstRow = (Invoke-DashboardSql @"
SELECT MIN(event_time) FROM (
    SELECT created_at AS event_time FROM questions WHERE profile_id='$profileSqlId' AND created_at < '$endStorage'
    UNION ALL
    SELECT end_time AS event_time FROM quiz_attempts WHERE profile_id='$profileSqlId' AND end_time IS NOT NULL AND end_time < '$endStorage'
) activity
"@)[0]
            if ($firstRow -ne "NULL" -and $firstRow -ne "") {
                $storageDate = [DateTime]::Parse($firstRow, [Globalization.CultureInfo]::InvariantCulture)
                $firstInstant = [TimeZoneInfo]::ConvertTimeToUtc(
                    [DateTime]::SpecifyKind($storageDate, [DateTimeKind]::Unspecified),
                    $storageZone
                )
                $bucketStart = [TimeZoneInfo]::ConvertTimeFromUtc($firstInstant, $requestedZone).Date
            }
        }
        elseif ($null -ne $startLocalDate) {
            $bucketStart = $startLocalDate
        }
    }
    $bucketEnd = if ($Weekly) { $period.AddDays(7) } else { $period.AddDays(1) }
    if ($bucketEnd -gt $endLocalDate) { $bucketEnd = $endLocalDate }
    return @(
        (Convert-ToStorageDateTime $bucketStart $requestedZone $storageZone),
        (Convert-ToStorageDateTime $bucketEnd $requestedZone $storageZone)
    )
}

function Get-RoundedAccuracy([long]$Correct, [long]$Graded) {
    if ($Graded -eq 0) { return 0.0 }
    return [Math]::Floor(($Correct * 1000.0 / $Graded) + 0.5) / 10.0
}

try {
    $uri = "$($ApiBaseUrl.TrimEnd('/'))/dashboard/metrics?range=$Range&tz=$([Uri]::EscapeDataString($TimeZone))"
    $metrics = Invoke-RestMethod -Method Get -Uri $uri -Headers @{ Authorization = "Bearer $accessToken" }
    $endInstant = [DateTimeOffset]::Parse($metrics.windowEnd).ToUniversalTime()
    $endStorageDate = [TimeZoneInfo]::ConvertTime($endInstant, $storageZone).DateTime
    $endStorage = $endStorageDate.ToString("yyyy-MM-dd HH:mm:ss.ffffff", [Globalization.CultureInfo]::InvariantCulture)
    $startStorage = ""
    $startLocalDate = $null
    if ($metrics.windowStart) {
        $startInstant = [DateTimeOffset]::Parse($metrics.windowStart).ToUniversalTime()
        $startStorageDate = [TimeZoneInfo]::ConvertTime($startInstant, $storageZone).DateTime
        $startStorage = $startStorageDate.ToString("yyyy-MM-dd HH:mm:ss.ffffff", [Globalization.CultureInfo]::InvariantCulture)
        $startLocalDate = [TimeZoneInfo]::ConvertTime($startInstant, $requestedZone).DateTime.Date
    }
    $endLocalDate = [TimeZoneInfo]::ConvertTime($endInstant, $requestedZone).DateTime.Date
    $profileSqlId = $ProfileId

    $qWindow = Get-DateWindowClause "q.created_at" $startStorage $endStorage
    $aWindow = Get-DateWindowClause "a.end_time" $startStorage $endStorage
    $totalQuestions = [long](Invoke-DashboardSql "SELECT COUNT(*) FROM questions q WHERE q.profile_id='$profileSqlId' AND$qWindow")[0]
    Assert-Same "total questions" $metrics.totalQuestionsAsked $totalQuestions

    $attempts = [long](Invoke-DashboardSql "SELECT COUNT(*) FROM quiz_attempts a WHERE a.profile_id='$profileSqlId' AND a.end_time IS NOT NULL AND$aWindow")[0]
    Assert-Same "completed attempts" $metrics.completedQuizAttempts $attempts

    $accuracyRow = (Invoke-DashboardSql @"
SELECT COALESCE(SUM(CASE WHEN r.is_correct=1 THEN 1 ELSE 0 END),0), COUNT(r.response_id)
FROM quiz_attempts a JOIN quiz_responses r ON r.attempt_id=a.attempt_id
WHERE a.profile_id='$profileSqlId' AND a.end_time IS NOT NULL AND$aWindow
"@)[0] -split "`t"
    $graded = [long]$accuracyRow[1]
    $accuracy = Get-RoundedAccuracy ([long]$accuracyRow[0]) $graded
    Assert-Same "weighted quiz accuracy" $metrics.averageQuizAccuracy $accuracy
    Assert-Same "legacy overall accuracy" $metrics.overallAccuracyRate $accuracy

    $sqlSubjectRows = @(Invoke-DashboardSql "SELECT q.subject, COUNT(*) FROM questions q WHERE q.profile_id='$profileSqlId' AND$qWindow GROUP BY q.subject ORDER BY COUNT(*) DESC, q.subject ASC")
    $sqlSubjects = @{}
    foreach ($row in $sqlSubjectRows) {
        $parts = $row -split "`t", 2
        $sqlSubjects[$parts[0]] = [long]$parts[1]
    }
    $apiSubjects = @($metrics.questionsPerSubject)
    $apiTop = @($apiSubjects | Where-Object { $_.subject -ne "Other" })
    $topCount = [Math]::Min($TopSubjects, $apiTop.Count)
    $expectedTop = @($sqlSubjectRows | Select-Object -First $topCount)
    if ($expectedTop.Count -ne $apiTop.Count) { throw "Questions by subject row count mismatch." }
    $topIncluded = [long]0
    for ($index = 0; $index -lt $expectedTop.Count; $index++) {
        $expected = $expectedTop[$index] -split "`t", 2
        Assert-Same "subject $($expected[0])" $expected[1] $apiTop[$index].questionCount
        $topIncluded += [long]$expected[1]
    }
    $other = [long]0
    foreach ($value in $sqlSubjects.Values) { $other += [long]$value }
    $other -= $topIncluded
    $apiOther = $apiSubjects | Where-Object { $_.subject -eq "Other" } | Select-Object -First 1
    Assert-Same "other subject questions" $other $(if ($apiOther) { $apiOther.questionCount } else { 0 })

    $apiAccuracyRows = @{}
    foreach ($item in $metrics.accuracyPerSubject) { $apiAccuracyRows[$item.subject] = $item }
    $sqlAccuracyRows = @(Invoke-DashboardSql @"
SELECT q.subject, SUM(CASE WHEN r.is_correct=1 THEN 1 ELSE 0 END), COUNT(r.response_id)
FROM quiz_attempts a
JOIN practice_quizzes q ON q.quiz_id=a.quiz_id
JOIN quiz_responses r ON r.attempt_id=a.attempt_id
WHERE a.profile_id='$profileSqlId' AND a.end_time IS NOT NULL AND$aWindow
GROUP BY q.subject ORDER BY q.subject ASC
"@)
    foreach ($row in $sqlAccuracyRows) {
        $parts = $row -split "`t"
        $count = [long]$parts[2]
        $value = Get-RoundedAccuracy ([long]$parts[1]) $count
        Assert-Same "accuracy for $($parts[0])" $value $apiAccuracyRows[$parts[0]].accuracyPercent
    }
    Assert-Same "accuracy-by-subject row count" $sqlAccuracyRows.Count $apiAccuracyRows.Count

    $index = 0
    foreach ($point in $metrics.questionsTrend) {
        $bounds = Get-BucketStorageBounds $point.periodStart ($Range -eq "all") ($index -eq 0)
        $bucketClause = Get-DateWindowClause "q.created_at" $bounds[0] $bounds[1]
        $count = [long](Invoke-DashboardSql "SELECT COUNT(*) FROM questions q WHERE q.profile_id='$profileSqlId' AND$bucketClause")[0]
        Assert-Same "question trend $($point.periodStart)" $point.count $count
        $index++
    }

    $index = 0
    foreach ($point in $metrics.accuracyTrend) {
        $bounds = Get-BucketStorageBounds $point.periodStart $true ($index -eq 0)
        $bucketClause = Get-DateWindowClause "a.end_time" $bounds[0] $bounds[1]
        $bucketRow = (Invoke-DashboardSql @"
SELECT COALESCE(SUM(CASE WHEN r.is_correct=1 THEN 1 ELSE 0 END),0), COUNT(r.response_id)
FROM quiz_attempts a JOIN quiz_responses r ON r.attempt_id=a.attempt_id
WHERE a.profile_id='$profileSqlId' AND a.end_time IS NOT NULL AND$bucketClause
"@)[0] -split "`t"
        $bucketAccuracy = Get-RoundedAccuracy ([long]$bucketRow[0]) ([long]$bucketRow[1])
        Assert-Same "accuracy trend $($point.periodStart) graded count" $point.gradedCount ([long]$bucketRow[1])
        Assert-Same "accuracy trend $($point.periodStart)" $point.accuracyPercent $bucketAccuracy
        $index++
    }

    $weakStartLocal = $endLocalDate.AddDays(-30)
    $weakStart = Convert-ToStorageDateTime $weakStartLocal $requestedZone $storageZone
    $weakClause = Get-DateWindowClause "a.end_time" $weakStart $endStorage
    $sqlWeakRows = @(Invoke-DashboardSql @"
SELECT q.subject, qq.topic, SUM(CASE WHEN r.is_correct=1 THEN 1 ELSE 0 END), COUNT(r.response_id)
FROM quiz_attempts a
JOIN practice_quizzes q ON q.quiz_id=a.quiz_id
JOIN quiz_responses r ON r.attempt_id=a.attempt_id
JOIN quiz_questions qq ON qq.question_id=r.question_id
WHERE a.profile_id='$profileSqlId' AND a.end_time IS NOT NULL AND$weakClause
  AND qq.topic IS NOT NULL AND TRIM(qq.topic)<>''
GROUP BY q.subject, qq.topic
HAVING COUNT(r.response_id)>=$WeakTopicMinimum
  AND SUM(CASE WHEN r.is_correct=1 THEN 1 ELSE 0 END)*100.0/COUNT(r.response_id)<$WeakTopicMaximumAccuracy
ORDER BY SUM(CASE WHEN r.is_correct=1 THEN 1 ELSE 0 END)*1.0/COUNT(r.response_id) ASC,
         COUNT(r.response_id) DESC, q.subject ASC, qq.topic ASC
"@)
    $apiWeakRows = @($metrics.weakTopics)
    Assert-Same "weak-topic row count" $sqlWeakRows.Count $apiWeakRows.Count
    for ($index = 0; $index -lt $sqlWeakRows.Count; $index++) {
        $parts = $sqlWeakRows[$index] -split "`t"
        Assert-Same "weak topic $($parts[0])/$($parts[1])" "$($parts[2])/$($parts[3])" `
            "$($apiWeakRows[$index].correctCount)/$($apiWeakRows[$index].gradedCount)"
    }

    $activitySql = @"
SELECT event_id, event_type FROM (
    (SELECT q.question_id AS event_id, 'QUESTION_CREATED' AS event_type, q.created_at AS occurred_at
     FROM questions q WHERE q.profile_id='$profileSqlId' AND$qWindow ORDER BY q.created_at DESC LIMIT 10)
    UNION ALL
    (SELECT a.attempt_id AS event_id, 'QUIZ_COMPLETED' AS event_type, a.end_time AS occurred_at
     FROM quiz_attempts a WHERE a.profile_id='$profileSqlId' AND a.end_time IS NOT NULL AND$aWindow
     ORDER BY a.end_time DESC LIMIT 10)
) recent
ORDER BY occurred_at DESC, FIELD(event_type, 'QUESTION_CREATED', 'QUIZ_COMPLETED') LIMIT 10
"@
    $apiActivity = @($metrics.recentActivity | ForEach-Object { "$($_.id)|$($_.type)" })
    $sqlActivity = @(Invoke-DashboardSql $activitySql)
    $expectedActivity = @($sqlActivity | ForEach-Object { ($_ -split "`t")[0..1] -join "|" })
    Assert-Same "recent activity IDs and order" ($expectedActivity -join ",") ($apiActivity -join ",")
    Write-Host "PASS recent activity"

    Write-Host "Dashboard metrics match independent SQL checks."
}
finally {
    Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
}
