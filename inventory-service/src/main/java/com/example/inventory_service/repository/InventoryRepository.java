package com.example.inventory_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.inventory_service.model.Inventory;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @Modifying
    @Query(value = "update Inventory i set i.quantity = i.quantity - :quantity where i.productId = :productId and i.quantity >= :quantity")
    int deductStock(Integer quantity, Long productId);

    @Modifying
    @Query(value = "update Inventory i set i.quantity = i.quantity + :quantity where i.productId = :productId")
    void addStock(Integer quantity, Long productId);
}
