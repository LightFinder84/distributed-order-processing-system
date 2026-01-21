package com.example.order_service.DTO;

import java.util.Collections;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record PlaceOrderRequest(
        @NotNull(message = "Customer ID is required") Long customerId,
        @NotEmpty(message = "Product list must not empty") List<OrderItem> items) {
    public record OrderItem(Long productId, Integer quantity) {
    }

    /**
     * Compact constructor to create a defensive copy of the mutable items list when
     * record is instantiated.
     * Ensure the record doesn't hold the reference to a list that can be changed
     * by the called.
     *
     * @param customerId
     * @param items
     */
    public PlaceOrderRequest {
        items = List.copyOf(items);
    }

    /**
     * The automatically generated accessor return a direct reference.
     * This method override the default accessor to return an immutable reference.
     *
     * @return
     */
    @Override
    public List<OrderItem> items() {
        return Collections.unmodifiableList(items);
    }
}
