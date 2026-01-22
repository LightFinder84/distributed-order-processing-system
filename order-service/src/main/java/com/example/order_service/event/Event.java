package com.example.order_service.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Domain event representing a state change in the order processing workflow.
 *
 * This record encapsulates events that occur during order processing, including
 * order creation, inventory operations, and payment processing. Each event is
 * uniquely identified by a transaction ID and timestamped for audit purposes.
 *
 * @param transactionId the unique transaction identifier for tracing the event
 * @param type the type of event indicating what operation occurred
 * @param createdAt the timestamp when the event was created
 * @param items the list of items associated with this event
 *
 * @author Order Service Team
 * @version 1.0
 */
public record Event(
    UUID transactionId,
    Type type,
    OffsetDateTime createdAt,
    List<OrderItem> items
) {
    /**
     * Nested record representing a single item associated with an event.
     *
     * This record holds comprehensive information about a product in the context
     * of an event, including the product identifier, quantity, and the price at
     * the time of purchase for audit and historical tracking.
     *
     * @param productId the unique identifier of the product
     * @param quantity the number of units involved in this event
     * @param priceAtPurchase the price of the product at the time of the event
     *
     * @author Order Service Team
     * @version 1.0
     */
    public record OrderItem(Long productId, Integer quantity, BigDecimal priceAtPurchase) {
    }

    /**
     * Enumeration of event types in the order processing workflow.
     *
     * Represents the various types of events that can occur during order processing,
     * including order creation, inventory management, and payment operations.
     *
     * @author Order Service Team
     * @version 1.0
     */
    public enum Type {
        /**
         * Order is created.
         */
        ORDER_CREATED,
        /**
         * Inventory failed on deduction.
         */
        INVENTORY_FAILED,
        /**
         * Inventory succeeded on dedection.
         */
        INVENTORY_RESERVED,
        /**
         * Payment completed.
         */
        PAYMENT_SUCCESS,
        /**
         * Payment failed.
         */
        PAYMENT_FAILED
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
    public Event {
        items = List.copyOf(items);
    }

    /**
     * Returns an unmodifiable view of the items in this event.
     *
     * This method overrides the default accessor to provide an unmodifiable list,
     * preventing external code from modifying the event items after the event
     * is created.
     *
     * @return an unmodifiable list of OrderItem objects
     */
    @Override
    public List<OrderItem> items() {
        return Collections.unmodifiableList(items);
    }
}
