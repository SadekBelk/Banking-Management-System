package com.bankingmanagement.accountservice.service.impl;

import com.bankingmanagement.accountservice.exception.AccountNotFoundException;
import com.bankingmanagement.accountservice.exception.InsufficientBalanceException;
import com.bankingmanagement.accountservice.exception.InvalidReservationStateException;
import com.bankingmanagement.accountservice.exception.ReservationNotFoundException;
import com.bankingmanagement.accountservice.model.Account;
import com.bankingmanagement.accountservice.model.BalanceReservation;
import com.bankingmanagement.accountservice.model.ReservationStatus;
import com.bankingmanagement.accountservice.repository.AccountRepository;
import com.bankingmanagement.accountservice.repository.BalanceReservationRepository;
import com.bankingmanagement.accountservice.service.BalanceReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Implementation of balance reservation logic.
 * 
 * Key design decisions:
 * 1. All operations are transactional to ensure consistency
 * 2. Idempotency is enforced via idempotency_key
 * 3. Available balance = actual balance - pending reservations
 * 4. Commit actually deducts from the account balance
 * 5. Release does NOT restore balance (it was never deducted)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceReservationServiceImpl implements BalanceReservationService {

    private final AccountRepository accountRepository;
    private final BalanceReservationRepository reservationRepository;

    /**
     * Default reservation expiry time (15 minutes).
     * In production, this would be configurable.
     */
    private static final Duration RESERVATION_EXPIRY = Duration.ofMinutes(15);

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAvailableBalance(UUID accountId) {
        Account account = findAccountOrThrow(accountId);
        BigDecimal totalReserved = reservationRepository.getTotalReservedAmount(accountId);
        return account.getBalance().subtract(totalReserved);
    }

    @Override
    @Transactional
    public BalanceReservation reserveBalance(UUID accountId, BigDecimal amount, String currency, String idempotencyKey) {
        log.info("Reserving balance: accountId={}, amount={}, currency={}, idempotencyKey={}",
                accountId, amount, currency, idempotencyKey);

        // 1️⃣ Idempotency check: Return existing reservation if same key
        var existingReservation = reservationRepository.findByIdempotencyKey(idempotencyKey);
        if (existingReservation.isPresent()) {
            log.info("Found existing reservation for idempotencyKey={}: {}", 
                    idempotencyKey, existingReservation.get().getId());
            return existingReservation.get();
        }

        // 2️⃣ Verify account exists
        Account account = findAccountOrThrow(accountId);

        // 3️⃣ Check available balance
        BigDecimal availableBalance = getAvailableBalance(accountId);
        if (availableBalance.compareTo(amount) < 0) {
            log.warn("Insufficient balance: accountId={}, requested={}, available={}",
                    accountId, amount, availableBalance);
            throw new InsufficientBalanceException(accountId, amount, availableBalance);
        }

        // 4️⃣ Create reservation (money is now "locked")
        BalanceReservation reservation = BalanceReservation.builder()
                .accountId(accountId)
                .amount(amount)
                .currency(currency)
                .status(ReservationStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .expiresAt(Instant.now().plus(RESERVATION_EXPIRY))
                .build();

        BalanceReservation saved = reservationRepository.save(reservation);
        log.info("Created reservation: id={}, accountId={}, amount={}", 
                saved.getId(), accountId, amount);

        return saved;
    }

    @Override
    @Transactional
    public void commitReservation(UUID reservationId, String transactionId) {
        log.info("Committing reservation: reservationId={}, transactionId={}", 
                reservationId, transactionId);

        // 1️⃣ Find reservation
        BalanceReservation reservation = findReservationOrThrow(reservationId);

        // 2️⃣ Validate state
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new InvalidReservationStateException(
                    reservationId, reservation.getStatus(), "commit"
            );
        }

        // 3️⃣ Find and update account balance (actual deduction)
        Account account = findAccountOrThrow(reservation.getAccountId());
        BigDecimal newBalance = account.getBalance().subtract(reservation.getAmount());
        
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            // This shouldn't happen if reservation logic is correct, but safety check
            log.error("Balance would go negative! accountId={}, currentBalance={}, reservedAmount={}",
                    account.getId(), account.getBalance(), reservation.getAmount());
            throw new InsufficientBalanceException(
                    account.getId(), reservation.getAmount(), account.getBalance()
            );
        }

        account.setBalance(newBalance);
        accountRepository.save(account);

        // 4️⃣ Update reservation status
        reservation.setStatus(ReservationStatus.COMMITTED);
        reservation.setTransactionId(transactionId);
        reservation.setCommittedAt(Instant.now());
        reservationRepository.save(reservation);

        log.info("Committed reservation: reservationId={}, newAccountBalance={}", 
                reservationId, newBalance);
    }

    @Override
    @Transactional
    public void releaseReservation(UUID reservationId, String reason) {
        log.info("Releasing reservation: reservationId={}, reason={}", reservationId, reason);

        // 1️⃣ Find reservation
        BalanceReservation reservation = findReservationOrThrow(reservationId);

        // 2️⃣ Validate state
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new InvalidReservationStateException(
                    reservationId, reservation.getStatus(), "release"
            );
        }

        // 3️⃣ Release reservation (no balance change needed - money was never deducted)
        reservation.setStatus(ReservationStatus.RELEASED);
        reservation.setReleaseReason(reason);
        reservation.setReleasedAt(Instant.now());
        reservationRepository.save(reservation);

        log.info("Released reservation: reservationId={}", reservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceReservation getReservation(UUID reservationId) {
        return findReservationOrThrow(reservationId);
    }

    // ================== Private Helpers ==================

    private Account findAccountOrThrow(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found with id: " + accountId
                ));
    }

    private BalanceReservation findReservationOrThrow(UUID reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
    }
}
