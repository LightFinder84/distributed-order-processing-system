package com.example.inventory_service.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record Event(
    UUID transactionId,
    Type type,
    OffsetDateTime createdAt,
    List<OrderItem> items
) {
    public record OrderItem(Long productId, Integer quantity, BigDecimal priceAtPurchase) {}
    public enum Type {
        ORDER_CREATED,
        INVENTORY_FAILED,
        INVENTORY_RESERVED,
        PAYMENT_SUCCESS,
        PAYMENT_FAILED
    }
}
