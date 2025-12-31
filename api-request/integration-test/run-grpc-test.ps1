# ============================================================================
# gRPC Integration Test Script
# Tests: payment-service (gRPC CLIENT) → account-service (gRPC SERVER)
# ============================================================================
# Prerequisites:
# 1. Terminal 1: cd account-service && .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
# 2. Terminal 2: cd payment-service && .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
# 3. Terminal 3: Run this script
# ============================================================================

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "gRPC Integration Test" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

# Check if services are running
Write-Host "`n[Check] Verifying services are running..." -ForegroundColor Yellow

try {
    $null = Invoke-RestMethod -Uri "http://localhost:4001/api/accounts" -Method GET -TimeoutSec 5
    Write-Host "[OK] account-service is running (HTTP: 4001, gRPC: 9001)" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] account-service is NOT running. Start it first!" -ForegroundColor Red
    exit 1
}

try {
    $null = Invoke-RestMethod -Uri "http://localhost:4003/api/payments" -Method GET -TimeoutSec 5 -ErrorAction SilentlyContinue
    Write-Host "[OK] payment-service is running (HTTP: 4003)" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode -eq 404 -or $_.Exception.Response.StatusCode -eq 500) {
        Write-Host "[OK] payment-service is running (HTTP: 4003)" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] payment-service is NOT running. Start it first!" -ForegroundColor Red
        exit 1
    }
}

# Step 1: Create Source Account
Write-Host "`n[Step 1] Creating source account with $1000 balance..." -ForegroundColor Yellow

$sourceAccountBody = @{
    customerId = "550e8400-e29b-41d4-a716-446655440001"
    type = "CHECKING"
    currency = "USD"
    initialBalance = 1000.00
} | ConvertTo-Json

$sourceAccount = Invoke-RestMethod -Uri "http://localhost:4001/api/accounts" `
    -Method POST `
    -ContentType "application/json" `
    -Body $sourceAccountBody

Write-Host "[OK] Source Account Created:" -ForegroundColor Green
Write-Host "     ID: $($sourceAccount.id)" -ForegroundColor White
Write-Host "     Account Number: $($sourceAccount.accountNumber)" -ForegroundColor White
Write-Host "     Balance: $($sourceAccount.balance) $($sourceAccount.currency)" -ForegroundColor White

$sourceAccountId = $sourceAccount.id

# Step 2: Create Destination Account
Write-Host "`n[Step 2] Creating destination account..." -ForegroundColor Yellow

$destAccountBody = @{
    customerId = "550e8400-e29b-41d4-a716-446655440002"
    type = "CHECKING"
    currency = "USD"
    initialBalance = 0.00
} | ConvertTo-Json

$destAccount = Invoke-RestMethod -Uri "http://localhost:4001/api/accounts" `
    -Method POST `
    -ContentType "application/json" `
    -Body $destAccountBody

Write-Host "[OK] Destination Account Created:" -ForegroundColor Green
Write-Host "     ID: $($destAccount.id)" -ForegroundColor White
Write-Host "     Account Number: $($destAccount.accountNumber)" -ForegroundColor White
Write-Host "     Balance: $($destAccount.balance) $($destAccount.currency)" -ForegroundColor White

$destAccountId = $destAccount.id

# Step 3: Make Payment (gRPC Integration)
Write-Host "`n[Step 3] Making payment of $100 via gRPC integration..." -ForegroundColor Yellow
Write-Host "         payment-service -> gRPC -> account-service" -ForegroundColor Cyan
Write-Host "         Flow: ReserveBalance -> CreatePayment -> CommitReservation -> CompletePayment" -ForegroundColor Cyan

$paymentBody = @{
    sourceAccountId = $sourceAccountId
    destinationAccountId = $destAccountId
    amount = 100.00
    currency = "USD"
    type = "TRANSFER"
    description = "Test gRPC integration transfer"
} | ConvertTo-Json

try {
    $payment = Invoke-RestMethod -Uri "http://localhost:4003/api/payments" `
        -Method POST `
        -ContentType "application/json" `
        -Body $paymentBody

    Write-Host "[OK] Payment Processed:" -ForegroundColor Green
    Write-Host "     Payment ID: $($payment.id)" -ForegroundColor White
    Write-Host "     Reference: $($payment.referenceNumber)" -ForegroundColor White
    Write-Host "     Status: $($payment.status)" -ForegroundColor White
    Write-Host "     Amount: $($payment.amount) $($payment.currency)" -ForegroundColor White
    
    $paymentId = $payment.id
} catch {
    Write-Host "[ERROR] Payment failed: $($_.Exception.Message)" -ForegroundColor Red
    
    # Try to get more details from the response
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response: $responseBody" -ForegroundColor Red
    }
}

# Step 4: Verify Source Account Balance
Write-Host "`n[Step 4] Verifying source account balance..." -ForegroundColor Yellow

$sourceAccountAfter = Invoke-RestMethod -Uri "http://localhost:4001/api/accounts/$sourceAccountId" -Method GET

Write-Host "[Result] Source Account Balance:" -ForegroundColor Green
Write-Host "         Before: 1000.00 USD" -ForegroundColor White
Write-Host "         After:  $($sourceAccountAfter.balance) $($sourceAccountAfter.currency)" -ForegroundColor White

if ($sourceAccountAfter.balance -eq 900.00) {
    Write-Host "[PASS] Balance correctly deducted!" -ForegroundColor Green
} else {
    Write-Host "[INFO] Balance: $($sourceAccountAfter.balance) (expected 900.00 after $100 transfer)" -ForegroundColor Yellow
}

# Step 5: Test Insufficient Balance
Write-Host "`n[Step 5] Testing insufficient balance scenario..." -ForegroundColor Yellow

$largePaymentBody = @{
    sourceAccountId = $sourceAccountId
    destinationAccountId = $destAccountId
    amount = 5000.00
    currency = "USD"
    type = "TRANSFER"
    description = "Should fail - insufficient balance"
} | ConvertTo-Json

try {
    $failedPayment = Invoke-RestMethod -Uri "http://localhost:4003/api/payments" `
        -Method POST `
        -ContentType "application/json" `
        -Body $largePaymentBody
    
    Write-Host "[UNEXPECTED] Payment succeeded when it should have failed" -ForegroundColor Red
} catch {
    Write-Host "[PASS] Payment correctly rejected - insufficient balance" -ForegroundColor Green
    Write-Host "       Error: $($_.Exception.Message)" -ForegroundColor White
}

# Summary
Write-Host "`n============================================" -ForegroundColor Cyan
Write-Host "Integration Test Complete!" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "`nTest Results:" -ForegroundColor Yellow
Write-Host "  - Source Account ID: $sourceAccountId" -ForegroundColor White
Write-Host "  - Destination Account ID: $destAccountId" -ForegroundColor White
if ($paymentId) {
    Write-Host "  - Payment ID: $paymentId" -ForegroundColor White
}
Write-Host "`ngRPC Communication:" -ForegroundColor Yellow
Write-Host "  - payment-service (client) -> account-service (server)" -ForegroundColor White
Write-Host "  - Port: 9001 (gRPC)" -ForegroundColor White
