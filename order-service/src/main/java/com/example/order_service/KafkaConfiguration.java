package com.example.order_service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.converter.JacksonJsonMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;

@Configuration
public class KafkaConfiguration {
    
    @Bean
    public RecordMessageConverter converter() {
        return new JacksonJsonMessageConverter();
    }
}
