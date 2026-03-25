package com.example.nicechicken.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.Builder;
import java.math.BigDecimal;

import com.example.nicechicken.common.entity.BaseTimeEntity;

@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private String name;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(length = 500)
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @Builder
    public Product(Category category, String name, BigDecimal basePrice, String description, String imageUrl, Integer stockQuantity) {
        this.category = category;
        this.name = name;
        this.basePrice = basePrice;
        this.description = description;
        this.imageUrl = imageUrl;
        this.stockQuantity = (stockQuantity != null) ? stockQuantity : 0;
    }

    public void update(Category category, String name, BigDecimal basePrice, String description, String imageUrl, Integer stockQuantity) {
        this.category = category;
        this.name = name;
        this.basePrice = basePrice;
        this.description = description;
        this.imageUrl = imageUrl;
        this.stockQuantity = stockQuantity;
    }

    public void decreaseStock(int quantity) {
        if (this.stockQuantity < quantity) {
            throw new IllegalArgumentException("Insufficient stock. (Current stock: " + this.stockQuantity + ")");
        }
        this.stockQuantity -= quantity;
    }

    public void restoreStock(int quantity) {
        this.stockQuantity += quantity;
    }
}
