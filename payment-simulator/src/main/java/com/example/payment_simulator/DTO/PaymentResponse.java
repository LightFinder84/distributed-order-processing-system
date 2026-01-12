package com.example.payment_simulator.DTO;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class PaymentResponse {
    private String transactionId;
    private String status = "pending";
    private BigDecimal amount;
}
