package com.example.orderservice.entity;

import com.example.orderservice.exception.InvalidOrderStatusTransitionException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    private static final Map<OrderStatus, EnumSet<OrderStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(OrderStatus.class);

    static {

        ALLOWED_TRANSITIONS.put(
                OrderStatus.PENDING,
                EnumSet.of(
                        OrderStatus.STOCK_RESERVED,
                        OrderStatus.CANCELLED
                )
        );

        ALLOWED_TRANSITIONS.put(
                OrderStatus.STOCK_RESERVED,
                EnumSet.of(
                        OrderStatus.PAYMENT_PENDING,
                        OrderStatus.STOCK_UNAVAILABLE
                )
        );

        ALLOWED_TRANSITIONS.put(
                OrderStatus.PAYMENT_PENDING,
                EnumSet.of(
                        OrderStatus.PAID,
                        OrderStatus.PAYMENT_FAILED
                )
        );

        ALLOWED_TRANSITIONS.put(
                OrderStatus.PAID,
                EnumSet.of(
                        OrderStatus.CONFIRMED
                )
        );

        ALLOWED_TRANSITIONS.put(
                OrderStatus.CONFIRMED,
                EnumSet.of(
                        OrderStatus.SHIPPED
                )
        );

        ALLOWED_TRANSITIONS.put(
                OrderStatus.SHIPPED,
                EnumSet.of(
                        OrderStatus.DELIVERED
                )
        );

        ALLOWED_TRANSITIONS.put(
                OrderStatus.DELIVERED,
                EnumSet.noneOf(OrderStatus.class)
        );

        ALLOWED_TRANSITIONS.put(
                OrderStatus.CANCELLED,
                EnumSet.noneOf(OrderStatus.class)
        );

        ALLOWED_TRANSITIONS.put(
                OrderStatus.PAYMENT_FAILED,
                EnumSet.of(
                        OrderStatus.CANCELLED
                )
        );

        ALLOWED_TRANSITIONS.put(
                OrderStatus.STOCK_UNAVAILABLE,
                EnumSet.of(
                        OrderStatus.CANCELLED
                )
        );
    }

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    @Version
    private Long version;

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }

    public void transitionTo(OrderStatus targetStatus) {

        EnumSet<OrderStatus> allowed =
                ALLOWED_TRANSITIONS.getOrDefault(
                        this.status,
                        EnumSet.noneOf(OrderStatus.class)
                );

        if (!allowed.contains(targetStatus)) {

            throw new InvalidOrderStatusTransitionException(
                    "Cannot transition order from "
                            + status
                            + " to "
                            + targetStatus
            );
        }

        this.status = targetStatus;
    }

}