package com.example.order_service.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.example.order_service.DTO.PlaceOrderRequest;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "orders")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "transaction_id", unique = true, nullable = false)
    private UUID transactionId;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "pending";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "created_at", nullable = false)
    private final OffsetDateTime createdAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;

    public static Order create(Customer customer, PlaceOrderRequest orderRequest, Map<Long, Product> productMap) {
        Order order = Order.builder().customer(customer).transactionId(UUID.randomUUID()).build();

        List<OrderItem> orderItems = orderRequest.items().stream().map(requestItem -> {
            Product product = productMap.get(requestItem.productId());
            OrderItem item = OrderItem.builder()
                    .id(new OrderItemId(null, requestItem.productId()))
                    .product(product)
                    .priceAtPurchase(product.getPrice())
                    .quantity(requestItem.quantity())
                    .order(order)
                    .build();
            return item;
        }).toList();
        order.setItems(orderItems);

        return order;
    }

    public void markAsCancelled() {
        this.status = "cancelled";
    }

    public void markAsCompleted() {
        this.status = "completed";
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
}
