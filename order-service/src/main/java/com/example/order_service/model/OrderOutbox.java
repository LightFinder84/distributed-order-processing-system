package com.example.order_service.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.example.order_service.event.Event;
import com.example.order_service.event.Event.Type;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Entity representing an outbox entry for event publishing.
 *
 * <p>This entity implements the Outbox Pattern to ensure reliable event publishing
 * when processing orders. Each entry in the outbox corresponds to an event that needs
 * to be published to external systems or event queues.</p>
 *
 * <p>The outbox table serves as a transactional event log where order-related events
 * are recorded. Events are typically processed asynchronously and marked as processed
 * once they have been successfully published to external systems.</p>
 *
 * <p>Events stored in the outbox include order creation events, payment events, and
 * other significant order lifecycle events that need to be communicated to other
 * services or systems.</p>
 *
 * @see Order
 * @see Event
 * @author Order Service Team
 * @version 1.0
 */
@Entity
@Getter
@Table(name = "outbox")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderOutbox {

    /**
     * The unique identifier for this outbox entry.
     *
     * <p>This is the primary key of the outbox table, generated automatically
     * using database identity columns.</p>
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The transaction ID associated with this outbox event.
     *
     * <p>This UUID uniquely identifies the business transaction that triggered
     * the creation of this outbox event, enabling correlation across multiple
     * services and systems.</p>
     */
    @Column(name = "transaction_id")
    private UUID transactionId;

    /**
     * The type of event stored in this outbox entry.
     *
     * <p>The event type determines how the event should be processed and routed
     * to the appropriate event handlers or external systems.</p>
     *
     * @see Event.Type
     */
    @Column(name = "event_type", nullable = false)
    private Type type;

    /**
     * The processing status of this outbox event.
     *
     * <p>Valid values include:
     * <ul>
     *   <li>"pending" - Event has not yet been processed</li>
     *   <li>"processed" - Event has been successfully published</li>
     *   <li>"failed" - Event processing failed</li>
     * </ul>
     * Default value is "pending".</p>
     */
    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "pending";

    /**
     * The event payload containing the details to be published.
     *
     * <p>This field stores the complete event data in JSON format (jsonb in PostgreSQL).
     * The payload includes all necessary information about the order and items that
     * need to be communicated to downstream systems.</p>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Event payload;

    /**
     * The timestamp when this outbox entry was created.
     *
     * <p>This field is automatically set to the current time when the outbox
     * entry is created and is useful for tracking event age and ordering events
     * chronologically.</p>
     */
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    /**
     * Factory method to create an OrderOutbox entry from an Order.
     *
     * <p>This method constructs a new outbox entry with an ORDER_CREATED event
     * containing all the order items and their details. The event payload is
     * built from the order's items, including product IDs, quantities, and
     * prices at the time of purchase.</p>
     *
     * @param order the Order from which to create the outbox entry, must not be null
     * @return a new OrderOutbox instance with the order data embedded as an event payload
     */
    public static OrderOutbox create(final Order order) {
        // create message
        List<Event.OrderItem> items = order.getItems().stream()
                .map(item -> new Event.OrderItem(item.getProduct().getProductId(), item.getQuantity(),
                        item.getPriceAtPurchase()))
                .toList();
        Event payload = new Event(order.getTransactionId(), Type.ORDER_CREATED,
                OffsetDateTime.now(), items);

        OrderOutbox outbox = OrderOutbox.builder()
                .payload(payload)
                .transactionId(order.getTransactionId())
                .type(payload.type())
                .build();
        return outbox;
    }

    /**
     * Marks this outbox entry as successfully processed.
     *
     * <p>This method should be called after the event has been successfully
     * published to external systems. The status is updated to "processed" to
     * indicate that this event no longer needs to be retried or republished.</p>
     */
    public final void markAsProcessed() {
        this.status = "processed";
    }

    /**
     * Marks this outbox entry as failed.
     *
     * <p>This method should be called when the event publishing has failed
     * after all retry attempts have been exhausted. The status is updated to
     * "failed" to indicate that manual intervention or investigation may be
     * required.</p>
     */
    public final void markAsFailed() {
        this.status = "failed";
    }
}
