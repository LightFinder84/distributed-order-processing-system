package com.example.order_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public final class OrderServiceApplication {

    private OrderServiceApplication() {
    }

    /**
     * Application main method.
     *
     * @param args
     */
    public static void main(final String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
