package com.example.order_service.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.order_service.model.OrderOutbox;
import com.example.order_service.repository.OrderOutboxRepository;
import com.example.order_service.service.EventProducer;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class EventProducerTest {

    @Mock
    private OrderOutboxRepository orderOutboxRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ObjectMapper jacksonJsonMapper;

    @InjectMocks
    private EventProducer eventProducer;

    @Mock
    private EventProducer self;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(eventProducer, "self", self);
        ReflectionTestUtils.setField(eventProducer, "batchSize", 10);
        ReflectionTestUtils.setField(eventProducer, "orderTopic", "order-events");
    }

    @Test
    @DisplayName("processOutbox should loop until there are no records left")
    void processOutbox_ShouldLoopUntilNoMoreRecords() {

        when(self.processBatch()).thenReturn(10).thenReturn(0);

        eventProducer.processOutbox();

        verify(self, times(2)).processBatch();
    }

    @Test
    @DisplayName("processBatch should send to kafka and update status")
    void processBatch_ShouldSendToKafkaAndUpdateStatus() {

        OrderOutbox mockingOutbox = OrderOutbox.builder()
                .status("pending")
                .transactionId(UUID.randomUUID())
                .build();
        when(orderOutboxRepository.findByStatus(eq("pending"), any(Pageable.class))).thenReturn(List.of(mockingOutbox));

        // kafka mock
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        @SuppressWarnings("unchecked")
        SendResult<String, Object> result = mock(SendResult.class);
        future.complete(result);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

        when(jacksonJsonMapper.writeValueAsString(any())).thenReturn("{}");

        int count = eventProducer.processBatch();

        assertEquals(1, count);
        assertEquals("processed", mockingOutbox.getStatus());
        verify(orderOutboxRepository).save(mockingOutbox);
    }

    @Test
    @DisplayName("processBatch should keep outbox status is pending when kafka timeout")
    void processBatch_ShouldKeepStatusPendingWhenKafkaTimeout() {
        OrderOutbox mockingOutbox = OrderOutbox.builder()
                .status("pending")
                .transactionId(UUID.randomUUID())
                .build();
        when(orderOutboxRepository.findByStatus(eq("pending"), any(Pageable.class))).thenReturn(List.of(mockingOutbox));

        // kafka mock
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

        when(jacksonJsonMapper.writeValueAsString(any())).thenReturn("{}");

        int count = eventProducer.processBatch();

        assertEquals(1, count);
        assertEquals("pending", mockingOutbox.getStatus());
        verify(orderOutboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update status to failed when got error sending to kafka")
    void processBatch_ShouldUpdateStatusWhenKafkaFailed() {

        OrderOutbox mockingOutbox = OrderOutbox.builder()
                .status("pending")
                .transactionId(UUID.randomUUID())
                .build();
        when(orderOutboxRepository.findByStatus(eq("pending"), any(Pageable.class))).thenReturn(List.of(mockingOutbox));

        when(jacksonJsonMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Mapping error"));

        int count = eventProducer.processBatch();

        assertEquals(1, count);
        assertEquals("failed", mockingOutbox.getStatus());
    }
}
