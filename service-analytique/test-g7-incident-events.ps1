# SGITU Incident Management (G9) -> G8 integration test.
# Prerequisites in root compose:
#   kafka, g8-analytics-service, g9-service (service-gestion-incidents), db-g9
#
# G9 publishes to incident.analytique.topic when an incident is annule or cloture,
# not on the initial signalement. This script signals then cancels to trigger G8.

param([int]$TimeoutSeconds = 120)

. (Join-Path $PSScriptRoot "test-sender-common.ps1")

$repoRoot = Resolve-RepoRoot
Set-Location $repoRoot
$g8Headers = Get-G8AuthHeaders $repoRoot
$envValues = Read-DotEnv (Join-Path $repoRoot ".env")
$secret = $envValues["JWT_SECRET"]
if (-not $secret) {
    $secret = "sgitu_g8_secret_key_2025_very_long_secret_for_analytics"
}

$runId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$baseUrl = "http://localhost:8089/api/incidents"
$topic = "incident.analytique.topic"
$probeText = "G8 Stage 4 analytics incident probe $runId"

Write-Host "SGITU Incident Management -> G8 integration test" -ForegroundColor Yellow
Write-Host "Requires root compose G8 + G9 (service-gestion-incidents on localhost:8089)."

Write-Step "Service readiness"
$checks = [ordered]@{
    "sgitu-kafka" = "Kafka"
    "g8-mongo" = "G8 Mongo"
    "g8-analytics-service" = "G8 Analytics"
    "service-gestion-incidents" = "G9 incident service"
}
foreach ($container in $checks.Keys) {
    Add-Result "$($checks[$container]) container is ready" (Wait-ContainerHealthy -ContainerName $container -Timeout $TimeoutSeconds) $container
}
Add-Result "G8 health endpoint is reachable" (Wait-HttpOk -Url "http://localhost:8088/actuator/health" -Timeout $TimeoutSeconds)
Add-Result "G9 health endpoint is reachable" (Wait-HttpOk -Url "http://localhost:8089/api/incidents/actuator/health" -Timeout $TimeoutSeconds)

$passengerHeaders = @{
    Authorization = "Bearer $(New-Hs256Jwt -Secret $secret -Subject "g8-incident-passenger-$runId" -Roles @("ROLE_PASSENGER"))"
    "X-User-Id" = "9001"
    "X-User-Role" = "ROLE_PASSENGER"
}
$supervisorHeaders = @{
    Authorization = "Bearer $(New-Hs256Jwt -Secret $secret -Subject "g8-incident-supervisor-$runId" -Roles @("ROLE_SUPERVISOR"))"
    "X-User-Id" = "9002"
    "X-User-Role" = "ROLE_SUPERVISOR"
}

Write-Step "Trigger a real incident action"
$incidentId = $null
$incidentReference = $null
try {
    $incidentBody = @{
        type = "ACCIDENT"
        description = $probeText
        dateIncident = ([DateTime]::UtcNow.ToString("yyyy-MM-ddTHH:mm:ss"))
        latitude = 33.5731
        longitude = -7.5898
        vehiculeId = "VEH-G8-$runId"
        ligneTransport = "L-STAGE4"
        preuves = @()
    } | ConvertTo-Json -Depth 10 -Compress

    $incident = Invoke-RestMethod -Uri "$baseUrl/signaler" -Method Post -ContentType "application/json" -Headers $passengerHeaders -Body $incidentBody -TimeoutSec 45
    $incidentId = "$($incident.incidentId)"
    $incidentReference = "$($incident.reference)"
    Add-Result "Incident signalement endpoint was called" (-not [string]::IsNullOrWhiteSpace($incidentId)) ($incident | ConvertTo-Json -Compress -Depth 8)
} catch {
    Add-Result "Incident signalement endpoint was called" $false $_.Exception.Message
}

Write-Step "Close the incident path to trigger G8 analytique publish"
$g9Published = $false
if ($incidentId) {
    try {
        $cancelBody = @{ motif = "G8 integration test cancellation $runId" } | ConvertTo-Json -Compress
        Invoke-RestMethod -Uri "$baseUrl/$incidentId/annuler" -Method Put -ContentType "application/json" -Headers $supervisorHeaders -Body $cancelBody -TimeoutSec 45 | Out-Null
        Add-Result "Incident cancellation triggers G9 analytique publish" $true "incidentId=$incidentId reference=$incidentReference"
        $g9Published = $true
    } catch {
        Add-Result "Incident cancellation triggers G9 analytique publish" $false $_.Exception.Message
    }
} else {
    Add-Result "Incident cancellation triggers G9 analytique publish" $false "No incidentId from signalement"
}

Write-Step "Verify Incident -> Kafka -> G8"
$rawKafka = Read-KafkaTopicQuiet -Topic $topic -MaxMessages 120 -TimeoutMs 10000
$eventPublished = $false
if ($incidentReference -and $rawKafka.Text -match [regex]::Escape($incidentReference)) {
    $eventPublished = $true
} elseif ($rawKafka.Text -match [regex]::Escape($probeText)) {
    $eventPublished = $true
} elseif ($incidentId -and $rawKafka.Text -match [regex]::Escape($incidentId)) {
    $eventPublished = $true
}
Add-Result "Incident service published to $topic" $eventPublished "incidentId=$incidentId reference=$incidentReference"

$mongoEval = "db.incoming_events.countDocuments({sourceType:'INCIDENT', `$or:[{'payload.reference':'$incidentReference'},{'payload.incidentId':'$incidentReference'},{'payload.description':/$([regex]::Escape($probeText))/}]})"
$g8Stored = $false
if ($incidentReference -or $probeText) {
    $g8Stored = Wait-MongoCondition $mongoEval { param($value) [int]$value -gt 0 } 75
}
Add-Result "G8 stored the incident event" $g8Stored "incidentId=$incidentId reference=$incidentReference"

Write-Step "Run G8 analytics"
try {
    Invoke-G8Run -Headers $g8Headers
    Add-Result "Manual analytics job completed" $true
} catch {
    Add-Result "Manual analytics job completed" $false $_.Exception.Message
}
$incExists = Wait-MongoCondition "db.stat_snapshots.countDocuments({statId:'INC_TOTAL'})" { param($value) [int]$value -gt 0 } 45
Add-Result "Incident impacted INC_TOTAL snapshot" $incExists

try {
    $prometheus = (Invoke-WebRequest -Uri "http://localhost:8088/actuator/prometheus" -Headers $g8Headers -UseBasicParsing -TimeoutSec 15).Content
    if ($prometheus -match 'sgitu_alerts_triggered_total\{alert_type="INCIDENT_ZONE_RISK"') {
        Write-Host "[INFO] INCIDENT_ZONE_RISK counter is present in Prometheus output." -ForegroundColor Green
    } else {
        Write-Host "[INFO] No INCIDENT_ZONE_RISK counter observed. That is acceptable unless this run intentionally created a repeated incident zone." -ForegroundColor Yellow
    }
} catch {
    Write-Host "[INFO] Could not read Prometheus alert counters: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Step "Diagnosis"
if (-not $g9Published) {
    Write-Host "[DIAGNOSIS] G9 signalement/cancellation failed. Start g9-service + db-g9 and verify JWT_SECRET matches the container." -ForegroundColor Yellow
} elseif (-not $eventPublished) {
    Write-Host "[DIAGNOSIS] G9 cancelled the incident but nothing appeared on $topic. Check service-gestion-incidents logs and KAFKA_BOOTSTRAP_SERVERS=kafka:9092." -ForegroundColor Yellow
} elseif (-not $g8Stored) {
    Write-Host "[DIAGNOSIS] G9 published, but G8 did not persist. Check G8 incident consumer logs and payload field mapping (reference/description)." -ForegroundColor Yellow
} else {
    Write-Host "[OK] G9 cancellation published analytique data and G8 persisted the event." -ForegroundColor Green
}

Complete-Script -UsefulLogs @(
    "docker logs service-gestion-incidents --tail=150",
    "docker logs g8-analytics-service --tail=150",
    "docker exec sgitu-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic incident.analytique.topic --from-beginning --max-messages 5 --timeout-ms 8000",
    "docker exec sgitu-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group g8-analytics-group"
)
