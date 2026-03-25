import { useEffect, useRef } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { type AdminOrderResponse } from "../../../shared/types";
import api from "../../../shared/api/axios";

const MAX_RECONNECT_ATTEMPTS = 5;

const useAdminSSE = (activeTab: "orders" | "products") => {
  const queryClient = useQueryClient();
  const reconnectCount = useRef(0);

  useEffect(() => {
    if (activeTab !== "orders") return;

    let eventSource: EventSource;
    let reconnectTimeout: ReturnType<typeof setTimeout>;
    let cancelled = false;

    const connectSSE = async () => {
      if (cancelled) return;

      try {
        // Step 1: Fetch a short-lived SSE token via axios (sends JWT cookie)
        const { data } = await api.get<{ token: string }>("/sse/token");

        if (cancelled) return;

        // Step 2: Connect EventSource with token as query param (no cookie needed)
        eventSource = new EventSource(
          `${import.meta.env.VITE_API_BASE_URL}/api/admin/sse/subscribe?token=${data.token}`
        );

        eventSource.addEventListener("connect", () => {
          reconnectCount.current = 0;
        });

        eventSource.addEventListener("new_order", (event) => {
          console.log("New order notification received:", event.data);
          queryClient.invalidateQueries({ queryKey: ["adminOrders"] });
        });

        eventSource.addEventListener("refund_completed", (event) => {
          let orderId = "";
          try {
            const parsed = JSON.parse(event.data);
            orderId = parsed.orderId || event.data;
          } catch {
            orderId = event.data;
          }
          alert(`Refund completed for order ID ${orderId}.`);

          queryClient.setQueryData<AdminOrderResponse[]>(
            ["adminOrders"],
            (old) => {
              if (!old) return old;
              return old.map((order) =>
                order.orderId === orderId
                  ? { ...order, status: "CANCELLED" }
                  : order
              );
            }
          );
        });

        eventSource.onerror = () => {
          eventSource.close();
          scheduleReconnect();
        };
      } catch (error) {
        console.error("Failed to fetch SSE token:", error);
        scheduleReconnect();
      }
    };

    const scheduleReconnect = () => {
      if (cancelled) return;
      if (reconnectCount.current >= MAX_RECONNECT_ATTEMPTS) {
        console.warn("Admin SSE: Max reconnect attempts reached. Stopping.");
        return;
      }
      reconnectCount.current += 1;
      const delay = Math.min(3000 * reconnectCount.current, 15000);
      console.log(
        `Admin SSE: Reconnecting in ${delay / 1000}s (attempt ${reconnectCount.current}/${MAX_RECONNECT_ATTEMPTS})`
      );
      reconnectTimeout = setTimeout(connectSSE, delay);
    };

    connectSSE();

    return () => {
      cancelled = true;
      if (eventSource!) eventSource.close();
      if (reconnectTimeout!) clearTimeout(reconnectTimeout);
    };
  }, [activeTab, queryClient]);
};

export default useAdminSSE;
