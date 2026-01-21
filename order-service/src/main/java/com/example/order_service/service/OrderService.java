package com.example.order_service.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.order_service.DTO.PlaceOrderRequest;
import com.example.order_service.DTO.PlaceOrderResponse;
import com.example.order_service.model.Customer;
import com.example.order_service.model.Order;
import com.example.order_service.model.OrderOutbox;
import com.example.order_service.model.Product;
import com.example.order_service.repository.CustomerRepository;
import com.example.order_service.repository.OrderOutboxRepository;
import com.example.order_service.repository.OrderRepository;
import com.example.order_service.repository.ProductRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

/**
 * Service for managing order operations.
 *
 * <p>This service provides business logic for placing and processing customer orders.
 * It handles order creation, validation, and coordination with the outbox pattern
 * for reliable event publishing to other microservices.</p>
 *
 * <p>Key responsibilities include:</p>
 * <ul>
 *   <li>Validating customer existence</li>
 *   <li>Validating product availability</li>
 *   <li>Creating Order entities with all items and pricing information</li>
 *   <li>Creating OrderOutbox entries for reliable event publishing</li>
 *   <li>Ensuring transactional consistency across order and outbox tables</li>
 * </ul>
 *
 * <p>The service implements the Outbox Pattern by automatically creating an outbox
 * entry whenever a new order is placed. This ensures that order creation and event
 * publishing are handled atomically within a single transaction.</p>
 *
 * @see Order
 * @see OrderOutbox
 * @see PlaceOrderRequest
 * @see PlaceOrderResponse
 * @author Order Service Team
 * @version 1.0
 */
@Service
public class OrderService {

    /**
     * Repository for managing Order entities.
     */
    @Autowired
    private OrderRepository orderRepository;

    /**
     * Repository for managing Customer entities.
     */
    @Autowired
    private CustomerRepository customerRepository;

    /**
     * Repository for managing Product entities.
     */
    @Autowired
    private ProductRepository productRepository;

    /**
     * Repository for managing OrderOutbox entries for reliable event publishing.
     */
    @Autowired
    private OrderOutboxRepository orderOutboxRepository;

    /**
     * Places a new order with the specified items.
     *
     * <p>This method processes a customer's order request by:</p>
     * <ul>
     *   <li>Validating that the customer exists in the system</li>
     *   <li>Validating that all requested products are available</li>
     *   <li>Creating an Order entity with all items and current product prices</li>
     *   <li>Creating an OrderOutbox entry to publish an ORDER_CREATED event</li>
     *   <li>Returning a response containing the created order details</li>
     * </ul>
     *
     * <p>All operations are performed within a single transaction to ensure
     * consistency between the order and outbox tables.</p>
     *
     * @param orderRequest the request containing customer ID and order items
     * @return a response containing the details of the created order
     * @throws EntityNotFoundException if the customer or any product is not found
     */
    @Transactional
    public PlaceOrderResponse placeOrder(final PlaceOrderRequest orderRequest) {
        Customer customer = this.customerRepository.findByCustomerId(orderRequest.customerId());
        if (customer == null) {
            throw new EntityNotFoundException("Customer not found with id: " + orderRequest.customerId());
        }
        List<Long> idList = orderRequest.items().stream().map(item -> item.productId()).toList();
        List<Product> products = productRepository.findAllByProductIdIn(idList);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p, (existing, replacement) -> existing));

        // check products
        if (products.size() != idList.stream().distinct().count()) {
            for (PlaceOrderRequest.OrderItem requestItem : orderRequest.items()) {
                Product product = productMap.get(requestItem.productId());
                if (product == null) {
                    throw new EntityNotFoundException("Product not found with id: " + requestItem.productId());
                }
            }
        }

        // create order
        Order order = Order.create(customer, orderRequest, productMap);
        orderRepository.save(order);

        // create outbox
        OrderOutbox outbox = OrderOutbox.create(order);
        orderOutboxRepository.save(outbox);

        return new PlaceOrderResponse(order);
    }
}
