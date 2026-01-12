package com.example.order_service.DTO;

import java.util.List;
import java.util.UUID;

import com.example.order_service.model.Order;

public record PlaceOrderResponse(
        Long customerId,
        Long orderId,
        UUID transactionId,
        List<OrderItem> items) {
    public record OrderItem(Long productId, Integer quantity) {
    }

    public PlaceOrderResponse(Order order) {
        this(
            order.getCustomer().getCustomerId(),
            order.getOrderId(),
            order.getTransactionId(),
            order.getItems().stream()
                    .map(item -> new OrderItem(item.getProduct().getProductId(), item.getQuantity()))
                    .toList());
    }
}
