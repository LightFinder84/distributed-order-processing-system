package com.example.order_service.integration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.example.order_service.event.Event;
import com.example.order_service.model.OrderOutbox;
import com.example.order_service.repository.OrderOutboxRepository;
import com.example.order_service.service.EventProducer;

@SpringBootTest
public class EventProducerIT extends BaseIntegration {

    @Autowired
    private OrderOutboxRepository orderOutboxRepository;

    @MockitoSpyBean
    private EventProducer eventProducer;

    @MockitoSpyBean
    private KafkaTemplate<?, ?> kafkaTemplate;

    @SuppressWarnings("unchecked")
    @BeforeEach
    @AfterEach
    void tearDown() {
        Mockito.reset(kafkaTemplate);
        orderOutboxRepository.deleteAll();
    }

    @Test
    @DisplayName("should update data when send message success")
    void shouldUpdateData_WhenSendMessageSuccess() {
        // arrange
        UUID transactionId = UUID.randomUUID();
        Event payload = new Event(transactionId, Event.Type.ORDER_CREATED, OffsetDateTime.now(), List.of());
        OrderOutbox outbox = new OrderOutbox(null, transactionId, Event.Type.ORDER_CREATED, "pending", payload,
                OffsetDateTime.now());

        // act: database listener automatically call producer
        orderOutboxRepository.save(outbox);

        // assert
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<OrderOutbox> all = orderOutboxRepository.findAll();
            assertEquals(1, all.size());
            assertEquals("processed", all.get(0).getStatus());
        });
    }

    // @Test
    // @DisplayName("should not update data when send message timeout")
    // void shouldNotUpdateData_WhenSendMessageTimeout() throws Exception {
    //     // arrange
    //     UUID transactionId = UUID.randomUUID();
    //     Event payload = new Event(transactionId, Event.Type.ORDER_CREATED, OffsetDateTime.now(), List.of());
    //     OrderOutbox outbox = new OrderOutbox(null, transactionId, Event.Type.ORDER_CREATED, "pending", payload,
    //             OffsetDateTime.now());

    //     doAnswer(invocation -> {
    //         CompletableFuture<?> realFuture = (CompletableFuture<?>) invocation.callRealMethod();
    //         CompletableFuture<?> spyFuture = spy(realFuture);
    //         doThrow(new TimeoutException("Simulated timeout")).when(spyFuture).get(anyLong(), any(TimeUnit.class));
    //         return spyFuture;
    //     }).when(kafkaTemplate).send(anyString(), any(), any());

    //     Thread.sleep(2000);

    //     // act: database listener automatically call producer
    //     orderOutboxRepository.save(outbox);

    //     // assert
    //     await().during(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
    //         List<OrderOutbox> all = orderOutboxRepository.findAll();
    //         assertEquals(1, all.size());
    //         assertEquals("pending", all.get(0).getStatus());
    //     });
    // }

    // @Test
    // @DisplayName("should not update data when send message failed")
    // void shouldNotUpdateData_WhenSendMessageFailed() {
    //     // arrange
    //     UUID transactionId = UUID.randomUUID();
    //     Event payload = new Event(transactionId, Event.Type.ORDER_CREATED, OffsetDateTime.now(), List.of());
    //     OrderOutbox outbox = new OrderOutbox(null, transactionId, Event.Type.ORDER_CREATED, "pending", payload,
    //             OffsetDateTime.now());

    //     doAnswer(invocation -> {
    //         CompletableFuture<?> realFuture = (CompletableFuture<?>) invocation.callRealMethod();
    //         CompletableFuture<?> spyFuture = spy(realFuture);
    //         doThrow(new KafkaException("Simulated kafka exception")).when(spyFuture).get(anyLong(), any(TimeUnit.class));
    //         return spyFuture;
    //     }).when(kafkaTemplate).send(anyString(), any(), any());

    //     // act: database listener automatically call producer
    //     orderOutboxRepository.save(outbox);

    //     // assert
    //     await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
    //         List<OrderOutbox> all = orderOutboxRepository.findAll();
    //         assertEquals(1, all.size());
    //         assertEquals("failed", all.get(0).getStatus());
    //     });
    // }
}
