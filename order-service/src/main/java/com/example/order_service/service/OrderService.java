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

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderOutboxRepository orderOutboxRepository;

    @Transactional
    public PlaceOrderResponse placeOrder(PlaceOrderRequest orderRequest) {
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
