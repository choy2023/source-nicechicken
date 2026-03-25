import { useState, useEffect, useCallback } from "react";
import { useQuery } from "@tanstack/react-query";
import api from "../../../shared/api/axios";
import {
  type Product,
  type Category,
  type CartItem,
  type SelectedOption,
  makeCartKey,
} from "../../../shared/types";
import styles from "./OrderPage.module.css";

import MenuList from "./MenuList";
import Cart from "./Cart";
import OrderForm from "./OrderForm";
import OrderHistory from "./OrderHistory";
import LandingScreen from "./LandingScreen";

const CART_STORAGE_KEY = "nicechicken_cart_v2";

const OrderPage = () => {
  const [cartItems, setCartItems] = useState<CartItem[]>(() => {
    try {
      const saved = localStorage.getItem(CART_STORAGE_KEY);
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  useEffect(() => {
    localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(cartItems));
  }, [cartItems]);

  const {
    data: user,
    isLoading: isUserLoading,
    isError: isUserError,
  } = useQuery({
    queryKey: ["userMe"],
    queryFn: async () => (await api.get("/users/me")).data,
    retry: false,
  });

  const { data: products = [], isLoading: isProductsLoading } = useQuery<
    Product[]
  >({
    queryKey: ["products"],
    queryFn: async () => (await api.get("/products")).data,
    enabled: !isUserError,
  });

  const { data: categories = [] } = useQuery<Category[]>({
    queryKey: ["categories"],
    queryFn: async () => (await api.get("/categories")).data,
    enabled: !isUserError,
  });

  const handleAddToCart = useCallback(
    (product: Product, selectedOptions: SelectedOption[], unitPrice: number) => {
      const cartKey = makeCartKey(product.id, selectedOptions);

      setCartItems((prev) => {
        const existing = prev.find((item) => item.cartKey === cartKey);
        if (existing) {
          if (existing.quantity >= product.stockQuantity) {
            alert(`Only ${product.stockQuantity} left in stock.`);
            return prev;
          }
          return prev.map((item) =>
            item.cartKey === cartKey
              ? { ...item, quantity: item.quantity + 1 }
              : item
          );
        }
        return [
          ...prev,
          { cartKey, productId: product.id, quantity: 1, selectedOptions, unitPrice },
        ];
      });
    },
    []
  );

  const handleUpdateQuantity = useCallback(
    (cartKey: string, delta: number) => {
      setCartItems((prev) => {
        return prev
          .map((item) => {
            if (item.cartKey !== cartKey) return item;
            const newQty = item.quantity + delta;
            if (newQty <= 0) return null;

            const product = products.find((p) => p.id === item.productId);
            if (product && newQty > product.stockQuantity) {
              alert(`Only ${product.stockQuantity} left in stock.`);
              return item;
            }
            return { ...item, quantity: newQty };
          })
          .filter((item): item is CartItem => item !== null);
      });
    },
    [products]
  );

  const handleRemoveItem = useCallback((cartKey: string) => {
    setCartItems((prev) => prev.filter((item) => item.cartKey !== cartKey));
  }, []);

  const handleClearCart = useCallback(() => {
    setCartItems([]);
    localStorage.removeItem(CART_STORAGE_KEY);
  }, []);

  const handleLogout = async () => {
    try {
      await api.post("/auth/logout");
    } catch (e) {
      console.error("Logout failed", e);
    } finally {
      window.location.href = "/";
    }
  };

  if (isUserLoading)
    return (
      <main className={styles.loadingScreen}>
        Verifying user information...
      </main>
    );
  if (isUserError) return <LandingScreen />;
  if (isProductsLoading)
    return <main className={styles.loadingScreen}>Loading menu data...</main>;

  const totalAmount = cartItems.reduce(
    (sum, item) => sum + item.unitPrice * item.quantity,
    0
  );

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <span>Welcome, {user?.name}.</span>
        <button className={styles.logoutBtn} onClick={handleLogout}>
          Logout
        </button>
      </header>

      <h1 className={styles.pageTitle}>Order Nice Chicken</h1>

      <main className={styles.contentWrapper}>
        <MenuList products={products} categories={categories} onAddToCart={handleAddToCart} />

        <aside>
          <section className={styles.cartSection}>
            <h2 className={styles.sectionTitle}>Your Cart</h2>

            <Cart
              products={products}
              cartItems={cartItems}
              totalAmount={totalAmount}
              onUpdateQuantity={handleUpdateQuantity}
              onRemoveItem={handleRemoveItem}
            />

            <OrderForm
              user={user}
              cartItems={cartItems}
              totalAmount={totalAmount}
              onClearCart={handleClearCart}
            />
          </section>

          <section className={styles.ordersSection}>
            <h2 className={styles.sectionTitle}>My Orders (Real-time)</h2>
            <OrderHistory products={products} />
          </section>
        </aside>
      </main>
    </div>
  );
};

export default OrderPage;
