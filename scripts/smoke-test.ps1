$ErrorActionPreference = 'Stop'

$baseQ = 'http://127.0.0.1:8082'
$baseO = 'http://127.0.0.1:8081'
$baseD = 'http://127.0.0.1:8080'
$baseF = 'http://127.0.0.1:8083'
$baseI = 'http://127.0.0.1:8084'
$dropId = "drop-smoke-$(Get-Random -Maximum 99999)"
$failed = 0

function Assert-Step([string]$name, [scriptblock]$script) {
  try {
    $detail = & $script
    Write-Host "PASS: $name => $detail"
  } catch {
    $script:failed++
    Write-Host "FAIL: $name => $($_.Exception.Message)"
  }
}

Assert-Step 'dashboard' { (Invoke-WebRequest "$baseD/" -UseBasicParsing).StatusCode }
Assert-Step 'order-health' { (Invoke-RestMethod "$baseO/actuator/health").status }
Assert-Step 'queue-health' { (Invoke-RestMethod "$baseQ/actuator/health").status }
Assert-Step 'fraud-health' { (Invoke-RestMethod "$baseF/actuator/health").status }
Assert-Step 'insights-health' { (Invoke-RestMethod "$baseI/actuator/health").status }

Assert-Step 'queue-join' {
  $body = @{ dropId = $dropId; accountId = 'acct-smoke-1'; fingerprint = 'fp-smoke-1' } | ConvertTo-Json
  $r = Invoke-RestMethod -Method Post -Uri "$baseQ/api/drops/$dropId/queue/join" -ContentType 'application/json' -Body $body
  if (-not $r.token) { throw 'missing token' }
  "position=$($r.queuePosition)"
}

Assert-Step 'hold-and-checkout' {
  $holdBody = @{ accountId = 'acct-smoke-hold'; quantity = 1 } | ConvertTo-Json
  $hold = Invoke-RestMethod -Method Post -Uri "$baseO/api/drops/drop-42/holds" -ContentType 'application/json' -Body $holdBody
  if (-not $hold.holdId) { throw 'missing holdId' }
  $before = (Invoke-RestMethod "$baseO/api/inventory/drop-42").available
  $checkoutBody = @{
    dropId = 'drop-42'
    accountId = 'acct-smoke-hold'
    holdId = $hold.holdId
    quantity = 1
    totalCents = 2500
  } | ConvertTo-Json
  $headers = @{ 'Idempotency-Key' = "idem-smoke-$(Get-Random)" }
  $paid = Invoke-RestMethod -Method Post -Uri "$baseO/api/checkout" -ContentType 'application/json' -Headers $headers -Body $checkoutBody
  $replay = Invoke-RestMethod -Method Post -Uri "$baseO/api/checkout" -ContentType 'application/json' -Headers $headers -Body $checkoutBody
  if ($paid.status -ne 'PAID') { throw "unexpected status $($paid.status)" }
  if ($paid.orderId -ne $replay.orderId) { throw 'idempotency replay mismatch' }
  $after = (Invoke-RestMethod "$baseO/api/inventory/drop-42").available
  if ($after -ne $before) { throw "stock should stay reserved after checkout before=$before after=$after" }
  "orderId=$($paid.orderId) remaining=$after"
}

Assert-Step 'rate-limit' {
  $throttled = 0
  for ($i = 0; $i -lt 12; $i++) {
    try {
      $body = @{ dropId = 'drop-burst'; accountId = "acct-burst-$i"; fingerprint = 'fp-burst-same' } | ConvertTo-Json
      Invoke-WebRequest -Method Post -Uri "$baseQ/api/drops/drop-burst/queue/join" -ContentType 'application/json' -Body $body -UseBasicParsing | Out-Null
    } catch {
      if ([int]$_.Exception.Response.StatusCode -eq 429) { $throttled++ }
    }
  }
  if ($throttled -lt 1) { throw 'expected 429 throttling' }
  "throttled=$throttled"
}

Assert-Step 'proxy-paths' {
  $inv = Invoke-RestMethod "$baseD/api/inventory/drop-42"
  $joinBody = @{ dropId = 'drop-proxy-smoke'; accountId = 'acct-proxy'; fingerprint = 'fp-proxy' } | ConvertTo-Json
  $join = Invoke-RestMethod -Method Post -Uri "$baseD/api/drops/drop-proxy-smoke/queue/join" -ContentType 'application/json' -Body $joinBody
  "available=$($inv.available) token=$($join.token)"
}

if ($failed -gt 0) {
  Write-Host "SMOKE FAILED ($failed)"
  exit 1
}

Write-Host 'SMOKE PASSED'
exit 0
