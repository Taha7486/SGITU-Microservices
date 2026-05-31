# ==============================================================================
# SGITU-Microservices - service-analytique A-to-Z Integration Testing Harness
# ==============================================================================
# Description: Automates A-to-Z containerized testing for the analytics service.
# Requirements: Docker containers must be running (`docker compose up -d`).
# Usage: powershell -ExecutionPolicy Bypass -File .\run-integration-tests.ps1
# ==============================================================================

# Ensure UTF-8 console output for beautiful emojis and formatting
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Clear-Host
Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host "   A-TO-Z CONTAINERIZED INTEGRATION TESTING HARNESS" -ForegroundColor Yellow
Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host "This script will test all endpoints, schema enforcement, Kafka queues," -ForegroundColor Gray
Write-Host "ETL processing, and ML integrations entirely within active containers." -ForegroundColor Gray
Write-Host "------------------------------------------------------------------------" -ForegroundColor Cyan

# --- GLOBAL CONFIGURATION ---
$BaseUrl = "http://localhost:8088"
$JwtSecret = "sgitu_g8_secret_key_2025_very_long_secret_for_analytics"
$TestTimeoutSeconds = 60
$ScoreCard = @{
    Total = 0
    Pass  = 0
    Fail  = 0
}

# --- CRYPTOGRAPHIC JWT GENERATOR (Native PowerShell HS256 Implementation) ---
function New-MockJwtToken {
    param (
        [string]$Subject = "integration-tester",
        [string[]]$Roles = @("ADMIN")
    )
    # Header (Base64UrlEncoded)
    $Header = @{ alg = "HS256"; typ = "JWT" }
    $HeaderJson = $Header | ConvertTo-Json -Compress
    $HeaderBase64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($HeaderJson)).Split('=')[0].Replace('+', '-').Replace('/', '_')

    # Payload (Base64UrlEncoded)
    $Exp = [DateTimeOffset]::UtcNow.AddHours(1).ToUnixTimeSeconds()
    $Payload = @{
        sub = $Subject
        roles = $Roles
        exp = $Exp
    }
    $PayloadJson = $Payload | ConvertTo-Json -Compress
    $PayloadBase64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($PayloadJson)).Split('=')[0].Replace('+', '-').Replace('/', '_')

    # Signature
    $SignatureInput = "$HeaderBase64.$PayloadBase64"
    $Hmac = New-Object System.Security.Cryptography.HMACSHA256
    $Hmac.Key = [Text.Encoding]::UTF8.GetBytes($JwtSecret)
    $SignatureBytes = $Hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($SignatureInput))
    $SignatureBase64 = [Convert]::ToBase64String($SignatureBytes).Split('=')[0].Replace('+', '-').Replace('/', '_')

    return "$HeaderBase64.$PayloadBase64.$SignatureBase64"
}

# --- TEST UTILITY FUNCTIONS ---
function Register-TestResult {
    param (
        [string]$Name,
        [bool]$Success,
        [string]$Details = ""
    )
    $ScoreCard.Total++
    if ($Success) {
        $ScoreCard.Pass++
        Write-Host "  [PASS] " -NoNewline -ForegroundColor Green
        Write-Host $Name -ForegroundColor White
    } else {
        $ScoreCard.Fail++
        Write-Host "  [FAIL] " -NoNewline -ForegroundColor Red
        Write-Host "$Name - $Details" -ForegroundColor Yellow
    }
}

# --- STEP 1: POLLING APP HEALTH (Phase 3 Intro) ---
Write-Host "[*] Phase 1 - Service Availability check..." -ForegroundColor Cyan
$AppReady = $false
$StartTime = Get-Date

Write-Host "Waiting for g8-analytics-service to be healthy at $BaseUrl/actuator/health..." -ForegroundColor Gray
while (((Get-Date) - $StartTime).TotalSeconds -lt $TestTimeoutSeconds) {
    try {
        $Health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -Method Get -TimeoutSec 2 -ErrorAction Stop
        if ($Health.status -eq "UP") {
            $AppReady = $true
            break
        }
    } catch {
        # Silent retry
    }
    Write-Host "." -NoNewline -ForegroundColor Yellow
    Start-Sleep -Seconds 2
}
Write-Host ""

if (-not $AppReady) {
    Write-Host "ERROR: The g8-analytics-service failed to start or is unhealthy!" -ForegroundColor Red
    Write-Host "Please ensure you have started the containers using: 'docker compose up -d'" -ForegroundColor Yellow
    Exit 1
}
Register-TestResult "Spring Actuator Health Endpoint is UP" $true

# --- STEP 2: VERIFY SECURITY LAYER (Phase 3) ---
Write-Host "`n[*] Phase 2 - JWT Security Filter Verification..." -ForegroundColor Cyan

# Test 2.1: Unauthenticated request should be rejected (401)
$UnauthSuccess = $false
$Details = ""
try {
    $res = Invoke-WebRequest -Uri "$BaseUrl/api/v1/analytics/dashboard" -Method Get -ErrorAction Stop
} catch {
    $StatusCode = $_.Exception.Response.StatusCode.value__
    if ($StatusCode -eq 401) {
        $UnauthSuccess = $true
    } else {
        $Details = "Expected status 401, but got $StatusCode"
    }
}
Register-TestResult "Unauthenticated requests are blocked (401 Unauthorized)" $UnauthSuccess $Details

# Test 2.2: Generate Administration JWT Token
Write-Host "Generating local cryptographic admin JWT token signed with development secret..." -ForegroundColor Gray
$JwtToken = New-MockJwtToken -Subject "admin-agent" -Roles @("ADMIN")

# Test 2.3: Authenticated Request should succeed (200)
$AuthSuccess = $false
$Details = ""
try {
    $Headers = @{ "Authorization" = "Bearer $JwtToken" }
    $res = Invoke-WebRequest -Uri "$BaseUrl/api/v1/analytics/dashboard" -Method Get -Headers $Headers -ErrorAction Stop
    if ($res.StatusCode -eq 200) {
        $AuthSuccess = $true
    }
} catch {
    $Details = $_.Exception.Message
}
Register-TestResult "Authenticated requests are accepted with JWT (200 OK)" $AuthSuccess $Details


# --- STEP 3: DATA INGESTION & DEFENSIVE SCHEMA VALIDATION (Phase 4) ---
Write-Host "`n[*] Phase 3 - Ingestion Layer & Defensive Validation..." -ForegroundColor Cyan

$DynamicTimestamp = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")

# Test 3.1: Valid Ingestion of Ticketing Events (version 1)
$IngestTicketSuccess = $false
$Details = ""
try {
    $Headers = @{ 
        "Authorization" = "Bearer $JwtToken"
        "Content-Type" = "application/json"
    }
    $TicketPayload = '[{"schemaVersion":1,"timestamp":"' + $DynamicTimestamp + '","userId":"user-abc-123","status":"validated","line":"L1","stationId":"ST-101"}]'
    
    $res = Invoke-RestMethod -Uri "$BaseUrl/api/v1/ingestion/tickets" -Method Post -Headers $Headers -Body $TicketPayload -ErrorAction Stop
    if ($res.status -eq "SUCCESS" -and $res.totalAccepted -eq 1) {
        $IngestTicketSuccess = $true
    } else {
        $Details = "Response indicated failure: $($res | ConvertTo-Json -Compress)"
    }
} catch {
    $Details = $_.Exception.Message
}
Register-TestResult "Ingest valid Ticketing event (schemaVersion: 1)" $IngestTicketSuccess $Details

# Test 3.2: Valid Ingestion of Payments Events (version 1)
$IngestPaymentSuccess = $false
$Details = ""
try {
    $PaymentPayload = '[{"schemaVersion":1,"timestamp":"' + $DynamicTimestamp + '","transactionId":"tx-pay-777","status":"completed","amount":25.50,"paymentMethod":"CARD","paymentType":"TICKET"}]'
    
    $res = Invoke-RestMethod -Uri "$BaseUrl/api/v1/ingestion/payments" -Method Post -Headers $Headers -Body $PaymentPayload -ErrorAction Stop
    if ($res.status -eq "SUCCESS" -and $res.totalAccepted -eq 1) {
        $IngestPaymentSuccess = $true
    }
} catch {
    $Details = $_.Exception.Message
}
Register-TestResult "Ingest valid Payment transaction event" $IngestPaymentSuccess $Details

# Test 3.3: Ingesting malformed event (missing schemaVersion) -> Should be rejected (400)
$DefensiveSuccess = $false
$Details = ""
try {
    $MalformedPayload = '[{"timestamp":"' + $DynamicTimestamp + '","userId":"user-broken","status":"validated"}]'
    
    $res = Invoke-WebRequest -Uri "$BaseUrl/api/v1/ingestion/tickets" -Method Post -Headers $Headers -Body $MalformedPayload -ErrorAction Stop
} catch {
    $StatusCode = $_.Exception.Response.StatusCode.value__
    if ($StatusCode -eq 400) {
        $DefensiveSuccess = $true
    } else {
        $Details = "Expected status 400, but got $StatusCode"
    }
}
Register-TestResult "Defensive Validation blocks schema-less payloads (400 Bad Request)" $DefensiveSuccess $Details


# --- STEP 4: ASYNC KAFKA QUEUES STREAMING (Phase 5) ---
Write-Host "`n[*] Phase 4 - Kafka Message Broker Integration..." -ForegroundColor Cyan

$KafkaSuccess = $false
$Details = ""

# Test 4.1: Stream payload directly into Kafka broker in the container
Write-Host "Streaming a mock ticketing JSON event directly into the dockerized Kafka container..." -ForegroundColor Gray
$KafkaPayload = "{`"schemaVersion`":1,`"timestamp`":`"$DynamicTimestamp`",`"userId`":`"kafka-agent-007`",`"status`":`"validated`",`"line`":`"L3`",`"stationId`":`"ST-303`"}"

try {
    $KafkaPayload | docker exec -i g8-kafka kafka-console-producer --bootstrap-server localhost:9092 --topic g2-ticketing-events 2>&1 | Out-Null
    Write-Host "Event piped. Waiting for asynchronous consumption and Mongo persistence..." -ForegroundColor Gray

    # Poll the logs of the analytics service for consumption verification
    Write-Host "Polling g8-analytics container logs to verify topic listener receipt..." -ForegroundColor Gray
    for ($i = 0; $i -lt 10; $i++) {
        $Logs = docker logs g8-analytics-service --tail=200 2>&1
        if ($Logs -match "Kafka \[TICKETING\]") {
            $KafkaSuccess = $true
            break
        }
        Start-Sleep -Seconds 2
    }
    
    if (-not $KafkaSuccess) {
        $Details = "Log trace did not contain 'Kafka [TICKETING]' after 20 seconds. Run 'docker logs g8-analytics-service' to inspect manually."
    }
} catch {
    $Details = $_.Exception.Message
}
Register-TestResult "Asynchronous Ingestion via Kafka Broker Queue" $KafkaSuccess $Details


# --- STEP 5: RUN ETL & ANALYTICS PIPELINES (Phase 6) ---
Write-Host "`n[*] Phase 5 - Analytical Aggregations & ML Predictions..." -ForegroundColor Cyan

# Test 5.1: Manually trigger Scheduled Analytics Aggregation Job
$JobSuccess = $false
$Details = ""
try {
    # Call TestController trigger
    $res = Invoke-RestMethod -Uri "$BaseUrl/test/run" -Method Get -Headers $Headers -ErrorAction Stop
    if ($res -match "Job") {
        $JobSuccess = $true
    } else {
        $Details = "Expected confirmation string, got: $res"
    }
} catch {
    $Details = $_.Exception.Message
}
Register-TestResult "Trigger scheduled aggregation job manually (/test/run)" $JobSuccess $Details

# Test 5.2: Query Compiled Database Snapshots
$SnapshotSuccess = $false
$Details = ""
$SnapshotsList = @()
try {
    $Headers = @{ "Authorization" = "Bearer $JwtToken" }
    $SnapshotsList = Invoke-RestMethod -Uri "$BaseUrl/test/snapshots" -Method Get -Headers $Headers -ErrorAction Stop
    if ($SnapshotsList.Count -gt 0) {
        $SnapshotSuccess = $true
    } else {
        $Details = "List of database snapshots was empty."
    }
} catch {
    $Details = $_.Exception.Message
}
Register-TestResult "Verify StatSnapshots generated in database" $SnapshotSuccess $Details

# Test 5.3: Query ML Predictions & Stats Summaries
$SummarySuccess = $false
$Details = ""
try {
    $res = Invoke-RestMethod -Uri "$BaseUrl/api/v1/analytics/trips/summary" -Method Get -Headers $Headers -ErrorAction Stop
    if ($res.Count -gt 0) {
        $SummarySuccess = $true
    }
} catch {
    $Details = $_.Exception.Message
}
Register-TestResult "Query trips summaries & ML predictions endpoints" $SummarySuccess $Details



# --- STEP 6: PRINT SCORECARD SUMMARY ---
Write-Host "`n========================================================================" -ForegroundColor Cyan
Write-Host "[DONE] INTEGRATION TESTING COMPLETE - SCORECARD SUMMARY" -ForegroundColor Yellow
Write-Host "========================================================================" -ForegroundColor Cyan

$SuccessRate = ($ScoreCard.Pass / $ScoreCard.Total) * 100
$SuccessRateFormatted = "{0:N2}" -f $SuccessRate

Write-Host "  Total Tests Executed: $($ScoreCard.Total)" -ForegroundColor White
Write-Host "  Successful Checks:   $($ScoreCard.Pass)" -ForegroundColor Green
if ($ScoreCard.Fail -gt 0) {
    Write-Host "  Failed Checks:       $($ScoreCard.Fail)" -ForegroundColor Red
} else {
    Write-Host "  Failed Checks:       0" -ForegroundColor Green
}
Write-Host "  Success Rate:        $SuccessRateFormatted%" -ForegroundColor White
Write-Host "------------------------------------------------------------------------" -ForegroundColor Cyan

if ($ScoreCard.Fail -eq 0) {
    Write-Host "  [SUCCESS] ALL TESTS PASSED! THE APPLICATION IS FULLY FUNCTIONAL!" -ForegroundColor Green
    Write-Host "  Feel free to navigate to Grafana (http://localhost:3000) for visual metrics." -ForegroundColor Green
} else {
    Write-Host "  [WARNING] Some integration tests failed. Check log traces above." -ForegroundColor Red
}
Write-Host "========================================================================" -ForegroundColor Cyan
