package com.example.order_service.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.example.order_service.DTO.PlaceOrderRequest;
import com.example.order_service.DTO.PlaceOrderRequest.OrderItem;
import com.example.order_service.repository.OrderItemRepository;
import com.example.order_service.repository.OrderOutboxRepository;
import com.example.order_service.repository.OrderRepository;
import com.example.order_service.service.OrderService;

import jakarta.persistence.EntityNotFoundException;

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
public class OrderServiceIT extends BaseIntegration {

    @MockitoSpyBean
    private OrderRepository orderRepository;

    @MockitoSpyBean
    private OrderOutboxRepository orderOutboxRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderService orderService;

    @BeforeEach
    @AfterAll
    void tearDown() {
        // clear data on table orders
        orderOutboxRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("should insert new data")
    void shouldInsertNewData() {
        // arrange
        OrderItem orderItem = new OrderItem(1L, 2);
        PlaceOrderRequest orderRequest = new PlaceOrderRequest(1L, List.of(orderItem));

        // act
        orderService.placeOrder(orderRequest);

        // assert
        assertEquals(1, orderRepository.count());
        assertEquals(1, orderItemRepository.count());
        assertEquals(1, orderOutboxRepository.count());
    }

    @Test
    @DisplayName("should not insert data when user not found")
    void shouldNotInsertData_WhenUserNotFound() {
        // arrange
        OrderItem orderItem = new OrderItem(1L, 2);
        PlaceOrderRequest orderRequest = new PlaceOrderRequest(100L, List.of(orderItem));

        // act & assert
        assertThrows(EntityNotFoundException.class, () -> {
            orderService.placeOrder(orderRequest);
        });

        // assert
        assertEquals(0, orderRepository.count());
        assertEquals(0, orderItemRepository.count());
        assertEquals(0, orderOutboxRepository.count());
    }

    @Test
    @DisplayName("should not insert data when product not found")
    void shouldNotInsertData_WhenProductNotFound() {
        // arrange
        OrderItem orderItem = new OrderItem(100L, 2);
        PlaceOrderRequest orderRequest = new PlaceOrderRequest(1L, List.of(orderItem));

        // act & assert
        assertThrows(EntityNotFoundException.class, () -> {
            orderService.placeOrder(orderRequest);
        });

        // assert
        assertEquals(0, orderRepository.count());
        assertEquals(0, orderItemRepository.count());
        assertEquals(0, orderOutboxRepository.count());
    }

    // @Test
    // @DisplayName("should not insert any data when failed to insert order")
    // void shouldNotInsertData_WhenFailedToInsertOrder() {
    //     // arrange
    //     OrderItem orderItem = new OrderItem(1L, 2);
    //     PlaceOrderRequest orderRequest = new PlaceOrderRequest(1L, List.of(orderItem));

    //     doThrow(new DataIntegrityViolationException("duplicate ID")).when(orderRepository).save(any(Order.class));

    //     // act & assert
    //     assertThrows(DataIntegrityViolationException.class, () -> {
    //         orderService.placeOrder(orderRequest);
    //     });

    //     // assert
    //     assertEquals(0, orderRepository.count());
    //     assertEquals(0, orderItemRepository.count());
    //     assertEquals(0, orderOutboxRepository.count());
    // }

    // @Test
    // @DisplayName("should not insert any data when failed to insert outbox")
    // void shouldNotInsertData_WhenFailedToInsertOutbox() {
    //     // arrange
    //     OrderItem orderItem = new OrderItem(1L, 2);
    //     PlaceOrderRequest orderRequest = new PlaceOrderRequest(1L, List.of(orderItem));

    //     doThrow(new DataIntegrityViolationException("duplicate ID")).when(orderOutboxRepository).save(any(OrderOutbox.class));

    //     // act & assert
    //     assertThrows(DataIntegrityViolationException.class, () -> {
    //         orderService.placeOrder(orderRequest);
    //     });

    //     // assert
    //     assertEquals(0, orderRepository.count());
    //     assertEquals(0, orderItemRepository.count());
    //     assertEquals(0, orderOutboxRepository.count());
    // }
}
