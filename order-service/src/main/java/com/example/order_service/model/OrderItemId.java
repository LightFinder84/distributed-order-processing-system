package com.example.order_service.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

/**
 * Composite primary key for the OrderItem entity.
 *
 * <p>
 * This class represents a composite identifier for OrderItem entities,
 * combining the order ID and product ID. It is used as an embedded ID in the
 * OrderItem entity to uniquely identify each line item within an order.
 * </p>
 *
 * <p>
 * The class implements {@link Serializable} to ensure it can be properly
 * serialized by the JPA provider and is marked with {@link Embeddable} to
 * indicate it should be embedded within the OrderItem entity.
 * </p>
 *
 * @see OrderItem
 * @author Order Service Team
 * @version 1.0
 */
@Embeddable
@Getter
@Setter
public class OrderItemId implements Serializable {

    /**
     * The unique identifier of the order.
     *
     * <p>
     * This field represents the first part of the composite primary key
     * and references the order to which this item belongs.
     * </p>
     */
    @Column(name = "order_id")
    private Long orderId;

    /**
     * The unique identifier of the product.
     *
     * <p>
     * This field represents the second part of the composite primary key
     * and references the product associated with this order item.
     * </p>
     */
    @Column(name = "product_id")
    private Long productId;

    /**
     * Default no-argument constructor.
     *
     * <p>
     * Required by JPA for entity instantiation and by frameworks that use
     * reflection to create instances.
     * </p>
     */
    public OrderItemId() {
    }

    /**
     * Constructor to create an OrderItemId with the specified order and product
     * identifiers.
     *
     * @param orderId   the ID of the order, must not be null
     * @param productId the ID of the product, must not be null
     */
    public OrderItemId(final Long orderId, final Long productId) {
        this.orderId = orderId;
        this.productId = productId;
    }

    /**
     * Compares this OrderItemId with another object for equality.
     *
     * <p>
     * Two OrderItemId instances are considered equal if they have the same
     * orderId and productId values. This method uses the final modifier to
     * prevent accidental overriding in subclasses.
     * </p>
     *
     * @param o the object to be compared with this OrderItemId
     * @return true if the specified object is equal to this OrderItemId, false
     *         otherwise
     */
    @Override
    public final boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OrderItemId that = (OrderItemId) o;
        return Objects.equals(orderId, that.orderId) && Objects.equals(productId, that.productId);
    }

    /**
     * Returns the hash code for this OrderItemId.
     *
     * <p>
     * The hash code is computed based on both the orderId and productId
     * fields, ensuring that equal OrderItemId instances produce the same
     * hash code. This method uses the final modifier to prevent accidental
     * overriding in subclasses.
     * </p>
     *
     * @return the hash code value for this OrderItemId
     */
    @Override
    public final int hashCode() {
        return Objects.hash(orderId, productId);
    }
}
