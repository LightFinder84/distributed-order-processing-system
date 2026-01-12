package com.example.payment_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import com.example.payment_service.model.Payment;

import jakarta.persistence.QueryHint;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @QueryHints({
        @QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")
    })
    Payment findByTransactionId(UUID id);
    
    @QueryHints({
        @QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")
    })
    List<Payment> findByStatus(String status, Pageable pageable);
}
