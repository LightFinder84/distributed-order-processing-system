package com.example.payment_simulator.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConfirmRequest {
    
    @NotNull
    private String token;

    @NotNull
    private boolean success;
}
