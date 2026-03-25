import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import api from "../../../shared/api/axios";
import { type UserOrderResponse } from "../../../shared/types";
import useCancelOrder from "../../orders/hooks/useCancelOrder";
import useCustomerSSE from "../../orders/hooks/useCustomerSSE";
import { hideOrder } from "../api/orderApi";
import type { OrderHistoryProps } from "../types/order.types";
import styles from "./OrderPage.module.css";

const OrderHistory = ({ products }: OrderHistoryProps) => {
  const queryClient = useQueryClient();
  const { mutate: cancelOrder } = useCancelOrder();

  // SSE replaces 5-second polling for real-time order status updates
  useCustomerSSE(true);

  const { data: myOrders = [] } = useQuery<UserOrderResponse[]>({
    queryKey: ["myOrders"],
    queryFn: async () => (await api.get("/orders/my")).data,
  });

  const hideOrderMutation = useMutation({
    mutationFn: hideOrder,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["myOrders"] }),
  });

  const getProductName = (productId: number) => products.find(p => p.id === productId)?.name || `Menu(${productId})`;

  if (myOrders.length === 0) return <div className={styles.emptyState}>No order history.</div>;

  return (
    <ul className={styles.ordersList}>
      {myOrders.map(order => (
        <li key={order.orderId} className={styles.orderItem}>
          <div className={styles.orderHeader}>
            <strong>Order #: {order.orderId.slice(0, 8)}...</strong>
            <span className={styles.orderStatus}>Status: {order.status}</span>
            <button className={styles.cancelBtn} disabled={order.status !== 'PENDING' && order.status !== 'PAID'} onClick={() => cancelOrder(order.orderId)}>Cancel</button>
            <button className={styles.hideBtn} disabled={hideOrderMutation.isPending} onClick={() => hideOrderMutation.mutate(order.orderId)}>Hide</button>
          </div>
          <ul className={styles.orderItemList}>
            {order.items.map((item, idx) => <li key={idx}>{getProductName(item.productId)} x {item.quantity} ea</li>)}
          </ul>
          <div className={styles.orderTotal}>Total: ${order.totalAmount.toFixed(2)}</div>
        </li>
      ))}
    </ul>
  );
};

export default OrderHistory;