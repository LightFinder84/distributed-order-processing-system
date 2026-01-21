package com.example.order_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.example.order_service.event.Event;
import com.example.order_service.event.Event.Type;
import com.example.order_service.model.OrderInbox;
import com.example.order_service.repository.OrderInboxRepository;

import jakarta.transaction.Transactional;

/**
 * Service for consuming events from Kafka topics.
 *
 * <p>This service listens to events published from other microservices (payment and
 * inventory services) on designated Kafka topics and stores them in the inbox table
 * for reliable processing. The service implements the Inbox Pattern to ensure
 * at-least-once processing semantics.</p>
 *
 * <p>The event consumer is responsible for:</p>
 * <ul>
 *   <li>Listening to payment and inventory events from Kafka</li>
 *   <li>Filtering out non-essential events (e.g., success confirmations)</li>
 *   <li>Ensuring idempotent processing through transaction ID and event type checks</li>
 *   <li>Persisting events to the inbox table for asynchronous processing</li>
 *   <li>Handling duplicate event detection and constraint violations gracefully</li>
 * </ul>
 *
 * <p>The consumer works in conjunction with {@link EventProcessor} which retrieves
 * and processes inbox events, ensuring reliable event handling across services.</p>
 *
 * @see EventProcessor
 * @see OrderInbox
 * @author Order Service Team
 * @version 1.0
 */
@Service
public class EventConsumer {

    /**
     * Repository for persisting and querying inbox entries.
     */
    @Autowired
    private OrderInboxRepository orderInboxRepository;

    /**
     * Listens to and consumes events from Kafka topics.
     *
     * <p>This method is invoked automatically by Spring Kafka when messages arrive
     * on the configured payment and inventory topics. It processes incoming events
     * and stores them in the inbox for reliable handling.</p>
     *
     * <p>The method implements idempotency by:</p>
     * <ul>
     *   <li>Skipping INVENTORY_RESERVED success events (no further processing needed)</li>
     *   <li>Checking if the event has already been processed using transaction ID and type</li>
     *   <li>Catching database constraint violations to handle race conditions</li>
     * </ul>
     *
     * @param event the event received from Kafka containing transaction and operation details
     */
    @KafkaListener(topics = {
            "${application.topic.payment}",
            "${application.topic.inventory}"
    })
    @Transactional
    public void eventListener(final Event event) {
        if (event.type() == Type.INVENTORY_RESERVED) {
            return; // skip success event
        }
        if (orderInboxRepository.existsByTransactionIdAndType(event.transactionId(), event.type())) {
            return;
        }
        // store to database
        try {
            OrderInbox inbox = OrderInbox.builder().payload(event).transactionId(event.transactionId())
                    .type(event.type()).build();

            orderInboxRepository.save(inbox);
        } catch (DataIntegrityViolationException e) {
            System.out.println("Duplicate detected by DB constraint for: " + event.transactionId());
        }
    }
}
