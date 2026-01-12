package com.example.order_service.integration;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.order_service.service.EventProcessor;
import com.example.order_service.service.EventProducer;

@SpringBootTest
public class DatabaseListenerIT extends BaseIntegration {

    @MockitoBean
    private EventProcessor eventProcessor;

    @MockitoBean
    private EventProducer eventProducer;

    @Test
    @DisplayName("Should trigger event producer when notify received")
    void shouldTriggerEventProducer_WhenNotifyReceived() throws Exception {

        // wait for channel to be ready
        Thread.sleep(2000);

        try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(),
                postgres.getPassword())) {
            Statement stmt = conn.createStatement();
            stmt.execute("NOTIFY outbox_channel");
        }

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            verify(eventProducer, atLeast(1)).processOutbox();
        });
    }

    @Test
    @DisplayName("Should trigger event processor when notify received")
    void shouldTriggerEventProcessor_WhenNotifyReceived() throws Exception {
        // wait for channel to be ready
        Thread.sleep(2000);

        try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(),
                postgres.getPassword())) {
            Statement stmt = conn.createStatement();
            stmt.execute("NOTIFY inbox_channel");
        }

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            verify(eventProcessor, atLeast(1)).processEvents();
        });
    }
}
