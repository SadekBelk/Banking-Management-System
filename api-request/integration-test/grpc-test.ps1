# =============================================================================
# Direct gRPC Testing Script using grpcurl
# =============================================================================
# 
# Prerequisites:
# 1. Install grpcurl: scoop install grpcurl (or download from GitHub)
# 2. account-service running with gRPC on port 9001
# 3. Docker: docker compose --profile full up
#
# Usage:
#   .\grpc-test.ps1                    # Run interactive menu
#   .\grpc-test.ps1 -ListServices      # List all gRPC services
#   .\grpc-test.ps1 -GetBalance -AccountId "uuid"
#   .\grpc-test.ps1 -Reserve -AccountId "uuid" -Amount 100
# =============================================================================

param(
    [switch]$ListServices,
    [switch]$ListMethods,
    [switch]$GetBalance,
    [switch]$Reserve,
    [switch]$Commit,
    [switch]$Release,
    [string]$AccountId,
    [string]$ReservationId,
    [decimal]$Amount = 100,
    [string]$Currency = "USD",
    [string]$GrpcHost = "localhost:9001"
)

$ErrorActionPreference = "Stop"

# Check if grpcurl is installed
function Test-GrpcUrl {
    try {
        $null = Get-Command grpcurl -ErrorAction Stop
        return $true
    } catch {
        Write-Host @"
[ERROR] grpcurl is not installed!

Install options:
  Windows (Scoop):  scoop install grpcurl
  Windows (Choco):  choco install grpcurl
  Download:         https://github.com/fullstorydev/grpcurl/releases

"@ -ForegroundColor Red
        return $false
    }
}

# List all available gRPC services
function Get-GrpcServices {
    Write-Host "`n[gRPC] Listing services on $GrpcHost..." -ForegroundColor Cyan
    grpcurl -plaintext $GrpcHost list
}

# List methods for AccountService
function Get-GrpcMethods {
    Write-Host "`n[gRPC] Listing methods for bank.account.AccountService..." -ForegroundColor Cyan
    grpcurl -plaintext $GrpcHost describe bank.account.AccountService
}

# Get balance for an account
function Get-AccountBalance {
    param([string]$Id)
    
    if (-not $Id) {
        Write-Host "[ERROR] AccountId is required. Use -AccountId parameter" -ForegroundColor Red
        return
    }
    
    Write-Host "`n[gRPC] GetBalance for account: $Id" -ForegroundColor Cyan
    
    $request = @{ account_id = $Id } | ConvertTo-Json -Compress
    
    grpcurl -plaintext -d $request $GrpcHost bank.account.AccountService/GetBalance
}

# Reserve balance
function New-BalanceReservation {
    param(
        [string]$Id,
        [decimal]$Amt,
        [string]$Cur
    )
    
    if (-not $Id) {
        Write-Host "[ERROR] AccountId is required. Use -AccountId parameter" -ForegroundColor Red
        return
    }
    
    $idempotencyKey = "reserve-" + [guid]::NewGuid().ToString()
    
    Write-Host "`n[gRPC] ReserveBalance:" -ForegroundColor Cyan
    Write-Host "       Account: $Id" -ForegroundColor White
    Write-Host "       Amount: $Amt $Cur" -ForegroundColor White
    Write-Host "       IdempotencyKey: $idempotencyKey" -ForegroundColor White
    
    # Amount in minor units (cents)
    $amountInCents = [int]($Amt * 100)
    
    $request = @{
        account_id = $Id
        amount = @{
            amount = $amountInCents
            currency = $Cur
        }
        idempotency_key = $idempotencyKey
    } | ConvertTo-Json -Compress
    
    grpcurl -plaintext -d $request $GrpcHost bank.account.AccountService/ReserveBalance
}

# Commit reservation
function Confirm-Reservation {
    param([string]$ResId)
    
    if (-not $ResId) {
        Write-Host "[ERROR] ReservationId is required. Use -ReservationId parameter" -ForegroundColor Red
        return
    }
    
    $transactionId = "txn-" + [guid]::NewGuid().ToString()
    
    Write-Host "`n[gRPC] CommitReservation:" -ForegroundColor Cyan
    Write-Host "       ReservationId: $ResId" -ForegroundColor White
    Write-Host "       TransactionId: $transactionId" -ForegroundColor White
    
    $request = @{
        reservation_id = $ResId
        transaction_id = $transactionId
    } | ConvertTo-Json -Compress
    
    grpcurl -plaintext -d $request $GrpcHost bank.account.AccountService/CommitReservation
}

# Release reservation
function Undo-Reservation {
    param([string]$ResId)
    
    if (-not $ResId) {
        Write-Host "[ERROR] ReservationId is required. Use -ReservationId parameter" -ForegroundColor Red
        return
    }
    
    Write-Host "`n[gRPC] ReleaseReservation:" -ForegroundColor Cyan
    Write-Host "       ReservationId: $ResId" -ForegroundColor White
    
    $request = @{
        reservation_id = $ResId
        reason = "Released via test script"
    } | ConvertTo-Json -Compress
    
    grpcurl -plaintext -d $request $GrpcHost bank.account.AccountService/ReleaseReservation
}

# Interactive menu
function Show-Menu {
    Write-Host @"

============================================
   gRPC Test Menu - Account Service
   Host: $GrpcHost
============================================
"@ -ForegroundColor Cyan

    Write-Host "1. List all gRPC services"
    Write-Host "2. Describe AccountService methods"
    Write-Host "3. GetBalance (requires account ID)"
    Write-Host "4. ReserveBalance (requires account ID)"
    Write-Host "5. CommitReservation (requires reservation ID)"
    Write-Host "6. ReleaseReservation (requires reservation ID)"
    Write-Host "7. Full test flow (create account + reserve + commit)"
    Write-Host "Q. Quit"
    Write-Host ""
    
    $choice = Read-Host "Select option"
    
    switch ($choice) {
        "1" { Get-GrpcServices; Show-Menu }
        "2" { Get-GrpcMethods; Show-Menu }
        "3" { 
            $id = Read-Host "Enter Account ID"
            Get-AccountBalance -Id $id
            Show-Menu 
        }
        "4" { 
            $id = Read-Host "Enter Account ID"
            $amt = Read-Host "Enter Amount (default: 100)"
            if (-not $amt) { $amt = 100 }
            New-BalanceReservation -Id $id -Amt $amt -Cur "USD"
            Show-Menu 
        }
        "5" { 
            $resId = Read-Host "Enter Reservation ID"
            Confirm-Reservation -ResId $resId
            Show-Menu 
        }
        "6" { 
            $resId = Read-Host "Enter Reservation ID"
            Undo-Reservation -ResId $resId
            Show-Menu 
        }
        "7" { 
            Start-FullTestFlow
            Show-Menu 
        }
        "Q" { return }
        "q" { return }
        default { Show-Menu }
    }
}

# Full test flow
function Start-FullTestFlow {
    Write-Host "`n============================================" -ForegroundColor Cyan
    Write-Host "   Full gRPC Test Flow" -ForegroundColor Cyan
    Write-Host "============================================" -ForegroundColor Cyan
    
    # Step 1: Create account via REST
    Write-Host "`n[Step 1] Creating account via REST API..." -ForegroundColor Yellow
    
    $accountBody = @{
        customerId = "550e8400-e29b-41d4-a716-446655440001"
        type = "CHECKING"
        currency = "USD"
        initialBalance = 1000.00
    } | ConvertTo-Json
    
    try {
        $account = Invoke-RestMethod -Uri "http://localhost:4001/api/accounts" `
            -Method POST `
            -ContentType "application/json" `
            -Body $accountBody
        
        Write-Host "[OK] Account created: $($account.id)" -ForegroundColor Green
        Write-Host "     Balance: $($account.balance) $($account.currency)" -ForegroundColor White
        
        $accountId = $account.id
    } catch {
        Write-Host "[ERROR] Failed to create account: $_" -ForegroundColor Red
        return
    }
    
    # Step 2: Get balance via gRPC
    Write-Host "`n[Step 2] Getting balance via gRPC..." -ForegroundColor Yellow
    Get-AccountBalance -Id $accountId
    
    # Step 3: Reserve balance
    Write-Host "`n[Step 3] Reserving `$100 via gRPC..." -ForegroundColor Yellow
    $reserveResult = grpcurl -plaintext -d "{`"account_id`":`"$accountId`",`"amount`":{`"amount`":10000,`"currency`":`"USD`"},`"idempotency_key`":`"test-$(Get-Random)`"}" $GrpcHost bank.account.AccountService/ReserveBalance
    
    Write-Host $reserveResult -ForegroundColor White
    
    # Extract reservation_id from response
    $reservationId = ($reserveResult | ConvertFrom-Json).reservation_id
    
    if ($reservationId) {
        Write-Host "[OK] Reservation ID: $reservationId" -ForegroundColor Green
        
        # Step 4: Check balance again
        Write-Host "`n[Step 4] Checking balance after reservation..." -ForegroundColor Yellow
        Get-AccountBalance -Id $accountId
        
        # Step 5: Commit
        Write-Host "`n[Step 5] Committing reservation..." -ForegroundColor Yellow
        Confirm-Reservation -ResId $reservationId
        
        # Step 6: Final balance
        Write-Host "`n[Step 6] Final balance check..." -ForegroundColor Yellow
        Get-AccountBalance -Id $accountId
    }
    
    Write-Host "`n[DONE] Full test flow completed!" -ForegroundColor Green
}

# Main execution
if (-not (Test-GrpcUrl)) {
    exit 1
}

if ($ListServices) {
    Get-GrpcServices
} elseif ($ListMethods) {
    Get-GrpcMethods
} elseif ($GetBalance) {
    Get-AccountBalance -Id $AccountId
} elseif ($Reserve) {
    New-BalanceReservation -Id $AccountId -Amt $Amount -Cur $Currency
} elseif ($Commit) {
    Confirm-Reservation -ResId $ReservationId
} elseif ($Release) {
    Undo-Reservation -ResId $ReservationId
} else {
    # Interactive mode
    Show-Menu
}
