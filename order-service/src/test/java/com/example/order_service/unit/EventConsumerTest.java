package com.example.order_service.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.order_service.event.Event;
import com.example.order_service.event.Event.Type;
import com.example.order_service.repository.OrderInboxRepository;
import com.example.order_service.service.EventConsumer;

@ExtendWith(MockitoExtension.class)
public class EventConsumerTest {

    @Mock
    private OrderInboxRepository orderInboxRepository;

    @InjectMocks
    private EventConsumer eventConsumer;

    @Test
    @DisplayName("Should not save Inventory reserved event to db")
    void shouldSkipInventoryReservedEvent() {
        // arrange
        Event mockEvent = new Event(UUID.randomUUID(), Type.INVENTORY_RESERVED, OffsetDateTime.now(), List.of());
        // act
        eventConsumer.eventListener(mockEvent);
        // assert
        verify(orderInboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should skip duplicated transaction id and type")
    void shouldSkipDuplicatedTransactionIDAndType() {
        // arrange
        Event mockEvent = new Event(UUID.randomUUID(), Type.PAYMENT_SUCCESS, OffsetDateTime.now(), List.of());
        when(orderInboxRepository.existsByTransactionIdAndType(mockEvent.transactionId(), mockEvent.type()))
                .thenReturn(true);

        // act
        eventConsumer.eventListener(mockEvent);

        // assert
        verify(orderInboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should store event to database")
    void shouldStoreEventToDb() {
        // arrange
        Event mockEvent = new Event(UUID.randomUUID(), Type.PAYMENT_SUCCESS, OffsetDateTime.now(), List.of());
        when(orderInboxRepository.existsByTransactionIdAndType(mockEvent.transactionId(), mockEvent.type()))
                .thenReturn(false);
        // act
        eventConsumer.eventListener(mockEvent);

        // assert
        verify(orderInboxRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should throw DataIntegrityViolationException when storing duplicate key")
    void shouldThrowDataIntegrityViolationException_WhenStoringDuplicateKey() {
        // arrange
        Event mockEvent = new Event(UUID.randomUUID(), Type.PAYMENT_SUCCESS, OffsetDateTime.now(), List.of());

        when(orderInboxRepository.existsByTransactionIdAndType(mockEvent.transactionId(), mockEvent.type()))
                .thenReturn(false);
        when(orderInboxRepository.save(any())).thenThrow(new DataIntegrityViolationException("Duplicate key"));

        // act & assert
        assertDoesNotThrow(() -> {
            eventConsumer.eventListener(mockEvent);
        });
        verify(orderInboxRepository, times(1)).save(any());
    }
}
