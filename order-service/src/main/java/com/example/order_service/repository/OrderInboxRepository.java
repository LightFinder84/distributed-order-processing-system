package com.example.order_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import com.example.order_service.event.Event.Type;
import com.example.order_service.model.OrderInbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

@Repository
public interface OrderInboxRepository extends JpaRepository<OrderInbox, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE) // select for update
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2") // skip locked
    })
    List<OrderInbox> findByStatus(String status, Pageable pageable);

    List<OrderInbox> findByTransactionId(UUID transactionId);

    boolean existsByTransactionIdAndType(UUID transactionId, Type type);
}
