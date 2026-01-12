package com.example.order_service.integration;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.example.order_service.event.Event;
import com.example.order_service.event.Event.Type;
import com.example.order_service.service.EventConsumer;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
public class EventConsumerIT extends BaseIntegration {

    @MockitoSpyBean
    private KafkaTemplate<?, ?> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoSpyBean
    private EventConsumer eventConsumer;

    @Test
    @DisplayName("Should receive kafka message on payment event")
    void shouldReceiveKafkaMessageOnPaymentEvent() throws Exception {
        // arrange
        Event mockEvent = new Event(UUID.randomUUID(), Type.PAYMENT_SUCCESS, OffsetDateTime.now(), List.of());

        // act
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> template = (KafkaTemplate<String, Object>) kafkaTemplate;
        template.send("payment-events", mockEvent.transactionId().toString(),
                objectMapper.writeValueAsString(mockEvent));

        // assert
        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    verify(eventConsumer, times(1)).eventListener(any(Event.class));
                });
    }

    @Test
    @DisplayName("Should receive kafka messave on inventory event")
    void shouldReceiveKafkaMesssageOnInventoryEvent() throws Exception {
        // arrange
        Event mockEvent = new Event(UUID.randomUUID(), Type.INVENTORY_FAILED, OffsetDateTime.now(), List.of());

        // act
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> template = (KafkaTemplate<String, Object>) kafkaTemplate;
        template.send("inventory-events", mockEvent.transactionId().toString(),
                objectMapper.writeValueAsString(mockEvent));

        // assert
        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    verify(eventConsumer, times(1)).eventListener(any(Event.class));
                });
    }
}
