import type { CartProps } from "../types/order.types";
import styles from "./OrderPage.module.css";

const Cart = ({
  products,
  cartItems,
  totalAmount,
  onUpdateQuantity,
  onRemoveItem,
}: CartProps) => {
  const getProductName = (productId: number) =>
    products.find((p) => p.id === productId)?.name || `Item #${productId}`;
  if (cartItems.length === 0) {
    return <div className={styles.emptyState}>Your cart is empty.</div>;
  }

  return (
    <>
      <ul className={styles.cartList}>
        {cartItems.map((item) => (
          <li
            key={item.cartKey}
            className={styles.cartItem}
            style={{ alignItems: "flex-start" }}
          >
            <div className={styles.cartItemDetails}>
              <span className={styles.cartItemName}>
                {getProductName(item.productId)}
              </span>
              {item.selectedOptions.length > 0 && (
                <span className={styles.cartItemOptions}>
                  {item.selectedOptions
                    .map((o) => o.optionName)
                    .join(", ")}
                </span>
              )}
              <div className={styles.cartItemActions}>
                <button
                  className={styles.cartQtyBtn}
                  onClick={() => onUpdateQuantity(item.cartKey, -1)}
                >
                  -
                </button>
                <span className={styles.cartItemQty}>{item.quantity}</span>
                <button
                  className={styles.cartQtyBtn}
                  onClick={() => onUpdateQuantity(item.cartKey, 1)}
                >
                  +
                </button>
                <button
                  className={styles.cartRemoveBtn}
                  onClick={() => onRemoveItem(item.cartKey)}
                >
                  Remove
                </button>
              </div>
            </div>
            <span style={{ fontWeight: 600, color: "#1D3557", whiteSpace: "nowrap" }}>
              ${(item.unitPrice * item.quantity).toFixed(2)}
            </span>
          </li>
        ))}
      </ul>
      <div className={styles.cartTotal}>
        <span>Total: </span>
        <span>${totalAmount.toFixed(2)}</span>
      </div>
    </>
  );
};

export default Cart;
