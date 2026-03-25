import { useState, useEffect } from "react";
import axios from "axios";
import api from "../../../shared/api/axios";
import type { OrderRequest } from "../../../shared/types";
import type { OrderFormProps } from "../types/order.types";
import styles from "./OrderPage.module.css";

const OrderForm = ({
  user,
  cartItems,
  totalAmount,
  onClearCart,
}: OrderFormProps) => {
  const [customerName, setCustomerName] = useState("");
  const [customerPhone, setCustomerPhone] = useState("");
  const [customerEmail, setCustomerEmail] = useState("");
  const [phoneError, setPhoneError] = useState("");
  const [emailError, setEmailError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (user?.name) setCustomerName(user.name);
    if (user?.email) setCustomerEmail(user.email);
  }, [user]);

  const handleOrder = async (e: React.FormEvent) => {
    e.preventDefault();
    if (cartItems.length === 0) return alert("Cart is empty.");

    const phoneRegex = /^[0-9]{2,3}-?[0-9]{3,4}-?[0-9]{4}$/;
    if (!phoneRegex.test(customerPhone))
      return setPhoneError("Invalid phone number");
    setPhoneError("");

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(customerEmail))
      return setEmailError("Invalid email address");
    setEmailError("");

    setIsSubmitting(true);
    try {
      // Aggregate same product+options into single items
      const items = cartItems.map((item) => ({
        productId: item.productId,
        quantity: item.quantity,
        selectedOptions: item.selectedOptions.map((o) => ({
          optionGroup: o.optionGroup,
          optionName: o.optionName,
        })),
      }));

      const orderData: OrderRequest = {
        customerName,
        customerPhone,
        customerEmail,
        items,
        clientTotalAmount: Number(totalAmount.toFixed(2)),
      };

      const orderResponse = await api.post("/orders", orderData);
      const checkoutResponse = await api.post(
        `/payments/checkout/${orderResponse.data.orderId}`
      );

      onClearCart();
      window.location.href = checkoutResponse.data.checkoutUrl;
    } catch (error) {
      if (
        axios.isAxiosError(error) &&
        error.response?.status === 400 &&
        error.response.data
      ) {
        alert(
          "Invalid input:\n" + Object.values(error.response.data).join("\n")
        );
      } else {
        alert("Order failed. Please try again.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form className={styles.orderForm} onSubmit={handleOrder}>
      <input
        className={styles.inputField}
        type="text"
        placeholder="Name"
        value={customerName}
        onChange={(e) => setCustomerName(e.target.value)}
        required
      />
      <input
        className={styles.inputField}
        type="tel"
        placeholder="Phone Number"
        value={customerPhone}
        onChange={(e) => {
          setCustomerPhone(e.target.value);
          setPhoneError("");
        }}
        required
      />
      {phoneError && <p className={styles.errorMessage}>{phoneError}</p>}
      <input
        className={styles.inputField}
        type="email"
        placeholder="Email Address"
        value={customerEmail}
        onChange={(e) => {
          setCustomerEmail(e.target.value);
          setEmailError("");
        }}
        required
      />
      {emailError && <p className={styles.errorMessage}>{emailError}</p>}

      <button
        className={styles.submitBtn}
        type="submit"
        disabled={
          isSubmitting ||
          cartItems.length === 0 ||
          !!emailError ||
          !!phoneError
        }
      >
        {isSubmitting ? "Preparing payment..." : "Pay Now"}
      </button>
    </form>
  );
};

export default OrderForm;
