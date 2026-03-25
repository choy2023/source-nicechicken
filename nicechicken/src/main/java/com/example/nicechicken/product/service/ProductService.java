package com.example.nicechicken.product.service;

import com.example.nicechicken.product.dto.CategoryResponse;
import com.example.nicechicken.product.dto.ProductRequest;
import com.example.nicechicken.product.dto.ProductResponse;
import com.example.nicechicken.product.entity.Category;
import com.example.nicechicken.product.entity.Product;
import com.example.nicechicken.product.entity.ProductOption;
import com.example.nicechicken.product.repository.ProductOptionRepository;
import com.example.nicechicken.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final CategoryService categoryService;

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    }

    public ProductOption getProductOption(Long productId, String optionGroup, String optionName) {
        return productOptionRepository.findByProductIdAndOptionGroupAndOptionName(
                productId, optionGroup, optionName)
                .orElseThrow(() -> new IllegalArgumentException("Option not found: " + optionName));
    }

    @Transactional
    public void decreaseStock(Long productId, int quantity) {
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found. ID: " + productId));
        product.decreaseStock(quantity);
    }

    @Transactional
    public void restoreStock(Long productId, int quantity) {
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found. ID: " + productId));
        product.restoreStock(quantity);
    }

    public List<ProductResponse> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return toResponsesWithOptions(products);
    }

    public List<ProductResponse> getProductsByCategory(Long categoryId) {
        List<Product> products = productRepository.findByCategory_Id(categoryId);
        return toResponsesWithOptions(products);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryService.getCategoryById(request.categoryId());
        Product product = Product.builder()
                .category(category)
                .name(request.name())
                .basePrice(request.basePrice())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .stockQuantity(request.stockQuantity())
                .build();
        Product savedProduct = productRepository.save(product);
        saveOptions(savedProduct, request.optionGroups());
        return toResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product does not exist."));
        Category category = categoryService.getCategoryById(request.categoryId());
        product.update(category, request.name(), request.basePrice(), request.description(), request.imageUrl(), request.stockQuantity());

        // Replace all options: delete existing, then recreate from request
        productOptionRepository.deleteByProductId(id);
        productOptionRepository.flush(); // ensure deletes execute before inserts
        saveOptions(product, request.optionGroups());
        return toResponse(product);
    }

    private void saveOptions(Product product, List<ProductRequest.OptionGroupRequest> optionGroups) {
        if (optionGroups == null || optionGroups.isEmpty()) return;
        for (ProductRequest.OptionGroupRequest group : optionGroups) {
            for (ProductRequest.OptionRequest opt : group.options()) {
                productOptionRepository.save(ProductOption.builder()
                        .product(product)
                        .optionGroup(group.optionGroup())
                        .optionName(opt.optionName())
                        .extraPrice(opt.extraPrice())
                        .build());
            }
        }
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new IllegalArgumentException("Product does not exist.");
        }
        productRepository.deleteById(id);
    }

    private List<ProductResponse> toResponsesWithOptions(List<Product> products) {
        if (products.isEmpty()) return List.of();

        List<Long> productIds = products.stream().map(Product::getId).toList();
        Map<Long, List<ProductOption>> optionsByProduct = productOptionRepository
                .findByProductIdIn(productIds).stream()
                .collect(Collectors.groupingBy(o -> o.getProduct().getId()));

        return products.stream()
                .map(p -> toResponse(p, optionsByProduct.getOrDefault(p.getId(), List.of())))
                .toList();
    }

    private ProductResponse toResponse(Product p, List<ProductOption> options) {
        CategoryResponse categoryResponse = null;
        if (p.getCategory() != null) {
            Category c = p.getCategory();
            categoryResponse = new CategoryResponse(c.getId(), c.getName(), c.getSortOrder(), c.isActive());
        }

        List<ProductResponse.OptionGroupResponse> optionGroups = options.stream()
                .collect(Collectors.groupingBy(ProductOption::getOptionGroup))
                .entrySet().stream()
                .map(entry -> new ProductResponse.OptionGroupResponse(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(o -> new ProductResponse.OptionDetail(o.getOptionName(), o.getExtraPrice()))
                                .toList()))
                .toList();

        return new ProductResponse(p.getId(), categoryResponse, p.getName(),
                p.getBasePrice(), p.getDescription(), p.getImageUrl(), p.getStockQuantity(), optionGroups);
    }

    private ProductResponse toResponse(Product p) {
        return toResponse(p, productOptionRepository.findByProductId(p.getId()));
    }
}
