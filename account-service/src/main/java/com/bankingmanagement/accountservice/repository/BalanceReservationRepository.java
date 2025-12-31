package com.bankingmanagement.accountservice.repository;

import com.bankingmanagement.accountservice.model.BalanceReservation;
import com.bankingmanagement.accountservice.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BalanceReservationRepository extends JpaRepository<BalanceReservation, UUID> {

    /**
     * Find reservation by idempotency key.
     * Used to handle duplicate reservation requests safely.
     */
    Optional<BalanceReservation> findByIdempotencyKey(String idempotencyKey);

    /**
     * Find all pending reservations for an account.
     * Used to calculate available balance.
     */
    List<BalanceReservation> findByAccountIdAndStatus(UUID accountId, ReservationStatus status);

    /**
     * Calculate total reserved (locked) amount for an account.
     * This is subtracted from actual balance to get available balance.
     */
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM BalanceReservation r " +
           "WHERE r.accountId = :accountId AND r.status = 'PENDING'")
    BigDecimal getTotalReservedAmount(@Param("accountId") UUID accountId);

    /**
     * Find expired reservations that need to be auto-released.
     */
    @Query("SELECT r FROM BalanceReservation r " +
           "WHERE r.status = 'PENDING' AND r.expiresAt < :now")
    List<BalanceReservation> findExpiredReservations(@Param("now") Instant now);
}
