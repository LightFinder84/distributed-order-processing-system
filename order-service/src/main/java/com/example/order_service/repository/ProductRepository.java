package com.example.order_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.order_service.model.Product;

/**
 * Repository interface for Product entity operations.
 *
 * <p>This repository provides data access functionality for Product entities,
 * managing CRUD (Create, Read, Update, Delete) operations and custom queries.
 * It extends {@link org.springframework.data.jpa.repository.JpaRepository} to
 * leverage Spring Data JPA's automatic method generation for common database operations.</p>
 *
 * <p>Products are the items that can be ordered by customers. The repository is
 * responsible for:</p>
 * <ul>
 *   <li>Persisting new products to the database</li>
 *   <li>Retrieving product records by ID and other criteria</li>
 *   <li>Updating product information such as pricing</li>
 *   <li>Querying products for order validation and fulfillment</li>
 * </ul>
 *
 * <p>Custom query methods are provided for retrieving products by individual ID
 * and bulk retrieval by multiple product IDs, supporting efficient order processing.</p>
 *
 * @see Product
 * @author Order Service Team
 * @version 1.0
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Finds a product by its product ID.
     *
     * <p>This method retrieves a single product record from the database
     * based on the provided product ID.</p>
     *
     * @param id the unique product ID to search for
     * @return the Product entity if found, null if not found
     */
    Product findByProductId(Long id);

    /**
     * Finds all products with IDs matching the provided list.
     *
     * <p>This method performs a bulk retrieval of products by their IDs, which is
     * useful when processing orders with multiple items. It returns all products
     * that match any of the provided IDs in the list.</p>
     *
     * @param idList a list of product IDs to search for
     * @return a list of Product entities that match any of the provided IDs
     */
    List<Product> findAllByProductIdIn(List<Long> idList);
}
