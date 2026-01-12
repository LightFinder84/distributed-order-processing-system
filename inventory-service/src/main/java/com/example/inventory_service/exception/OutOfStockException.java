package com.example.inventory_service.exception;

public class OutOfStockException extends Exception {
    
    public OutOfStockException(String message) {
        super(message);
    }
}
