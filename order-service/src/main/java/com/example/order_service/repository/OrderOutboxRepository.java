package com.example.order_service.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import com.example.order_service.model.OrderOutbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

/**
 * Repository interface for OrderOutbox entity operations.
 *
 * <p>This repository provides data access functionality for OrderOutbox entities,
 * which implement the Outbox Pattern for reliable event publishing. It extends
 * {@link org.springframework.data.jpa.repository.JpaRepository} to leverage Springw
 * Data JPA's automatic method generation for common database operations.</p>
 *
 * <p>The Outbox Pattern ensures exactly-once event publishing semantics by storing
 * events in the database within the same transaction as the business operation.
 * Events are subsequently processed asynchronously and published to external systems
 * or message queues. This approach guarantees no event loss and maintains consistency
 * in distributed systems.</p>
 *
 * <p>Key responsibilities include:</p>
 * <ul>
 *   <li>Persisting events that need to be published to external systems</li>
 *   <li>Retrieving unpublished events for batch processing and publishing</li>
 *   <li>Managing event publishing status (pending, processed, failed)</li>
 *   <li>Supporting pessimistic locking to prevent concurrent processing of the same event</li>
 * </ul>
 *
 * @see OrderOutbox
 * @author Order Service Team
 * @version 1.0
 */
@Repository
public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, Long> {

    /**
     * Finds outbox entries by their publishing status with pessimistic locking.
     *
     * <p>This method retrieves a paginated list of outbox entries with the specified
     * status. The results are locked using pessimistic write locking (SELECT FOR UPDATE)
     * to prevent other transactions from processing the same records concurrently.
     * If a record is already locked, it will be skipped due to the SKIP LOCKED hint.</p>
     *
     * <p>This method is typically used to retrieve unpublished events (status = "pending")
     * for batch processing and subsequent publication to external systems while ensuring
     * exclusive access and preventing duplicate event publishing.</p>
     *
     * @param status the publishing status to filter by (e.g., "pending", "processed", "failed")
     * @param pageable pagination information including page size and sorting
     * @return a list of OrderOutbox entries with the specified status, locked for exclusive access
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE) // select for update
    @QueryHints({
        @QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2") // skip locked
    })
    List<OrderOutbox> findByStatus(String status, Pageable pageable);
}
