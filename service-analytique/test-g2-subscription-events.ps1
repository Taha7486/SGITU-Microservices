# SGITU Subscription -> G8 integration test.
# Prerequisites:
#   - G8 + Kafka + G3 (user-service + g3-users-db) + abonnement-service + db-abonnement
#   - Root .env: G3_BASE_URL=http://g3-user-service:8083
#
# G2 analytics path:
#   souscrire -> AnalytiqueTrace in G2 MySQL -> AnalyseClient POST /api/events/batch -> G8
# Kafka topic abonnement.souscription is G5 notification-shaped and only fires on confirmation.

param([int]$TimeoutSeconds = 120)

. (Join-Path $PSScriptRoot "test-sender-common.ps1")

$repoRoot = Resolve-RepoRoot
Set-Location $repoRoot
$g8Headers = Get-G8AuthHeaders $repoRoot
$runId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$baseUrl = "http://localhost:8082"
$notificationTopic = "abonnement.souscription"

$envValues = Read-DotEnv (Join-Path $repoRoot ".env")
$secret = $envValues["JWT_SECRET"]
if (-not $secret) { $secret = "sgitu_g8_secret_key_2025_very_long_secret_for_analytics" }

$g2AdminToken = New-Hs256Jwt -Secret $secret -Subject "g2-admin-$runId" -Roles @("ROLE_ADMIN_G2")
$g2AdminHeaders = @{ Authorization = "Bearer $g2AdminToken"; "Content-Type" = "application/json" }

Write-Host "SGITU Subscription -> G8 integration test" -ForegroundColor Yellow
Write-Host "Requires G8, G3, Kafka, and abonnement-service."

Write-Step "Service readiness"
$checks = [ordered]@{
    "sgitu-kafka" = "Kafka"
    "g8-mongo" = "G8 Mongo"
    "g8-analytics-service" = "G8 Analytics"
    "g3-users-db" = "G3 database"
    "g3-user-service" = "G3 user-service"
    "db-abonnement" = "Subscription MySQL"
    "service-abonnement" = "Subscription service"
}
foreach ($container in $checks.Keys) {
    Add-Result "$($checks[$container]) container is ready" (Wait-ContainerHealthy -ContainerName $container -Timeout $TimeoutSeconds) $container
}
Add-Result "G8 health endpoint is reachable" (Wait-HttpOk -Url "http://localhost:8088/actuator/health" -Timeout $TimeoutSeconds)

$g3BaseUrl = $envValues["G3_BASE_URL"]
if ($g3BaseUrl -and $g3BaseUrl -notmatch "g3-user-service") {
    Write-Host "[WARN] G3_BASE_URL=$g3BaseUrl - expected http://g3-user-service:8083 for root compose." -ForegroundColor Yellow
}

Write-Step "Prepare a real G3 passenger"
$userId = $null
$userEmail = $null
try {
    $g3User = New-G3VerifiedPassenger -RunId $runId -TimeoutSeconds $TimeoutSeconds
    $userId = $g3User.UserId
    $userEmail = $g3User.Email
    Add-Result "G3 verified passenger is ready" $true "userId=$userId email=$userEmail"
} catch {
    Add-Result "G3 verified passenger is ready" $false $_.Exception.Message
}

Write-Step "Trigger a real subscription action"
$subscriptionId = $null
$planId = $null
if ($userId) {
    try {
        $planBody = @{
            nomPlan = "G8 Stage4 Plan $runId"
            description = "Plan used by G8 Stage 4 sender integration tests"
            prix = 35.0
            duree = "MENSUEL"
            categorie = "ROLE_PASSENGER"
            transportType = "BUS"
            estActif = "ACTIF"
            maxDesactivation = 1
            minJoursEntreDesactivation = 1
            maxPeriodeDesactivation = 3
        } | ConvertTo-Json -Depth 10 -Compress

        $plan = Invoke-RestMethod -Uri "$baseUrl/plans" -Method Post -Headers $g2AdminHeaders -Body $planBody -TimeoutSec 30
        $planId = "$($plan.idPlan)"
        Add-Result "Subscription plan creation succeeds" (-not [string]::IsNullOrWhiteSpace($planId)) ($plan | ConvertTo-Json -Compress -Depth 8)
    } catch {
        Add-Result "Subscription plan creation succeeds" $false $_.Exception.Message
    }
}

if ($planId -and $userId) {
    $passengerToken = New-Hs256Jwt -Secret $secret -Subject $userEmail -Email $userEmail -Roles @("ROLE_PASSENGER")
    $passengerHeaders = @{ Authorization = "Bearer $passengerToken"; "Content-Type" = "application/json" }
    try {
        $souscrireUrl = "$baseUrl/abonnements/souscrire?userId=$userId" + "&planId=$planId"
        $created = Invoke-RestMethod -Uri $souscrireUrl -Method Post -Headers $passengerHeaders -TimeoutSec 45
        $subscriptionId = "$($created.id)"
        Add-Result "Subscription creation succeeds" (-not [string]::IsNullOrWhiteSpace($subscriptionId)) ($created | ConvertTo-Json -Compress -Depth 8)
    } catch {
        Add-Result "Subscription creation succeeds" $false $_.Exception.Message
    }
}

Write-Step "Verify G2 analytics trace"
$traceJson = $null
if ($userId) {
    try {
        $dbUser = $envValues["abonnement_DB_USER"]
        if (-not $dbUser) { $dbUser = "abonnement" }
        $dbPass = $envValues["abonnement_DB_PASSWORD"]
        if (-not $dbPass) { $dbPass = "abonnement" }
        $dbName = $envValues["abonnement_DB_NAME"]
        if (-not $dbName) { $dbName = "abonnement_db" }

        $sql = "SELECT id,user_id,action,plan_type,timestamp FROM analytique_trace WHERE user_id='USR-$userId' OR user_id='$userId' ORDER BY id DESC LIMIT 1;"
        $oldPreference = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        try {
            $traceJson = docker exec -e "MYSQL_PWD=$dbPass" db-abonnement mysql -N -u $dbUser $dbName -e $sql 2>$null
        } finally {
            $ErrorActionPreference = $oldPreference
        }
        if ($traceJson -is [System.Array]) {
            $traceJson = ($traceJson | Where-Object { $_ -and $_ -notmatch '^\s*$' } | Select-Object -First 1)
        }
        $traceJson = ("$traceJson").Trim()
        $traceCollected = -not [string]::IsNullOrWhiteSpace($traceJson)
        Add-Result "G2 collected AnalytiqueTrace for the user" $traceCollected $traceJson
    } catch {
        Add-Result "G2 collected AnalytiqueTrace for the user" $false $_.Exception.Message
    }
}

Write-Step "Relay G2 batch contract to G8"
$batchRelayed = $false
if ($traceJson) {
    try {
        $parts = ($traceJson -split "`t")
        if ($parts.Count -ge 5) {
            $eventObj = @{
                timestamp = $parts[4]
                userId = $parts[1]
                action = $parts[2]
                planType = $parts[3]
            }
            # PowerShell ConvertTo-Json unwraps single-element arrays; G8 expects a JSON array.
            $batchBody = '[' + (ConvertTo-Json -InputObject $eventObj -Compress -Depth 5) + ']'

            $batchResponse = Invoke-RestMethod -Uri "http://localhost:8088/api/events/batch" -Method Post -Body $batchBody -ContentType "application/json" -TimeoutSec 30
            $batchRelayed = ($batchResponse.status -eq "SUCCESS" -or $batchResponse.totalAccepted -gt 0)
            Add-Result "G8 accepts G2 /api/events/batch payload" $batchRelayed ($batchResponse | ConvertTo-Json -Compress -Depth 5)
        } else {
            Add-Result "G8 accepts G2 /api/events/batch payload" $false "Could not parse AnalytiqueTrace row: $traceJson"
        }
    } catch {
        Add-Result "G8 accepts G2 /api/events/batch payload" $false $_.Exception.Message
    }
} else {
    Add-Result "G8 accepts G2 /api/events/batch payload" $false "No AnalytiqueTrace row to relay"
}

Write-Step "Verify G8 persistence and optional Kafka notification topic"
$g8Stored = $false
if ($userId) {
    $mongoEval = "db.incoming_events.countDocuments({sourceType:'SUBSCRIPTION', `$or:[{'payload.userId':'$userId'},{'payload.userId':'USR-$userId'}]})"
    $g8Stored = Wait-MongoCondition $mongoEval { param($value) [int]$value -gt 0 } 45
    Add-Result "G8 stored the subscription event" $g8Stored "userId=$userId subscriptionId=$subscriptionId"
}

$notificationKafka = Read-KafkaTopicQuiet -Topic $notificationTopic -MaxMessages 80 -TimeoutMs 6000
$notificationPublished = $false
if ($subscriptionId -and $notificationKafka.Text -match [regex]::Escape($subscriptionId)) {
    $notificationPublished = $true
} elseif ($userId -and ($notificationKafka.Text -split "`r?`n" | Where-Object { $_ -match "`"userId`"\s*:\s*`"$userId`"" })) {
    $notificationPublished = $true
}
Add-Result "G2 notification topic has confirmation event (optional)" $notificationPublished "topic=$notificationTopic - only expected after payment confirmation, not initial souscription"

Write-Step "Run G8 analytics"
try {
    Invoke-G8Run -Headers $g8Headers
    Add-Result "Manual analytics job completed" $true
} catch {
    Add-Result "Manual analytics job completed" $false $_.Exception.Message
}
$subExists = Wait-MongoCondition "db.stat_snapshots.countDocuments({statId:'SUB_NEW'})" { param($value) [int]$value -gt 0 } 45
Add-Result "Subscription impacted SUB_NEW snapshot" $subExists

Write-Step "Diagnosis"
if (-not $userId) {
    Write-Host "[DIAGNOSIS] Start G3 and verify G3_BASE_URL=http://g3-user-service:8083 in root .env." -ForegroundColor Yellow
} elseif (-not $subscriptionId) {
    Write-Host "[DIAGNOSIS] Souscription failed. Use a ROLE_PASSENGER JWT with email claim and a verified G3 user." -ForegroundColor Yellow
} elseif (-not $traceJson) {
    Write-Host "[DIAGNOSIS] G2 did not collect AnalytiqueTrace. Check service-abonnement logs for G3/G6 dependency errors." -ForegroundColor Yellow
} elseif (-not $g8Stored) {
    Write-Host "[DIAGNOSIS] G2 trace exists but G8 did not persist. Rebuild g8-analytics-service so /api/events/batch is available." -ForegroundColor Yellow
} else {
    Write-Host "[OK] G2 trace -> G8 batch contract -> G8 Mongo persistence works." -ForegroundColor Green
}

Complete-Script -UsefulLogs @(
    "docker logs service-abonnement --tail=150",
    "docker logs g3-user-service --tail=80",
    "docker logs g8-analytics-service --tail=150",
    "docker exec sgitu-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group g8-analytics-group"
)
