package com.example.nicechicken.product.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import java.math.BigDecimal;

@Entity
@Table(name = "product_option")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "option_group", nullable = false)
    private String optionGroup;

    @Column(name = "option_name", nullable = false)
    private String optionName;

    @Column(name = "extra_price", precision = 10, scale = 2)
    private BigDecimal extraPrice = BigDecimal.ZERO;

    @Builder
    public ProductOption(Product product, String optionGroup, String optionName, BigDecimal extraPrice) {
        this.product = product;
        this.optionGroup = optionGroup;
        this.optionName = optionName;
        this.extraPrice = extraPrice != null ? extraPrice : BigDecimal.ZERO;
    }
}
