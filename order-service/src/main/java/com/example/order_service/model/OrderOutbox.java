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

@Entity
@Getter
@Table(name = "outbox")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderOutbox {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "event_type", nullable = false)
    private Type type;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "pending";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Event payload;   

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public static OrderOutbox create(Order order) {
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

    public void markAsProcessed() {
        this.status = "processed";
    }

    public void markAsFailed() {
        this.status = "failed";
    }
}