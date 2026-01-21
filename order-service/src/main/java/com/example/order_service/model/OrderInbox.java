package com.example.order_service.model;

import java.time.OffsetDateTime;
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
@Table(name = "inbox")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderInbox {

    /**
     * Unique idientifier for order inbox.
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Order's unique transaction ID.
     */
    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    /**
     * Incoming event type.
     */
    @Column(name = "event_type", nullable = false)
    private Type type;

    /**
     * Event processing status.
     */
    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "pending";

    /**
     * Event payload.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Event payload;

    /**
     * Created time.
     */
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    /**
     * Mark event as "skipped".
     */
    public final void markAsSkipped() {
        this.status = "skipped";
    }

    /**
     * Mark event as "processed".
     */
    public final void markAsProcessed() {
        this.status = "processed";
    }
}
