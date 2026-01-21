package com.example.order_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.order_service.model.Customer;

/**
 * Repository interface for Customer entity operations.
 *
 * <p>This repository provides data access functionality for Customer entities,
 * managing CRUD (Create, Read, Update, Delete) operations and custom queries.
 * It extends {@link org.springframework.data.jpa.repository.JpaRepository} to
 * leverage Spring Data JPA's automatic method generation for common database operations.</p>
 *
 * <p>The repository is responsible for:</p>
 * <ul>
 *   <li>Persisting new customers to the database</li>
 *   <li>Retrieving customer records by ID and other criteria</li>
 *   <li>Updating existing customer information</li>
 *   <li>Deleting customer records when necessary</li>
 * </ul>
 *
 * <p>Custom methods can be added to this interface to provide specialized
 * queries beyond the standard CRUD operations provided by JpaRepository.</p>
 *
 * @see Customer
 * @author Order Service Team
 * @version 1.0
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Finds a customer by their customer ID.
     *
     * <p>This method retrieves a single customer record from the database
     * based on the provided customer ID.</p>
     *
     * @param id the unique customer ID to search for
     * @return the Customer entity if found, null if not found
     */
    Customer findByCustomerId(Long id);
}
