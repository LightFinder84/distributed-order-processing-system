package com.example.order_service.integration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.order_service.event.Event;
import com.example.order_service.model.Order;
import com.example.order_service.model.OrderInbox;
import com.example.order_service.repository.OrderInboxRepository;
import com.example.order_service.repository.OrderRepository;

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
public class EventProcessorIT extends BaseIntegration {

    @Autowired
    private OrderInboxRepository orderInboxRepository;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    @AfterAll
    void tearDown() {
        orderInboxRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("should skip inbox when not found order")
    void shouldSkipInbox_WhenNotFoundOrder() {
        // arrange
        UUID transactionId = UUID.randomUUID();
        Event payload = new Event(transactionId, Event.Type.PAYMENT_SUCCESS, OffsetDateTime.now(), List.of());
        OrderInbox inbox = OrderInbox.builder().transactionId(transactionId).type(Event.Type.PAYMENT_SUCCESS)
                .status("pending").payload(payload).createdAt(OffsetDateTime.now()).build();

        // act (insert to inbox)
        orderInboxRepository.save(inbox);

        // assert
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            List<OrderInbox> all = orderInboxRepository.findAll();
            assertEquals(1, all.size());
            assertEquals("skipped", all.get(0).getStatus());
        });
    }

    @Test
    @DisplayName("should cancel when recieve inventory failed")
    void shouldCancelOrder_WhenReceiveInventoryFailed() {
        // arrange
        UUID transactionId = UUID.randomUUID();
        Event payload = new Event(transactionId, Event.Type.INVENTORY_FAILED, OffsetDateTime.now(), List.of());
        OrderInbox inbox = OrderInbox.builder().transactionId(transactionId).type(Event.Type.INVENTORY_FAILED)
                .status("pending").payload(payload).createdAt(OffsetDateTime.now()).build();
        Order order = Order.builder().transactionId(transactionId).build();
        orderRepository.save(order);

        // act (insert to inbox)
        orderInboxRepository.save(inbox);

        // assert
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            List<OrderInbox> allInbox = orderInboxRepository.findAll();
            assertEquals(1, allInbox.size());
            assertEquals("processed", allInbox.get(0).getStatus());

            List<Order> allOrders = orderRepository.findAll();
            assertEquals(1, allOrders.size());
            assertEquals("cancelled", allOrders.get(0).getStatus());
        });
    }

    @Test
    @DisplayName("should cancel when receive payment failed")
    void shouldCancelOrder_WhenReceivePaymentFailed() {
        // arrange
        UUID transactionId = UUID.randomUUID();
        Event payload = new Event(transactionId, Event.Type.PAYMENT_FAILED, OffsetDateTime.now(), List.of());
        OrderInbox inbox = OrderInbox.builder().transactionId(transactionId).type(Event.Type.PAYMENT_FAILED)
                .status("pending").payload(payload).createdAt(OffsetDateTime.now()).build();
        Order order = Order.builder().transactionId(transactionId).build();
        orderRepository.save(order);

        // act (insert to inbox)
        orderInboxRepository.save(inbox);

        // assert
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            List<OrderInbox> allInbox = orderInboxRepository.findAll();
            assertEquals(1, allInbox.size());
            assertEquals("processed", allInbox.get(0).getStatus());

            List<Order> allOrders = orderRepository.findAll();
            assertEquals(1, allOrders.size());
            assertEquals("cancelled", allOrders.get(0).getStatus());
        });
    }

    @Test
    @DisplayName("should complete when receive payment success")
    void shouldCompleteOrder_WhenReceivePaymentSuccess() {
        // arrange
        UUID transactionId = UUID.randomUUID();
        Event payload = new Event(transactionId, Event.Type.PAYMENT_SUCCESS, OffsetDateTime.now(), List.of());
        OrderInbox inbox = OrderInbox.builder().transactionId(transactionId).type(Event.Type.PAYMENT_SUCCESS)
                .status("pending").payload(payload).createdAt(OffsetDateTime.now()).build();
        Order order = Order.builder().transactionId(transactionId).build();
        orderRepository.save(order);

        // act (insert to inbox)
        orderInboxRepository.save(inbox);

        // assert
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            List<OrderInbox> allInbox = orderInboxRepository.findAll();
            assertEquals(1, allInbox.size());
            assertEquals("processed", allInbox.get(0).getStatus());

            List<Order> allOrders = orderRepository.findAll();
            assertEquals(1, allOrders.size());
            assertEquals("completed", allOrders.get(0).getStatus());
        });
    }
}
