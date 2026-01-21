package com.example.order_service.DTO;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.example.order_service.model.Order;

/**
 * Data Transfer Object representing the response for a place order request.
 *
 * This record encapsulates the order confirmation details returned to the client
 * after successfully placing an order. It contains the customer identifier,
 * order identifier, transaction identifier, and the list of ordered items.
 *
 * @param customerId the unique identifier of the customer who placed the order
 * @param orderId the unique identifier of the placed order
 * @param transactionId the unique transaction identifier for tracking purposes
 * @param items the list of items included in the order
 *
 * @author Order Service Team
 * @version 1.0
 */
public record PlaceOrderResponse(
        Long customerId,
        Long orderId,
        UUID transactionId,
        List<OrderItem> items) {
    /**
     * Nested record representing a single item in the order response.
     *
     * This record holds minimal information about each product ordered,
     * including the product identifier and the quantity ordered.
     *
     * @param productId the unique identifier of the product
     * @param quantity the number of units ordered for this product
     *
     * @author Order Service Team
     * @version 1.0
     */
    public record OrderItem(Long productId, Integer quantity) {
    }

    /**
     * Compact constructor for validating and ensuring immutability of items.
     *
     * This constructor creates a defensive copy of the items list to ensure
     * that the record remains immutable and cannot be modified through external
     * references to the original list.
     *
     * @throws NullPointerException if any of the fields are null
     */
    public PlaceOrderResponse {
        items = List.copyOf(items);
    }

    /**
     * Constructs a PlaceOrderResponse from an Order entity.
     *
     * This constructor transforms an Order domain object into a response DTO
     * by extracting the customer ID, order ID, transaction ID, and converting
     * the order items into the corresponding OrderItem records. Each item is
     * mapped to extract the product ID and quantity.
     *
     * @param order the Order entity to convert into a response
     *
     * @throws NullPointerException if order, customer, or items are null
     */
    public PlaceOrderResponse(final Order order) {
        this(
            order.getCustomer().getCustomerId(),
            order.getOrderId(),
            order.getTransactionId(),
            order.getItems().stream()
                    .map(item -> new OrderItem(item.getProduct().getProductId(), item.getQuantity()))
                    .toList());
    }

    /**
     * Returns an unmodifiable view of the items in this order response.
     *
     * This method overrides the default accessor to provide an unmodifiable list,
     * preventing external code from modifying the order items after the response
     * is created.
     *
     * @return an unmodifiable list of OrderItem objects
     */
    @Override
    public List<OrderItem> items() {
        return Collections.unmodifiableList(items);
    }
}
