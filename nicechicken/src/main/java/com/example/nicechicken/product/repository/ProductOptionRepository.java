package com.example.nicechicken.product.repository;

import com.example.nicechicken.product.entity.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {
    Optional<ProductOption> findByProductIdAndOptionGroupAndOptionName(
        Long productId, String optionGroup, String optionName
    );

    List<ProductOption> findByProductId(Long productId);

    List<ProductOption> findByProductIdIn(List<Long> productIds);

    void deleteByProductId(Long productId);
}