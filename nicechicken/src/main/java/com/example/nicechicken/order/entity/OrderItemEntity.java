package com.example.nicechicken.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderEntity order;

    @Column(name = "product_id")
    private Long productId;

    @JdbcTypeCode(SqlTypes.JSON) // JSONB Mapping (Hibernate 6 approach)
    @Column(name = "selected_options", columnDefinition = "jsonb")
    private List<Map<String, Object>> selectedOptions;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "price_at_order", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtOrder;

    @Builder
    public OrderItemEntity(Long productId, List<Map<String, Object>> selectedOptions, Integer quantity, BigDecimal priceAtOrder) {
        this.productId = productId;
        this.selectedOptions = selectedOptions;
        this.quantity = quantity;
        this.priceAtOrder = priceAtOrder;
    }

    public void setOrder(OrderEntity order) {
        this.order = order;
    }
}