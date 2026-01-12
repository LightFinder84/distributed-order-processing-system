package com.example.payment_service.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "payment")
public class Payment {
    
    @Id
    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "hook_url", nullable = false)
    private String hookUrl;

    @Column(name = "payment_site", nullable = true)
    private String paymentSite;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "status", nullable = false)
    private String status = "created"; // created, pending, completed, failed, expired

    @Column(name = "last_updated_at", nullable = false)
    private OffsetDateTime lastUdatedAt;
}
