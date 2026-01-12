package com.example.payment_simulator;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.payment_simulator.DTO.MockRequest;

@Configuration
public class CustomConfig {
    
    @Bean
    public Map<String, MockRequest> requestStorage() {
        Map<String, MockRequest> storage = new HashMap<>();
        return storage;
    }
}
