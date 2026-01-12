package com.example.payment_simulator.DTO;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MockRequest {

    @NotNull(message = "Transaction ID is required")
    private String transactionId;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotNull(message = "Transaction status is required")
    private String transactionStatus = "pending";

    @NotNull(message = "Webhook URL is required")
    private String hookUrl;
}
