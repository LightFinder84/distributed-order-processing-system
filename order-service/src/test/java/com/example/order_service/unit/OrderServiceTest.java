package com.example.order_service.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.order_service.DTO.PlaceOrderRequest;
import com.example.order_service.DTO.PlaceOrderRequest.OrderItem;
import com.example.order_service.model.Customer;
import com.example.order_service.model.Order;
import com.example.order_service.model.Product;
import com.example.order_service.DTO.PlaceOrderResponse;
import com.example.order_service.repository.CustomerRepository;
import com.example.order_service.repository.OrderOutboxRepository;
import com.example.order_service.repository.OrderRepository;
import com.example.order_service.repository.ProductRepository;
import com.example.order_service.service.OrderService;

import jakarta.persistence.EntityNotFoundException;

/**
 * Unit Test for OrderService
 * Use Mockito to simulate Repositories, focus on business logic on service layer
 */
@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {
    
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderOutboxRepository orderOutboxRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("Should place order successfully")
    void shouldPlaceOrderSuccessfully() {
        // 1. prepare data and simlator behavior
        Long customerId = 1L;
        Long productId = 101L;

        OrderItem item = new PlaceOrderRequest.OrderItem(productId, 2);
        PlaceOrderRequest request = new PlaceOrderRequest(customerId, List.of(item));

        Customer mockCustomer = new Customer();
        mockCustomer.setCustomerId(customerId);
        when(customerRepository.findByCustomerId(customerId)).thenReturn(mockCustomer);

        Product mockProduct = new Product();
        mockProduct.setProductId(productId);
        mockProduct.setPrice(new BigDecimal(100000.00));
        when(productRepository.findAllByProductIdIn(anyList())).thenReturn(List.of(mockProduct));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "orderId", 10L);
            return order;
        });

        // 2. Act (Conduct test)
        PlaceOrderResponse response = orderService.placeOrder(request);

        assertNotNull(response);
        assertNotNull(response.orderId());
        assertNotNull(response.transactionId());
        assertEquals(customerId, response.customerId());
        assertEquals(1, response.items().size());
        assertEquals(productId, response.items().get(0).productId());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when Customer is not found")
    void shouldThrowException_WhenCustomerNotFound() {
        // 1. prepare data and simlator behavior
        Long customerId = 1L;
        Long productId = 101L;

        OrderItem item = new PlaceOrderRequest.OrderItem(productId, 2);
        PlaceOrderRequest request = new PlaceOrderRequest(customerId, List.of(item));

        when(customerRepository.findByCustomerId(customerId)).thenReturn(null);

        // 2. Act & Assert (Conduct test)
        EntityNotFoundException exception =  assertThrows(EntityNotFoundException.class, () -> {
            orderService.placeOrder(request);
        });

        assertEquals("Customer not found with id: " + customerId, exception.getMessage());

        verify(orderRepository, never()).save(any());
        verify(orderOutboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when product is not found")
    void shouldThrowException_WhenProductNotFound() {
        // 1. prepare data and simlator behavior
        Long customerId = 1L;
        Long productId = 101L;

        OrderItem item = new PlaceOrderRequest.OrderItem(productId, 2);
        PlaceOrderRequest request = new PlaceOrderRequest(customerId, List.of(item));

        Customer mockCustomer = new Customer();
        mockCustomer.setCustomerId(customerId);
        when(customerRepository.findByCustomerId(customerId)).thenReturn(mockCustomer);

        when(productRepository.findAllByProductIdIn(anyList())).thenReturn(List.of());

        // 2. Act & assert (Conduct test)
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            orderService.placeOrder(request);
        });

        assertEquals("Product not found with id: " + productId, exception.getMessage());
        verify(orderRepository, never()).save(any());
        verify(orderOutboxRepository, never()).save(any());
    }
}
