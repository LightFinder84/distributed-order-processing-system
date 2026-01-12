package com.example.order_service.service;

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
    private String user;
    
    @Value("${spring.datasource.password}")
    private String password;
    
    @Autowired
    private EventProcessor eventProcessor;
    
    @Autowired
    private EventProducer eventProducer;

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @PostConstruct
    public void init() throws Exception {
        startInboxListener();
        startOutboxListener();
    }

    private void startOutboxListener() {
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try (Connection conn = DriverManager.getConnection(url, user, password)) {
                    PGConnection pgConn = conn.unwrap(PGConnection.class);
                    
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("LISTEN outbox_channel");
                    }
                    
                    eventProducer.processOutbox();

                    // get notification
                    while (!Thread.currentThread().isInterrupted()) {
                        PGNotification[] notifications = pgConn.getNotifications(5000);
                        if (notifications != null && notifications.length > 0) {
                            executorService.submit(eventProducer::processOutbox);
                        }
                    }
                    
                } catch (Exception e) {
                    System.err.println("Database connection dropped, retry in 5 seconds... " + e.getMessage());
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

    private void startInboxListener() {
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try (Connection conn = DriverManager.getConnection(url, user, password)) {
                    PGConnection pgConn = conn.unwrap(PGConnection.class);

                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("LISTEN inbox_channel");
                    }

                    eventProcessor.processEvents();

                    while (!Thread.currentThread().isInterrupted()) {
                        PGNotification[] notifications = pgConn.getNotifications(5000);
                        if (notifications != null && notifications.length > 0) {
                            executorService.submit(eventProcessor::processEvents);
                        }
                    }

                } catch (Exception e) {
                    System.err.println("Mất kết nối DB Listener, đang thử lại sau 5 giây... " + e.getMessage());
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
