package com.example.payment_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import com.example.payment_service.event.Event.Type;
import com.example.payment_service.model.PaymentInbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

@Repository
public interface PaymentInboxRepository extends JpaRepository<PaymentInbox, Long> {
 
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
        @QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")
    })
    List<PaymentInbox> findByStatus(String status, Pageable pageable);

    PaymentInbox findByTransactionId(UUID transactionId);

    boolean existsByTransactionIdAndType(UUID transactionId, Type type);
}
