package com.example.order_service.service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.example.order_service.model.OrderOutbox;
import com.example.order_service.repository.OrderOutboxRepository;

import jakarta.transaction.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Service for producing and publishing events from the outbox.
 *
 * <p>This service retrieves events that were previously stored in the outbox table
 * and publishes them to a Kafka topic for consumption by other microservices.
 * It implements the Outbox Pattern to ensure reliable exactly-once event publishing semantics.</p>
 *
 * <p>The event producer is responsible for:</p>
 * <ul>
 *   <li>Retrieving pending events from the outbox in batches</li>
 *   <li>Publishing events to the configured Kafka topic</li>
 *   <li>Handling publishing failures gracefully (timeouts and other exceptions)</li>
 *   <li>Marking events as processed or failed after publish attempts</li>
 *   <li>Processing events in a transactional manner to maintain consistency</li>
 * </ul>
 *
 * <p>The producer uses the Jackson ObjectMapper to serialize events to JSON before
 * publishing to Kafka. It includes timeout handling for publish operations and
 * differentiates between timeout/interrupt failures (skipped for retry) and other
 * exceptions (marked as failed).</p>
 *
 * @see OrderOutbox
 * @author Order Service Team
 * @version 1.0
 */
@Service
public class EventProducer {

    /**
     * Kafka publish timeout in seconds.
     */
    private static final int KAFKA_TIMEOUT = 5;

    /**
     * Repository for querying and updating outbox entries.
     */
    @Autowired
    private OrderOutboxRepository orderOutboxRepository;

    /**
     * Kafka template for publishing messages to Kafka topics.
     */
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Jackson ObjectMapper for serializing events to JSON.
     */
    @Autowired
    private ObjectMapper jacksonJsonMapper;

    /**
     * Self-reference to this service, lazily injected for use in transactional method calls.
     * This allows circumventing Spring's transaction proxy to properly handle batch processing.
     */
    @Autowired
    @Lazy
    private EventProducer self;

    /**
     * Batch size for processing outbox events in a single transaction.
     * Configured via {@code application.batch-size} property.
     */
    @Value("${application.batch-size}")
    private int batchSize;

    /**
     * Kafka topic name for publishing order events.
     * Configured via {@code application.topic.order} property.
     */
    @Value("${application.topic.order}")
    private String orderTopic;

    /**
     * Processes all pending events in the outbox.
     *
     * <p>This method continuously processes and publishes events in batches until
     * no more pending events remain. It uses a self-reference to ensure that each
     * batch is processed within its own transaction boundary.</p>
     */
    public void processOutbox() {
        int count = self.processBatch();
        while (count != 0) {
            count = self.processBatch();
        }
    }

    /**
     * Processes a single batch of pending outbox events.
     *
     * <p>This method retrieves up to {@code batchSize} pending events from the outbox,
     * sorted by creation time in ascending order. Each event in the batch is published
     * to Kafka with the transaction ID as the message key.</p>
     *
     * <p>Error handling:</p>
     * <ul>
     *   <li>TimeoutException or InterruptedException - Event is skipped for potential retry</li>
     *   <li>Other exceptions - Event is marked as failed and an error is logged</li>
     * </ul>
     *
     * <p>The method is transactional to ensure that status updates are persisted atomically.</p>
     *
     * @return the number of events processed in this batch; returns 0 when no pending events remain
     */
    @Transactional
    public int processBatch() {
        Pageable pageable = PageRequest.of(0, batchSize, Sort.by("createdAt").ascending());
        List<OrderOutbox> pendingPayloads = orderOutboxRepository.findByStatus("pending", pageable);
        for (OrderOutbox pendingOutbox : pendingPayloads) {
            try {
                kafkaTemplate
                        .send(orderTopic, pendingOutbox.getTransactionId().toString(),
                                jacksonJsonMapper.writeValueAsString(pendingOutbox.getPayload()))
                        .get(KAFKA_TIMEOUT, TimeUnit.SECONDS);

                pendingOutbox.markAsProcessed();
                orderOutboxRepository.save(pendingOutbox);
            } catch (TimeoutException e1) {
                System.out.println(
                        "Send kafka message failed, skipped for transaction " + pendingOutbox.getTransactionId());
            } catch (Exception e) {
                pendingOutbox.markAsFailed();
                e.printStackTrace();
            }
        }
        return pendingPayloads.size();
    }
}
