package com.example.payment_service.DTO;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WebhookRequest {

    @NotNull
    private UUID transactionId;

    @NotNull
    private String status;

    @NotNull
    private BigDecimal amount;
}
