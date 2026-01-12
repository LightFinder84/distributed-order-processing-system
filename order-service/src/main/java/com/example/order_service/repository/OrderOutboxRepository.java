package com.example.order_service.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import com.example.order_service.model.OrderOutbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

@Repository
public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE) // select for update
    @QueryHints({
        @QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2") // skip locked
    })
    List<OrderOutbox> findByStatus(String status, Pageable pageable);
}
