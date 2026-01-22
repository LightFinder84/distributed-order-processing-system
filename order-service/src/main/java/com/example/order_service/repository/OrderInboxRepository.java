package com.example.order_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import com.example.order_service.event.Event.Type;
import com.example.order_service.model.OrderInbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

/**
 * Repository interface for OrderInbox entity operations.
 *
 * <p>This repository provides data access functionality for OrderInbox entities,
 * which implement the Inbox Pattern for reliable handling of incoming events from
 * other services. It extends {@link org.springframework.data.jpa.repository.JpaRepository}
 * to leverage Spring Data JPA's automatic method generation for common database operations.</p>
 *
 * <p>The OrderInbox pattern ensures at-least-once processing of events by storing
 * incoming events in the database before processing them. This prevents event loss
 * in case of failures and ensures idempotent event handling.</p>
 *
 * <p>Key responsibilities include:</p>
 * <ul>
 *   <li>Storing incoming events from other services</li>
 *   <li>Querying unprocessed events for batch processing</li>
 *   <li>Managing event processing status and idempotency</li>
 *   <li>Supporting pessimistic locking to prevent concurrent processing of the same event</li>
 * </ul>
 *
 * @see OrderInbox
 * @author Order Service Team
 * @version 1.0
 */
@Repository
public interface OrderInboxRepository extends JpaRepository<OrderInbox, Long> {

    /**
     * Finds inbox entries by their processing status with pessimistic locking.
     *
     * <p>This method retrieves a paginated list of inbox entries with the specified
     * status. The results are locked using pessimistic write locking (SELECT FOR UPDATE)
     * to prevent other transactions from processing the same records concurrently.
     * If a record is already locked, it will be skipped due to the SKIP LOCKED hint.</p>
     *
     * <p>This is typically used to retrieve unprocessed or failed events for batch
     * processing while ensuring exclusive access.</p>
     *
     * @param status the processing status to filter by (e.g., "pending", "processed", "failed")
     * @param pageable pagination information including page size and sorting
     * @return a list of OrderInbox entries with the specified status, locked for exclusive access
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE) // select for update
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2") // skip locked
    })
    List<OrderInbox> findByStatus(String status, Pageable pageable);

    /**
     * Finds all inbox entries associated with a specific transaction ID.
     *
     * <p>This method retrieves all inbox entries that belong to a particular
     * business transaction, enabling correlation and tracking of events across
     * multiple service interactions.</p>
     *
     * @param transactionId the UUID of the transaction to search for
     * @return a list of OrderInbox entries associated with the transaction ID
     */
    List<OrderInbox> findByTransactionId(UUID transactionId);

    /**
     * Checks if an inbox entry exists for a specific transaction ID and event type.
     *
     * <p>This method is used for idempotency checking to determine if an event
     * of a specific type has already been processed for a given transaction.
     * This ensures that duplicate events are not processed multiple times.</p>
     *
     * @param transactionId the UUID of the transaction
     * @param type the event type to check for
     * @return true if an inbox entry exists for the given transaction ID and event type, false otherwise
     */
    boolean existsByTransactionIdAndType(UUID transactionId, Type type);
}
