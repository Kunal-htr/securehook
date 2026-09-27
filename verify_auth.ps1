$ErrorActionPreference = 'Stop'

$apiKey = $env:API_KEY
if (-not $apiKey) {
    Write-Error "API_KEY environment variable is not set. Please set it before running this script."
    exit 1
}

$baseUrl = "http://localhost:8080"
$wrongKey = "WRONG-KEY-12345"

Write-Host "Starting API Key Authentication Verification..."
Write-Host "============================================="

# 1. 401 with no header
Write-Host "`nTest 1: GET /subscriptions with NO header"
$response = curl.exe -s -i -X GET $baseUrl/subscriptions
if ($response -match "HTTP/1.1 401") {
    Write-Host "[PASS] 401 Unauthorized with no header."
    # Extract body just to show
    $body = curl.exe -s -X GET $baseUrl/subscriptions
    Write-Host "       Body: $body"
} else {
    Write-Host "[FAIL] Expected 401 but got something else."
    Write-Host $response
}

# 2. 401 with wrong key
Write-Host "`nTest 2: GET /subscriptions with WRONG key"
$response = curl.exe -s -i -X GET $baseUrl/subscriptions -H "X-API-Key: $wrongKey"
if ($response -match "HTTP/1.1 401") {
    Write-Host "[PASS] 401 Unauthorized with wrong key."
    $body = curl.exe -s -X GET $baseUrl/subscriptions -H "X-API-Key: $wrongKey"
    Write-Host "       Body: $body"
} else {
    Write-Host "[FAIL] Expected 401 but got something else."
    Write-Host $response
}

# 3. 200 with correct key
Write-Host "`nTest 3: GET /subscriptions with CORRECT key"
$response = curl.exe -s -i -X GET $baseUrl/subscriptions -H "X-API-Key: $apiKey"
if ($response -match "HTTP/1.1 200") {
    Write-Host "[PASS] 200 OK with correct key."
} else {
    Write-Host "[FAIL] Expected 200 but got something else."
    Write-Host $response
}

# 4. Confirm /deliveries is also protected
Write-Host "`nTest 4: GET /deliveries/{uuid} with NO header"
$dummyId = [guid]::NewGuid().ToString()
$response = curl.exe -s -i -X GET $baseUrl/deliveries/$dummyId
if ($response -match "HTTP/1.1 401") {
    Write-Host "[PASS] /deliveries is protected (401 Unauthorized)."
} else {
    Write-Host "[FAIL] /deliveries is NOT protected or returned unexpected status."
    Write-Host $response
}

# 5. Full subscribe-publish-deliver-verify round trip
Write-Host "`nTest 5: Full Subscribe -> Publish -> Deliver -> Verify Round Trip"

# 5a. Create webhook.site inbox
$webhookTokenRaw = curl.exe -s -X POST https://webhook.site/token
$webhookToken = $webhookTokenRaw | ConvertFrom-Json
$uuid = $webhookToken.uuid
$targetUrl = "https://webhook.site/$uuid"
Write-Host "       Created Webhook Inbox: $targetUrl"

# 5b. Subscribe
$subJson = @{
    targetUrl = $targetUrl
    eventTypes = @("order.created")
} | ConvertTo-Json -Compress
[io.file]::WriteAllText("$PWD\sub.json", $subJson)

$subResponseRaw = curl.exe -s -X POST $baseUrl/subscriptions -H "Content-Type: application/json" -H "X-API-Key: $apiKey" -d "@sub.json"
$subResponse = $subResponseRaw | ConvertFrom-Json
$subId = $subResponse.id
Write-Host "       Created Subscription: $subId"

# 5c. Publish Event
$eventJson = @{
    eventType = "order.created"
    payload = @{ orderId = "auth-test-1" }
} | ConvertTo-Json -Compress
[io.file]::WriteAllText("$PWD\event1.json", $eventJson)

$eventResponseRaw = curl.exe -s -X POST $baseUrl/events -H "Content-Type: application/json" -H "X-API-Key: $apiKey" -d "@event1.json"
$eventResponse = $eventResponseRaw | ConvertFrom-Json
$eventId = $eventResponse.eventId
Write-Host "       Published Event: $eventId"

# 5d. Wait and verify delivery
Write-Host "       Waiting 3 seconds for async delivery..."
Start-Sleep -Seconds 3

$deliveriesRaw = curl.exe -s -X GET $baseUrl/deliveries/$eventId -H "X-API-Key: $apiKey"
$deliveries = $deliveriesRaw | ConvertFrom-Json

if ($deliveries.Count -gt 0 -and $deliveries[0].success -eq $true) {
    Write-Host "       [PASS] Delivery log confirmed success for attempt 1."
} else {
    Write-Host "       [FAIL] Delivery log not found or not successful."
    Write-Host $deliveriesRaw
}

# 5e. Cleanup
$cleanupCmd = "curl.exe -s -i -X DELETE $baseUrl/subscriptions/$subId -H `"X-API-Key: $apiKey`""
Write-Host "       Running cleanup: $cleanupCmd"
$delResp = Invoke-Expression $cleanupCmd
if ($delResp -match "HTTP/1.1 204" -or $delResp -match "HTTP/1.1 404") {
    Write-Host "       [PASS] Subscription deleted successfully."
} else {
    Write-Host "       [FAIL] Failed to delete subscription."
    Write-Host $delResp
}
Remove-Item sub.json, event1.json -ErrorAction Ignore

Write-Host "`nAll tests completed."
