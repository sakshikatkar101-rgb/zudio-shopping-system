# ========================================================
# ZUDIO Security Audit & Vulnerability Verification Suite
# ========================================================
$baseUrl = "http://localhost:8080"
$totalTests = 0
$passedTests = 0

function Assert-Test {
    param(
        [string]$testName,
        [bool]$condition,
        [string]$details
    )
    $script:totalTests++
    if ($condition) {
        $script:passedTests++
        Write-Host " [PASS] $testName" -ForegroundColor Green
        if ($details) { Write-Host "        $details" -ForegroundColor DarkGray }
    } else {
        Write-Host " [FAIL] $testName" -ForegroundColor Red
        if ($details) { Write-Host "        $details" -ForegroundColor Yellow }
    }
}

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  ZUDIO Automated Security Audit & Verification Runner" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "Target: $baseUrl`n"

# -------------------------------------------------------------------
# TEST SUITE 1: Static File Path Traversal & Protected Files Defense
# -------------------------------------------------------------------
Write-Host ">>> TEST SUITE 1: Static File Path Traversal Defense" -ForegroundColor Yellow

$traversalUrls = @(
    "$baseUrl/../../pom.xml",
    "$baseUrl/sql/schema.sql",
    "$baseUrl/src/main/resources/config.properties",
    "$baseUrl/%2e%2e/%2e%2e/pom.xml",
    "$baseUrl/WEB-INF/web.xml",
    "$baseUrl/.env",
    "$baseUrl/run.bat"
)

foreach ($url in $traversalUrls) {
    try {
        $resp = Invoke-WebRequest -Uri $url -Method Get -UseBasicParsing -ErrorAction Stop
        Assert-Test "Path Traversal Probe: $url" $false "Returned HTTP $($resp.StatusCode), expected 403 Forbidden"
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Assert-Test "Path Traversal Probe: $url" ($statusCode -eq 403) "Rejected with HTTP $statusCode"
    }
}

# -------------------------------------------------------------------
# TEST SUITE 2: RBAC & Customer Privilege Escalation Defense
# -------------------------------------------------------------------
Write-Host "`n>>> TEST SUITE 2: RBAC & Customer Privilege Escalation" -ForegroundColor Yellow

# Authenticate as regular customer Sakshi (User ID = 2, role = CUSTOMER)
$clientIp = "192.168.1.50"
$custLoginBody = @{ email = "sakshi@example.com"; password = "pass123" } | ConvertTo-Json
$custAuth = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Headers @{ "X-Forwarded-For" = $clientIp } -Body $custLoginBody -ContentType "application/json"
$customerToken = $custAuth.data.token

Assert-Test "Customer Authentication" ($customerToken -ne $null) "Customer token obtained successfully"

# Attempt Admin Endpoints with Customer Token
$adminEndpoints = @(
    "$baseUrl/api/admin/dashboard",
    "$baseUrl/api/admin/orders",
    "$baseUrl/api/admin/customers"
)

foreach ($url in $adminEndpoints) {
    try {
        $headers = @{ Authorization = "Bearer $customerToken"; "X-Forwarded-For" = $clientIp }
        $resp = Invoke-WebRequest -Uri $url -Method Get -Headers $headers -UseBasicParsing -ErrorAction Stop
        Assert-Test "Admin Access with Customer Token: $url" $false "Returned HTTP $($resp.StatusCode), expected 403 Forbidden"
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Assert-Test "Admin Access with Customer Token: $url" ($statusCode -eq 403) "Rejected with HTTP $statusCode Forbidden"
    }
}

# -------------------------------------------------------------------
# TEST SUITE 3: IDOR (Insecure Direct Object Reference) Protection
# -------------------------------------------------------------------
Write-Host "`n>>> TEST SUITE 3: Insecure Direct Object Reference (IDOR)" -ForegroundColor Yellow

# Sakshi (User ID = 2). Attempt to access Order #1003 (which belongs to Rohit, User ID = 3)
try {
    $headers = @{ Authorization = "Bearer $customerToken"; "X-Forwarded-For" = $clientIp }
    $resp = Invoke-WebRequest -Uri "$baseUrl/api/orders/1003" -Method Get -Headers $headers -UseBasicParsing -ErrorAction Stop
    Assert-Test "Customer reading another user's order (IDOR on order #1003)" $false "Returned HTTP $($resp.StatusCode), expected 403 Forbidden"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Assert-Test "Customer reading another user's order (IDOR on order #1003)" ($statusCode -eq 403) "Rejected with HTTP $statusCode Forbidden"
}

# -------------------------------------------------------------------
# TEST SUITE 4: Server-Side Price & Total Tampering Defense
# -------------------------------------------------------------------
Write-Host "`n>>> TEST SUITE 4: Server-Side Price & Integrity Enforcement" -ForegroundColor Yellow

# Add product #1 to cart
$addCart1 = @{ productId = 1; size = "M"; quantity = 2 } | ConvertTo-Json
Invoke-RestMethod -Uri "$baseUrl/api/cart" -Method Post -Headers @{ Authorization = "Bearer $customerToken"; "X-Forwarded-For" = $clientIp } -Body $addCart1 -ContentType "application/json" | Out-Null

# Customer submits checkout with intentionally forged client prices (₹2.00 total)
$tamperedOrder = @{
    paymentMethod = "COD";
    address = "123 Security Enclave, Cyber Hub, Gurugram 122002";
    couponCode = $null;
    totalAmount = 2.00 # Forged total!
} | ConvertTo-Json

$orderResp = Invoke-RestMethod -Uri "$baseUrl/api/orders" -Method Post -Headers @{ Authorization = "Bearer $customerToken"; "X-Forwarded-For" = $clientIp } -Body $tamperedOrder -ContentType "application/json"
$createdOrder = $orderResp.data

# Fetch product 1 price from catalog
$prod1 = Invoke-RestMethod -Uri "$baseUrl/api/products/1" -Method Get
$actualTotal = $createdOrder.totalAmount
Assert-Test "Server Ignores Client Forged Price" ($actualTotal -gt 500) "Client submitted ₹2.00, Server computed official total ₹$actualTotal from database catalog prices"

# -------------------------------------------------------------------
# TEST SUITE 5: SQL Injection Attack Defense
# -------------------------------------------------------------------
Write-Host "`n>>> TEST SUITE 5: SQL Injection Defense" -ForegroundColor Yellow

$sqliLogins = @(
    "' OR '1'='1",
    "admin@zudio.in' --",
    "' UNION SELECT 1, 'hacker', 'hash', 'ADMIN' --",
    "1' OR '1' = '1"
)

$sqliIp = "198.51.100.10"
foreach ($payload in $sqliLogins) {
    try {
        $body = @{ email = $payload; password = "dummy" } | ConvertTo-Json
        $resp = Invoke-WebRequest -Uri "$baseUrl/api/auth/login" -Method Post -Headers @{ "X-Forwarded-For" = $sqliIp } -Body $body -ContentType "application/json" -UseBasicParsing -ErrorAction Stop
        Assert-Test "SQLi Login Bypass: $payload" $false "SQL injection granted access!"
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Assert-Test "SQLi Login Bypass: $payload" ($statusCode -eq 400 -or $statusCode -eq 401 -or $statusCode -eq 429) "Rejected cleanly with HTTP $statusCode (No 500 internal crash)"
    }
}

# SQLi in Search query
$searchResp = Invoke-RestMethod -Uri "$baseUrl/api/products?search=%27%20OR%20%271%27=%271" -Method Get
Assert-Test "SQLi in Search Filter Parameter" ($searchResp.success -eq $true) "Parameterized query handled SQL special characters cleanly without syntax errors"

# -------------------------------------------------------------------
# TEST SUITE 6: Stored XSS Mitigation in Orders
# -------------------------------------------------------------------
Write-Host "`n>>> TEST SUITE 6: Stored XSS Payload Defense" -ForegroundColor Yellow

# Add item to cart first
$addCart2 = @{ productId = 2; size = "L"; quantity = 1 } | ConvertTo-Json
Invoke-RestMethod -Uri "$baseUrl/api/cart" -Method Post -Headers @{ Authorization = "Bearer $customerToken"; "X-Forwarded-For" = $clientIp } -Body $addCart2 -ContentType "application/json" | Out-Null

$xssAddress = "Flat 101, <script>alert('XSS')</script><img src=x onerror=alert(1)>, Bandra West, Mumbai 400050"
$xssOrder = @{
    paymentMethod = "UPI";
    address = $xssAddress
} | ConvertTo-Json

$xssOrderResp = Invoke-RestMethod -Uri "$baseUrl/api/orders" -Method Post -Headers @{ Authorization = "Bearer $customerToken"; "X-Forwarded-For" = $clientIp } -Body $xssOrder -ContentType "application/json"
$createdXssOrder = $xssOrderResp.data
Assert-Test "Order placement with XSS string payload" ($createdXssOrder.id -ne $null) "Order created safely with ID #$($createdXssOrder.id)"

# Verify retrieved order
$getOrderResp = Invoke-RestMethod -Uri "$baseUrl/api/orders/$($createdXssOrder.id)" -Method Get -Headers @{ Authorization = "Bearer $customerToken"; "X-Forwarded-For" = $clientIp }
Assert-Test "Fetch Order with XSS payload intact for UI escaping" ($getOrderResp.data.address -like "*<script>*") "Backend safely preserved address literal; frontend UI.escapeHtml handles escaping"

# -------------------------------------------------------------------
# TEST SUITE 7: Brute-Force Rate Limiting Protection
# -------------------------------------------------------------------
Write-Host "`n>>> TEST SUITE 7: Brute-Force Rate Limiting" -ForegroundColor Yellow

$attackerIp = "198.51.100.99"
$attackerEmail = "victim_rate_test_$(Get-Random)@example.com"
$rateLimited = $false

for ($attempt = 1; $attempt -le 6; $attempt++) {
    try {
        $body = @{ email = $attackerEmail; password = "WrongPassword$attempt!" } | ConvertTo-Json
        $r = Invoke-WebRequest -Uri "$baseUrl/api/auth/login" -Method Post -Headers @{ "X-Forwarded-For" = $attackerIp } -Body $body -ContentType "application/json" -UseBasicParsing -ErrorAction Stop
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($attempt -le 5) {
            # Expected 401
        } else {
            # 6th attempt should be 429
            if ($code -eq 429) {
                $rateLimited = $true
            }
        }
    }
}

Assert-Test "Brute Force Defense (5 max attempts, 6th triggers HTTP 429)" $rateLimited "6th attempt returned HTTP 429 Too Many Requests"

# -------------------------------------------------------------------
# TEST SUITE 8: Production Healthcheck Endpoint
# -------------------------------------------------------------------
Write-Host "`n>>> TEST SUITE 8: Health Probe" -ForegroundColor Yellow

$health = Invoke-RestMethod -Uri "$baseUrl/health" -Method Get
Assert-Test "GET /health probe" ($health.status -eq "UP" -and $health.db -eq "UP") "Status: $($health.status), DB: $($health.db)"

# -------------------------------------------------------------------
# TEST SUITE 9: Order Cancellation & ACID Inventory Restoration
# -------------------------------------------------------------------
Write-Host "`n>>> TEST SUITE 9: Order Cancellation & Inventory Stock Restoration" -ForegroundColor Yellow

# 9.1: IDOR Cancellation Attempt (Sakshi attempts to cancel Rohit's order #1003)
try {
    $headers = @{ Authorization = "Bearer $customerToken"; "X-Forwarded-For" = $clientIp }
    $cancelBody = @{ reason = "Malicious cancellation attempt" } | ConvertTo-Json
    $resp = Invoke-WebRequest -Uri "$baseUrl/api/orders/1003/cancel" -Method Post -Headers $headers -Body $cancelBody -ContentType "application/json" -UseBasicParsing -ErrorAction Stop
    Assert-Test "Anti-IDOR: Customer cancelling another user's order (#1003)" $false "Returned HTTP $($resp.StatusCode), expected 403 Forbidden"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Assert-Test "Anti-IDOR: Customer cancelling another user's order (#1003)" ($statusCode -eq 403) "Blocked with HTTP $statusCode Forbidden"
}

# 9.2: Stock decrement, cancellation, and exact stock restoration
# Fetch product #3 initial stock
$prod3Initial = Invoke-RestMethod -Uri "$baseUrl/api/products/3" -Method Get
$initialStock = $prod3Initial.data.stock
Write-Host "        Product #3 Initial Stock: $initialStock" -ForegroundColor DarkGray

# Add product #3 (qty 2) to cart
$addCart3 = @{ productId = 3; size = "M"; quantity = 2 } | ConvertTo-Json
Invoke-RestMethod -Uri "$baseUrl/api/cart" -Method Post -Headers @{ Authorization = "Bearer $customerToken"; "X-Forwarded-For" = $clientIp } -Body $addCart3 -ContentType "application/json" | Out-Null

# Place Order with CARD payment simulation
$cardOrderBody = @{
    paymentMethod = "CARD";
    paymentStatus = "PAID";
    cardLast4 = "8890";
    cardBrand = "Visa";
    address = "42 Silicon Avenue, Outer Ring Road, Bengaluru 560103"
} | ConvertTo-Json

$placedCardOrder = Invoke-RestMethod -Uri "$baseUrl/api/orders" -Method Post -Headers @{ Authorization = "Bearer $customerToken"; "X-Forwarded-For" = $clientIp } -Body $cardOrderBody -ContentType "application/json"
$cardOrderId = $placedCardOrder.data.id

# Check stock after placement (should be initialStock - 2)
$prod3AfterPlace = Invoke-RestMethod -Uri "$baseUrl/api/products/3" -Method Get
$stockAfterPlace = $prod3AfterPlace.data.stock
Assert-Test "Inventory Stock Decrement on Order Placement" ($stockAfterPlace -eq ($initialStock - 2)) "Stock decreased from $initialStock to $stockAfterPlace (-2)"

# Check payment fields persisted
Assert-Test "Simulated Card Details Persisted" ($placedCardOrder.data.paymentStatus -eq "PAID" -and $placedCardOrder.data.cardLast4 -eq "8890" -and $placedCardOrder.data.cardBrand -eq "Visa") "Payment Status: $($placedCardOrder.data.paymentStatus), Brand: $($placedCardOrder.data.cardBrand), Last4: $($placedCardOrder.data.cardLast4)"

# Cancel Order via POST /api/orders/{id}/cancel
$cancelPayload = @{ reason = "Found better discount elsewhere" } | ConvertTo-Json
$cancelResp = Invoke-RestMethod -Uri "$baseUrl/api/orders/$cardOrderId/cancel" -Method Post -Headers @{ Authorization = "Bearer $customerToken"; "X-Forwarded-For" = $clientIp } -Body $cancelPayload -ContentType "application/json"

Assert-Test "Order Cancellation Execution" ($cancelResp.success -eq $true -and $cancelResp.data.status -eq "Cancelled") "Order #$cardOrderId cancelled. Status: $($cancelResp.data.status)"
Assert-Test "Online Order Refund Status" ($cancelResp.data.paymentStatus -eq "REFUND_PENDING") "Payment status set to REFUND_PENDING for online card order"
Assert-Test "Cancel Reason Recorded" ($cancelResp.data.cancelReason -eq "Found better discount elsewhere") "Reason recorded: '$($cancelResp.data.cancelReason)'"

# Verify stock restored back to initialStock
$prod3AfterCancel = Invoke-RestMethod -Uri "$baseUrl/api/products/3" -Method Get
$stockAfterCancel = $prod3AfterCancel.data.stock
Assert-Test "Inventory Stock Restoration on Cancellation" ($stockAfterCancel -eq $initialStock) "Stock restored from $stockAfterPlace back to $stockAfterCancel (+2)"

# 9.3: Double Cancellation Rejection
try {
    $doubleCancelResp = Invoke-WebRequest -Uri "$baseUrl/api/orders/$cardOrderId/cancel" -Method Post -Headers @{ Authorization = "Bearer $customerToken"; "X-Forwarded-For" = $clientIp } -Body $cancelPayload -ContentType "application/json" -UseBasicParsing -ErrorAction Stop
    Assert-Test "Double Cancellation Defense" $false "Returned HTTP $($doubleCancelResp.StatusCode), expected 400 Bad Request"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Assert-Test "Double Cancellation Defense" ($statusCode -eq 400) "Blocked duplicate cancellation with HTTP $statusCode Bad Request"
}

# 9.4: Non-Cancellable Status Rejection (Shipped / Delivered cannot be cancelled)
# Create a new COD order
$addCart4 = @{ productId = 1; size = "L"; quantity = 1 } | ConvertTo-Json
Invoke-RestMethod -Uri "$baseUrl/api/cart" -Method Post -Headers @{ Authorization = "Bearer $customerToken"; "X-Forwarded-For" = $clientIp } -Body $addCart4 -ContentType "application/json" | Out-Null

$codOrderBody = @{
    paymentMethod = "COD";
    address = "10 Downing Street, Colaba, Mumbai 400001"
} | ConvertTo-Json
$codOrder = Invoke-RestMethod -Uri "$baseUrl/api/orders" -Method Post -Headers @{ Authorization = "Bearer $customerToken"; "X-Forwarded-For" = $clientIp } -Body $codOrderBody -ContentType "application/json"
$codOrderId = $codOrder.data.id

# Admin logs in (try configured admin passwords)
$adminPasswords = @(
    $env:ADMIN_PASSWORD,
    "admin123",
    "Admin@Zudio2026!",
    "Zudio@Admin#4827"
) | Where-Object { $_ -ne $null -and $_ -ne "" }

$adminToken = $null
foreach ($p in $adminPasswords) {
    try {
        $body = @{ email = "admin@zudio.in"; password = $p } | ConvertTo-Json
        $auth = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Headers @{ "X-Forwarded-For" = "127.0.0.1" } -Body $body -ContentType "application/json"
        if ($auth.success -and $auth.data.token) {
            $adminToken = $auth.data.token
            break
        }
    } catch {}
}
Assert-Test "Admin Authentication" ($adminToken -ne $null) "Admin token acquired successfully"

# Admin updates order status to 'Shipped'
$shipPayload = @{ status = "Shipped" } | ConvertTo-Json
Invoke-RestMethod -Uri "$baseUrl/api/admin/orders/$codOrderId" -Method Put -Headers @{ Authorization = "Bearer $adminToken"; "X-Forwarded-For" = "127.0.0.1" } -Body $shipPayload -ContentType "application/json" | Out-Null

# Customer attempts to cancel Shipped order
try {
    $cancelShippedResp = Invoke-WebRequest -Uri "$baseUrl/api/orders/$codOrderId/cancel" -Method Post -Headers @{ Authorization = "Bearer $customerToken"; "X-Forwarded-For" = $clientIp } -Body $cancelPayload -ContentType "application/json" -UseBasicParsing -ErrorAction Stop
    Assert-Test "Dispatched Order Cancellation Blocked" $false "Shipped order was cancelled, expected 400 rejection"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Assert-Test "Dispatched Order Cancellation Blocked" ($statusCode -eq 400) "Shipped order cancellation rejected with HTTP $statusCode"
}

# -------------------------------------------------------------------
# SUMMARY
# -------------------------------------------------------------------
Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "  TEST SUMMARY: $passedTests / $totalTests Passed ($([Math]::Round(($passedTests / $totalTests) * 100))%)" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

if ($passedTests -eq $totalTests) {
    Write-Host "[SUCCESS] ALL SECURITY & INTEGRITY TESTS PASSED (100%)!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "[FAIL] Some tests failed. Please review." -ForegroundColor Red
    exit 1
}
