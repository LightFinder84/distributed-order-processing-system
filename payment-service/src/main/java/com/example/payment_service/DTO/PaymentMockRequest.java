package com.example.payment_service.DTO;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class PaymentMockRequest {
    private String transactionId;
    private BigDecimal amount;
    private String hookUrl;
}
