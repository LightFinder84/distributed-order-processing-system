package com.example.inventory_service.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import com.example.inventory_service.model.InventoryOutbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

@Repository
public interface InventoryOutboxRepository extends JpaRepository<InventoryOutbox, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
        @QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")
    })
    List<InventoryOutbox> findByStatus(String status, Pageable pageable);
}
