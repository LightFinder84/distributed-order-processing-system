package com.example.order_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "customers")
public class Customer {

    /**
     * Unique identifier for the customer.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;

    /**
     * Customer fullname.
     */
    @Column(name = "customer_name", nullable = false)
    private String customerName;

    /**
     * Default constructor required by JPA.
     */
    public Customer() {
    }

    /**
     * Constructs a new Customer with a specified name.
     * @param customerName
     */
    public Customer(final String customerName) {
        this.customerName = customerName;
    }
}
