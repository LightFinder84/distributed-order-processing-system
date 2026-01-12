package com.example.payment_service.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class DatabaseListener {

    @Value("${spring.datasource.url}")
    private String url;
    
    @Value("${spring.datasource.username}")
    private String username;
    
    @Value("${spring.datasource.password}")
    private String password;

    @Autowired
    private PaymentInboxProcessor paymentInboxProcessor;

    @Autowired
    private EventProducer eventProducer;

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @PostConstruct
    public void init() {
        startInboxListener();
        startOutboxListener();
    }

    private void startInboxListener() {
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try (Connection connection = DriverManager.getConnection(url, username, password)) {
                    PGConnection pgConnection = connection.unwrap(PGConnection.class);
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute("LISTEN inbox_channel");
                    }
                    paymentInboxProcessor.processPaymentEvents();

                    while (!Thread.currentThread().isInterrupted()) {
                        PGNotification[] notifications = pgConnection.getNotifications();
                        if (notifications != null && notifications.length > 0) {
                            executorService.submit(paymentInboxProcessor::processPaymentEvents);
                        }
                    }
                    
                } catch (Exception e) {
                    System.err.println("Database connection dropped. Retry in 5 seconds... " + e.getMessage());
                    e.printStackTrace();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void startOutboxListener() {
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try (Connection connection = DriverManager.getConnection(url, username, password)) {
                    PGConnection pgConnection = connection.unwrap(PGConnection.class);
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute("LISTEN outbox_channel");
                    }
                    eventProducer.produce();

                    while (!Thread.currentThread().isInterrupted()) {
                        PGNotification[] notifications = pgConnection.getNotifications();
                        if (notifications != null && notifications.length > 0) {
                            executorService.submit(eventProducer::produce);
                        }
                    }

                } catch (Exception e) {
                    System.err.println("Database connection dropped. Retry in 5 seconds... " + e.getMessage());
                    e.printStackTrace();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
}
