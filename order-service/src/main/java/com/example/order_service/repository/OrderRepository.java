package com.example.order_service.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.order_service.model.Order;

/**
 * Repository interface for Order entity operations.
 *
 * <p>This repository provides data access functionality for Order entities,
 * managing CRUD (Create, Read, Update, Delete) operations and custom queries.
 * It extends {@link org.springframework.data.jpa.repository.JpaRepository} to
 * leverage Spring Data JPA's automatic method generation for common database operations.</p>
 *
 * <p>Orders represent customer purchase requests and contain multiple order items.
 * The repository is responsible for:</p>
 * <ul>
 *   <li>Persisting new orders to the database</li>
 *   <li>Retrieving order records by ID and other criteria</li>
 *   <li>Updating existing order information and status</li>
 *   <li>Managing order lifecycle and related transactions</li>
 * </ul>
 *
 * <p>Custom query methods are provided for retrieving orders by order ID and
 * transaction ID, which are essential identifiers for order processing.</p>
 *
 * @see Order
 * @author Order Service Team
 * @version 1.0
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Finds an order by its order ID.
     *
     * <p>This method retrieves a single order record from the database
     * based on the provided order ID.</p>
     *
     * @param id the unique order ID to search for
     * @return the Order entity if found, null if not found
     */
    Order findByOrderId(Long id);

    /**
     * Finds an order by its transaction ID.
     *
     * <p>This method retrieves an order based on its transaction ID, which is
     * a UUID that uniquely identifies the business transaction. This is useful
     * for correlating orders with events and tracking order processing across
     * distributed services.</p>
     *
     * @param transactionId the UUID of the transaction to search for
     * @return the Order entity associated with the transaction ID if found, null if not found
     */
    Order findByTransactionId(UUID transactionId);
}
