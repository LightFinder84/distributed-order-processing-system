package com.example.inventory_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.inventory_service.event.Event;
import com.example.inventory_service.event.Event.OrderItem;
import com.example.inventory_service.exception.OutOfStockException;
import com.example.inventory_service.repository.InventoryRepository;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@Service
public class StockService {

    @Autowired
    private InventoryRepository inventoryRepository;
    
    @Transactional(rollbackOn = OutOfStockException.class, value = TxType.REQUIRES_NEW)
    public void deductStock(Event payload) throws OutOfStockException {
        for (Event.OrderItem item : payload.items()) {
            int count = inventoryRepository.deductStock(item.quantity(), item.productId());
            if (count == 0) {
                throw new OutOfStockException("Product is out of stock. Product ID: " + item.productId());
            }
        }
    }

    @Transactional(value = TxType.REQUIRED)
    public void addStock(Event payload) {
        List<OrderItem> items = payload.items();
        for (OrderItem orderItem : items) {
            inventoryRepository.addStock(orderItem.quantity(), orderItem.productId());
        }
    }
}
