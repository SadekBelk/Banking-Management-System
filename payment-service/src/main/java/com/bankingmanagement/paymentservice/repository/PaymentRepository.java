package com.bankingmanagement.paymentservice.repository;

import com.bankingmanagement.paymentservice.model.Payment;
import com.bankingmanagement.paymentservice.model.PaymentStatus;
import com.bankingmanagement.paymentservice.model.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // 🔐 Uniqueness checks
    boolean existsByReferenceNumber(String referenceNumber);

    // 🔍 Lookup methods
    Optional<Payment> findByReferenceNumber(String referenceNumber);

    List<Payment> findBySourceAccountId(UUID sourceAccountId);

    List<Payment> findByDestinationAccountId(UUID destinationAccountId);

    List<Payment> findBySourceAccountIdOrDestinationAccountId(UUID sourceAccountId, UUID destinationAccountId);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByType(PaymentType type);

    List<Payment> findBySourceAccountIdAndStatus(UUID sourceAccountId, PaymentStatus status);

}
