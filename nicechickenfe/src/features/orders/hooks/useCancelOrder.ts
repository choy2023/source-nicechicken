import { useMutation, useQueryClient } from "@tanstack/react-query";
import { cancelOrder } from "../api/orderApi";
import { type AdminOrderResponse, type UserOrderResponse } from "../../../shared/types";

const useCancelOrder = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: cancelOrder,
    onSuccess: (_, orderId) => {
      // Update Admin Orders Cache Immediately
      queryClient.setQueryData<AdminOrderResponse[]>(["adminOrders"], (old) => {
        if (!old) return old;
        return old.map((order) =>
          order.orderId === orderId ? { ...order, status: "CANCELLED" } : order
        );
      });

      // Update User Orders Cache Immediately
      queryClient.setQueryData<UserOrderResponse[]>(["myOrders"], (old) => {
        if (!old) return old;
        return old.map((order) =>
          order.orderId === orderId ? { ...order, status: "CANCELLED" } : order
        );
      });
      
      alert("Order has been cancelled.");
    },
    onError: (error) => {
      console.error("Cancel failed:", error);
      alert("Failed to cancel order.");
    },
  });
};

export default useCancelOrder;