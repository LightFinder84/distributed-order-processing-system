package com.example.order_service.DTO;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record PlaceOrderRequest(
    @NotNull(message = "Customer ID is required")
    Long customerId,

    @NotEmpty(message = "Product list must not empty")
    List<OrderItem> items
) {
    public record OrderItem(Long productId, Integer quantity) {}
}
