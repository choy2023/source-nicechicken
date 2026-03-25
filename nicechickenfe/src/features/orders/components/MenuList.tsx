import { useState, useRef, useEffect, useCallback, useMemo } from "react";
import type { Product, SelectedOption } from "../../../shared/types";
import type { MenuListProps } from "../types/order.types";
import ProductOptionModal from "./ProductOptionModal";
import CategoryNav from "./CategoryNav";
import styles from "./OrderPage.module.css";

const MenuList = ({ products, categories, onAddToCart }: MenuListProps) => {
  const [modalProduct, setModalProduct] = useState<Product | null>(null);
  const [activeCategoryId, setActiveCategoryId] = useState<number | null>(null);
  const sectionRefs = useRef<Record<number, HTMLDivElement | null>>({});
  const isScrollingByClick = useRef(false);

  // Sort categories by sortOrder
  const sortedCategories = useMemo(
    () => [...categories].sort((a, b) => a.sortOrder - b.sortOrder),
    [categories]
  );

  // Group products by category
  const productsByCategory = useMemo(() => {
    const map = new Map<number, Product[]>();
    for (const product of products) {
      const catId = product.category.id;
      if (!map.has(catId)) map.set(catId, []);
      map.get(catId)!.push(product);
    }
    return map;
  }, [products]);

  // Set initial active category
  useEffect(() => {
    if (sortedCategories.length > 0 && activeCategoryId === null) {
      setActiveCategoryId(sortedCategories[0].id);
    }
  }, [sortedCategories, activeCategoryId]);

  // Intersection Observer for scroll-based active tab tracking
  useEffect(() => {
    if (sortedCategories.length === 0) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (isScrollingByClick.current) return;
        for (const entry of entries) {
          if (entry.isIntersecting) {
            const id = Number(entry.target.getAttribute("data-category-id"));
            if (!isNaN(id)) setActiveCategoryId(id);
          }
        }
      },
      { rootMargin: "-80px 0px -60% 0px", threshold: 0.1 }
    );

    for (const cat of sortedCategories) {
      const el = sectionRefs.current[cat.id];
      if (el) observer.observe(el);
    }

    return () => observer.disconnect();
  }, [sortedCategories, products]);

  const handleCategoryClick = useCallback((categoryId: number) => {
    setActiveCategoryId(categoryId);
    const el = sectionRefs.current[categoryId];
    if (!el) return;

    isScrollingByClick.current = true;
    el.scrollIntoView({ behavior: "smooth", block: "start" });

    // Re-enable observer tracking after scroll settles
    setTimeout(() => {
      isScrollingByClick.current = false;
    }, 800);
  }, []);

  return (
    <section className={styles.menuSection}>
      <h2 className={styles.sectionTitle}>Menu</h2>

      <CategoryNav
        categories={sortedCategories}
        activeCategoryId={activeCategoryId}
        onCategoryClick={handleCategoryClick}
      />

      {sortedCategories.map((cat) => {
        const catProducts = productsByCategory.get(cat.id);
        if (!catProducts || catProducts.length === 0) return null;

        return (
          <div
            key={cat.id}
            ref={(el) => { sectionRefs.current[cat.id] = el; }}
            data-category-id={cat.id}
            className={styles.categorySection}
          >
            <h3 className={styles.categorySectionTitle}>{cat.name}</h3>
            <div className={styles.menuGrid}>
              {catProducts.map((product) => {
                const isOutOfStock = product.stockQuantity <= 0;
                return (
                  <article
                    key={product.id}
                    className={`${styles.productCard} ${
                      isOutOfStock ? styles.productCardOutOfStock : ""
                    }`}
                    onClick={() => !isOutOfStock && setModalProduct(product)}
                    style={{ cursor: isOutOfStock ? "default" : "pointer" }}
                  >
                    <div className={styles.imageContainer}>
                      <img
                        src={
                          product.imageUrl ||
                          "https://placehold.co/300x200?text=No+Image"
                        }
                        alt={product.name}
                        className={styles.productImage}
                      />
                      {isOutOfStock && (
                        <div className={styles.outOfStockBadge}>
                          Out of Stock
                        </div>
                      )}
                    </div>
                    <div className={styles.productInfo}>
                      <h3>{product.name}</h3>
                      <p className={styles.productDesc}>
                        {product.description}
                      </p>
                      <p className={styles.productPrice}>
                        ${product.basePrice.toFixed(2)}
                        {product.optionGroups.length > 0 && " ~"}
                      </p>
                    </div>
                    {!isOutOfStock && (
                      <div className={styles.tapHint}>Tap to customize</div>
                    )}
                  </article>
                );
              })}
            </div>
          </div>
        );
      })}

      {modalProduct && (
        <ProductOptionModal
          product={modalProduct}
          onClose={() => setModalProduct(null)}
          onAddToCart={(
            product: Product,
            selectedOptions: SelectedOption[],
            unitPrice: number
          ) => {
            onAddToCart(product, selectedOptions, unitPrice);
            setModalProduct(null);
          }}
        />
      )}
    </section>
  );
};

export default MenuList;
