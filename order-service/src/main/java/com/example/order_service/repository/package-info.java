/**
 * Data Access Layer (Repository) Package.
 *
 * <p>This package contains Spring Data JPA repository interfaces that provide
 * data access functionality for the Order Service application. Repositories
 * abstract away the details of database operations and provide a clean interface
 * for accessing and manipulating domain entities.</p>
 *
 * <p>Key repositories in this package include:</p>
 * <ul>
 *   <li>{@code OrderRepository} - Manages Order entity operations including
 *       retrieval, creation, and updates</li>
 *   <li>{@code OrderItemRepository} - Handles OrderItem entity persistence</li>
 *   <li>{@code ProductRepository} - Manages Product entity access</li>
 *   <li>{@code CustomerRepository} - Handles Customer entity operations</li>
 *   <li>{@code OrderOutboxRepository} - Manages outbox entries for reliable
 *       event publishing using the Outbox Pattern</li>
 *   <li>{@code OrderInboxRepository} - Manages inbox entries for processing
 *       incoming events from other services</li>
 * </ul>
 *
 * <p>All repositories extend {@code JpaRepository} or {@code CrudRepository}
 * to leverage Spring Data JPA's automatic CRUD method generation. Custom query
 * methods can be added to repositories as needed for specific business operations.</p>
 *
 * <p>The repositories are designed to work with a PostgreSQL database and support
 * transaction management through Spring's transaction framework.</p>
 *
 * @author Order Service Team
 * @version 1.0
 * @see org.springframework.data.jpa.repository.JpaRepository
 */
package com.example.order_service.repository;
