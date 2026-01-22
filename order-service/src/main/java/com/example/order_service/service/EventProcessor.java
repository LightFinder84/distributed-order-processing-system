package com.example.order_service.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.order_service.event.Event;
import com.example.order_service.model.Order;
import com.example.order_service.model.OrderInbox;
import com.example.order_service.repository.OrderInboxRepository;
import com.example.order_service.repository.OrderRepository;

import jakarta.transaction.Transactional;

/**
 * Service for processing events from the inbox.
 *
 * <p>This service retrieves events that were previously stored in the inbox table
 * by {@link EventConsumer} and processes them to update order states. It implements
 * the Inbox Pattern for reliable event processing with at-least-once semantics.</p>
 *
 * <p>The event processor is responsible for:</p>
 * <ul>
 *   <li>Retrieving pending events from the inbox in batches</li>
 *   <li>Correlating events with their corresponding orders using transaction IDs</li>
 *   <li>Updating order statuses based on event types (payment success/failure, inventory failure)</li>
 *   <li>Marking events as processed or skipped after handling</li>
 *   <li>Processing events in a transactional manner to maintain consistency</li>
 * </ul>
 *
 * <p>The processor handles various event types from payment and inventory services,
 * updating order states accordingly. If an order cannot be found for a given event,
 * the event is marked as skipped to prevent blocking subsequent events.</p>
 *
 * @see EventConsumer
 * @see OrderInbox
 * @see Order
 * @author Order Service Team
 * @version 1.0
 */
@Service
public class EventProcessor {

    /**
     * Repository for querying and updating inbox entries.
     */
    @Autowired
    private OrderInboxRepository orderInboxRepository;

    /**
     * Repository for querying and updating order entities.
     */
    @Autowired
    private OrderRepository orderRepository;

    /**
     * Self-reference to this service, lazily injected for use in transactional method calls.
     * This allows circumventing Spring's transaction proxy to properly handle batch processing.
     */
    @Autowired
    @Lazy
    private EventProcessor self;

    /**
     * Batch size for processing inbox events in a single transaction.
     * Configured via {@code application.batch-size} property.
     */
    @Value("${application.batch-size}")
    private int batchSize;

    /**
     * Processes all pending events in the inbox.
     *
     * <p>This method continuously processes events in batches until no more pending
     * events remain. It uses a self-reference to ensure that each batch is processed
     * within its own transaction boundary.</p>
     */
    public void processEvents() {
        int count = self.processBatch();
        while (count != 0) {
            count = self.processBatch();
        }
    }

    /**
     * Processes a single batch of pending inbox events.
     *
     * <p>This method retrieves up to {@code batchSize} pending events from the inbox,
     * sorted by creation time in ascending order. Each event in the batch is processed
     * to update the corresponding order's state.</p>
     *
     * <p>The method is transactional to ensure that all updates within a batch are
     * atomic and consistent.</p>
     *
     * @return the number of events processed in this batch; returns 0 when no pending events remain
     */
    @Transactional
    public int processBatch() {
        Pageable pageable = PageRequest.of(0, batchSize, Sort.by("createdAt").ascending());
        List<OrderInbox> pendingInboxes = orderInboxRepository.findByStatus("pending", pageable);
        for (OrderInbox inbox : pendingInboxes) {
            processEvent(inbox);
        }
        return pendingInboxes.size();
    }

    /**
     * Processes a single event from the inbox.
     *
     * <p>This method handles the processing of an individual inbox event by:</p>
     * <ul>
     *   <li>Retrieving the order associated with the event's transaction ID</li>
     *   <li>Updating the order's status based on the event type:
     *       <ul>
     *         <li>INVENTORY_FAILED or PAYMENT_FAILED - marks order as cancelled</li>
     *         <li>PAYMENT_SUCCESS - marks order as completed</li>
     *       </ul>
     *   </li>
     *   <li>Persisting the updated order</li>
     *   <li>Marking the inbox event as processed or skipped</li>
     * </ul>
     *
     * <p>If no order is found for the transaction ID, the event is marked as skipped
     * to prevent blocking subsequent event processing.</p>
     *
     * @param inbox the inbox event to process
     */
    private void processEvent(final OrderInbox inbox) {
        UUID transactionId = inbox.getTransactionId();
        Order order = orderRepository.findByTransactionId(transactionId);
        if (order == null) {
            inbox.markAsSkipped();
            orderInboxRepository.save(inbox);
            return;
        }
        Event event = inbox.getPayload();

        if (Event.Type.INVENTORY_FAILED == event.type()) {
            order.markAsCancelled();
        }
        if (Event.Type.PAYMENT_FAILED == event.type()) {
            order.markAsCancelled();
        }
        if (Event.Type.PAYMENT_SUCCESS == event.type()) {
            order.markAsCompleted();
        }
        orderRepository.save(order);

        inbox.markAsProcessed();
        orderInboxRepository.save(inbox);
    }
}
