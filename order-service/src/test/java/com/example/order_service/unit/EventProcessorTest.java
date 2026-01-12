package com.example.order_service.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.order_service.event.Event;
import com.example.order_service.model.Order;
import com.example.order_service.model.OrderInbox;
import com.example.order_service.repository.OrderInboxRepository;
import com.example.order_service.repository.OrderRepository;
import com.example.order_service.service.EventProcessor;

@ExtendWith(MockitoExtension.class)
public class EventProcessorTest {

    @Mock
    private OrderInboxRepository orderInboxRepository;

    @Mock
    private OrderRepository orderRepository;

    @Spy
    @InjectMocks
    private EventProcessor eventProcessor;

    @BeforeEach
    void beforeEach() {
        ReflectionTestUtils.setField(eventProcessor, "self", eventProcessor);
        ReflectionTestUtils.setField(eventProcessor, "batchSize", 10);
    }

    @Test
    @DisplayName("processEvents should call processBatch until there are no records left")
    void processEvents_ShouldLoopUntilNoRecordsLeft() {
        // arrange
        doReturn(10).doReturn(5).doReturn(0)
                .when(eventProcessor).processBatch();
        // act
        eventProcessor.processEvents();
        // assert
        verify(eventProcessor, times(3)).processBatch();
    }

    @Test
    @DisplayName("processBatch should mark inbox as skipped when order is not found")
    void shouldMarkInboxAsSkipped_WhenOrderNotfound() {
        // arrange
        OrderInbox mockInbox = OrderInbox.builder().transactionId(UUID.randomUUID()).status("pending").build();
        when(orderInboxRepository.findByStatus(eq("pending"), any(Pageable.class))).thenReturn(List.of(mockInbox));

        when(orderRepository.findByTransactionId(any())).thenReturn(null);

        // act
        eventProcessor.processBatch();

        // assert
        assertEquals("skipped", mockInbox.getStatus());
        verify(orderInboxRepository, times(1)).save(mockInbox);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("processBatch should mark order as cancelled on inventory failed")
    void shouldMarkOrderAsCancelled_WhenInventoryFailed() {
        // arrange
        Event mockEvent = new Event(null, Event.Type.INVENTORY_FAILED, null, List.of());

        OrderInbox mockInbox = OrderInbox.builder().status("pending").payload(mockEvent).build();
        when(orderInboxRepository.findByStatus(eq("pending"), any(Pageable.class))).thenReturn(List.of(mockInbox));

        Order mockOrder = new Order();
        when(orderRepository.findByTransactionId(any())).thenReturn(mockOrder);

        // act
        eventProcessor.processBatch();

        // assert
        assertEquals("processed", mockInbox.getStatus());
        verify(orderInboxRepository, times(1)).save(mockInbox);

        assertEquals("cancelled", mockOrder.getStatus());
        verify(orderRepository, times(1)).save(mockOrder);
    }

    @Test
    @DisplayName("processBatch should mark order as cancelled on payment failed")
    void shouldMarkOrderAsCancelled_WhenPaymentFailed() {
        // arrange
        Event mockEvent = new Event(null, Event.Type.PAYMENT_FAILED, null, List.of());

        OrderInbox mockInbox = OrderInbox.builder().status("pending").payload(mockEvent).build();
        when(orderInboxRepository.findByStatus(eq("pending"), any(Pageable.class))).thenReturn(List.of(mockInbox));

        Order mockOrder = new Order();
        when(orderRepository.findByTransactionId(any())).thenReturn(mockOrder);

        // act
        eventProcessor.processBatch();

        // assert
        assertEquals("processed", mockInbox.getStatus());
        verify(orderInboxRepository, times(1)).save(mockInbox);

        assertEquals("cancelled", mockOrder.getStatus());
        verify(orderRepository, times(1)).save(mockOrder);
    }

    @Test
    @DisplayName("processBatch should mark order as completed on payment success")
    void shouldMarkOrderAsCompleted_WhenPaymentSuccess() {
        // arrange
        Event mockEvent = new Event(null, Event.Type.PAYMENT_SUCCESS, null, List.of());

        OrderInbox mockInbox = OrderInbox.builder().status("pending").payload(mockEvent).build();
        when(orderInboxRepository.findByStatus(eq("pending"), any(Pageable.class))).thenReturn(List.of(mockInbox));

        Order mockOrder = new Order();
        when(orderRepository.findByTransactionId(any())).thenReturn(mockOrder);

        // act
        eventProcessor.processBatch();

        // assert
        assertEquals("processed", mockInbox.getStatus());
        verify(orderInboxRepository, times(1)).save(mockInbox);

        assertEquals("completed", mockOrder.getStatus());
        verify(orderRepository, times(1)).save(mockOrder);
    }
}
