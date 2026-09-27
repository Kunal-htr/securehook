$ErrorActionPreference = 'Stop'

# 1. Create a webhook.site inbox
$webhookTokenRaw = curl.exe -s -X POST https://webhook.site/token
$webhookToken = $webhookTokenRaw | ConvertFrom-Json
$uuid = $webhookToken.uuid
$targetUrl = "https://webhook.site/$uuid"

Write-Host "Webhook URL: $targetUrl"

# 2. POST subscription
$subJson = @{
    targetUrl = $targetUrl
    eventTypes = @("order.created")
} | ConvertTo-Json -Depth 5 -Compress

[io.file]::WriteAllText("$PWD\sub.json", $subJson)
$subResponseRaw = curl.exe -s -X POST http://localhost:8080/subscriptions -H "Content-Type: application/json" -d "@sub.json"
$subResponse = $subResponseRaw | ConvertFrom-Json
$subId = $subResponse.id
$secretKey = $subResponse.secretKey

Write-Host "Subscription ID: $subId"
Write-Host "Secret Key: $secretKey"

# 3. POST event
$eventJson = @{
    eventType = "order.created"
    payload = @{
        orderId = "sig-test-1"
    }
} | ConvertTo-Json -Depth 5 -Compress

[io.file]::WriteAllText("$PWD\event1.json", $eventJson)
$eventResponseRaw = curl.exe -s -X POST http://localhost:8080/events -H "Content-Type: application/json" -d "@event1.json"

Write-Host "Event posted"

# 4. Wait 3 seconds, fetch webhook requests
Start-Sleep -Seconds 3

$requestsRaw = curl.exe -s "https://webhook.site/token/$uuid/requests"
$requestsData = $requestsRaw | ConvertFrom-Json
$latestRequest = $requestsData.data[0]

$receivedBody = $latestRequest.content
$receivedSignature = $latestRequest.headers.'x-signature'[0]

Write-Host "Received Body: $receivedBody"
Write-Host "Received Signature: $receivedSignature"

# 5. Independently recompute HMAC
# The received body might contain quotes that need escaping for Python, so pass it via env var
$env:PY_SECRET = $secretKey
$env:PY_BODY = $receivedBody

$pythonScript = @"
import hmac, hashlib, base64, os
key_b64 = os.environ['PY_SECRET']
# Pad base64url string properly
key_b64 += '=' * (-len(key_b64) % 4)
key = base64.urlsafe_b64decode(key_b64)
body = os.environ['PY_BODY'].encode('utf-8')
expected = 'sha256=' + hmac.new(key, body, hashlib.sha256).hexdigest()
print(expected)
"@

$computedSignature = python -c $pythonScript

Write-Host "Computed Signature: $computedSignature"

# 6. Compare
if ($receivedSignature -eq $computedSignature) {
    Write-Host "MATCH"
} else {
    Write-Host "MISMATCH"
}

# 7. Cleanup
curl.exe -s -X DELETE http://localhost:8080/subscriptions/$subId
Remove-Item sub.json, event1.json
