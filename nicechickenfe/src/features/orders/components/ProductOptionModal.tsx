import { useState, useMemo } from "react";
import type { SelectedOption } from "../../../shared/types";
import type { ProductOptionModalProps } from "../types/order.types";
import styles from "./ProductOptionModal.module.css";

const ProductOptionModal = ({
  product,
  onClose,
  onAddToCart,
}: ProductOptionModalProps) => {
  // State: one selection per option group (radio behavior)
  const [selections, setSelections] = useState<Record<string, SelectedOption>>(
    {}
  );
  const [quantity, setQuantity] = useState(1);

  const hasOptions = product.optionGroups.length > 0;

  const allGroupsSelected = useMemo(() => {
    if (!hasOptions) return true;
    return product.optionGroups.every(
      (group) => selections[group.optionGroup] !== undefined
    );
  }, [product.optionGroups, selections, hasOptions]);

  const unitPrice = useMemo(() => {
    const extraTotal = Object.values(selections).reduce(
      (sum, opt) => sum + opt.extraPrice,
      0
    );
    return product.basePrice + extraTotal;
  }, [product.basePrice, selections]);

  const handleSelect = (
    optionGroup: string,
    optionName: string,
    extraPrice: number
  ) => {
    setSelections((prev) => ({
      ...prev,
      [optionGroup]: { optionGroup, optionName, extraPrice },
    }));
  };

  const handleAdd = () => {
    const selectedOptions = Object.values(selections);
    onAddToCart(product, selectedOptions, unitPrice);
    onClose();
  };

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <div className={styles.imageSection}>
          <img
            src={
              product.imageUrl ||
              "https://placehold.co/480x220?text=No+Image"
            }
            alt={product.name}
            className={styles.productImage}
          />
          <button className={styles.closeBtn} onClick={onClose}>
            &times;
          </button>
        </div>

        <div className={styles.content}>
          <h2 className={styles.productName}>{product.name}</h2>
          {product.description && (
            <p className={styles.productDesc}>{product.description}</p>
          )}
          <p className={styles.basePrice}>
            ${product.basePrice.toFixed(2)}
          </p>

          {hasOptions ? (
            product.optionGroups.map((group) => (
              <div key={group.optionGroup} className={styles.optionGroup}>
                <p className={styles.groupLabel}>
                  {group.optionGroup}
                  <span className={styles.requiredBadge}>Required</span>
                </p>
                <div className={styles.optionList}>
                  {group.options.map((opt) => {
                    const isSelected =
                      selections[group.optionGroup]?.optionName ===
                      opt.optionName;
                    return (
                      <div
                        key={opt.optionName}
                        className={`${styles.optionItem} ${isSelected ? styles.optionItemSelected : ""}`}
                        onClick={() =>
                          handleSelect(
                            group.optionGroup,
                            opt.optionName,
                            opt.extraPrice
                          )
                        }
                      >
                        <div
                          className={`${styles.radioCircle} ${isSelected ? styles.radioCircleSelected : ""}`}
                        >
                          {isSelected && (
                            <div className={styles.radioInner} />
                          )}
                        </div>
                        <span className={styles.optionLabel}>
                          {opt.optionName}
                        </span>
                        <span className={styles.optionPrice}>
                          {opt.extraPrice > 0
                            ? `+$${opt.extraPrice.toFixed(2)}`
                            : "Free"}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>
            ))
          ) : (
            <p className={styles.noOptions}>
              No customization options available.
            </p>
          )}
        </div>

        <div className={styles.footer}>
          <div className={styles.quantityRow}>
            <button
              className={styles.qtyBtn}
              onClick={() => setQuantity((q) => Math.max(1, q - 1))}
            >
              -
            </button>
            <span className={styles.qtyValue}>{quantity}</span>
            <button
              className={styles.qtyBtn}
              onClick={() =>
                setQuantity((q) =>
                  q < product.stockQuantity ? q + 1 : q
                )
              }
            >
              +
            </button>
          </div>
          <button
            className={styles.addBtn}
            disabled={!allGroupsSelected}
            onClick={() => {
              for (let i = 0; i < quantity; i++) handleAdd();
            }}
          >
            {allGroupsSelected
              ? `Add to Cart — $${(unitPrice * quantity).toFixed(2)}`
              : "Select all options"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ProductOptionModal;
