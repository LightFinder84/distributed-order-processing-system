package com.example.payment_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import com.example.payment_service.model.PaymentOutbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

@Repository
public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
        @QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")
    })
    List<PaymentOutbox> findByStatus(String status);
}
