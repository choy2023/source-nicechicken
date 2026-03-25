import { useEffect, useRef } from "react";
import { useQueryClient } from "@tanstack/react-query";
import api from "../../../shared/api/axios";

const MAX_RECONNECT_ATTEMPTS = 5;

const useCustomerSSE = (enabled: boolean) => {
  const queryClient = useQueryClient();
  const reconnectCount = useRef(0);

  useEffect(() => {
    if (!enabled) return;

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
          `${import.meta.env.VITE_API_BASE_URL}/api/sse/subscribe?token=${data.token}`
        );

        eventSource.addEventListener("connect", () => {
          // If this is a reconnect, sync latest order state
          // (covers status changes missed while disconnected)
          if (reconnectCount.current > 0) {
            queryClient.invalidateQueries({ queryKey: ["myOrders"] });
          }
          reconnectCount.current = 0;
        });

        eventSource.addEventListener("order_status", (event) => {
          try {
            const payload = JSON.parse(event.data);
            console.log(
              `Order ${payload.orderId} status changed: ${payload.oldStatus} → ${payload.newStatus}`
            );
          } catch {
            // ignore parse errors
          }
          queryClient.invalidateQueries({ queryKey: ["myOrders"] });
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
        console.warn(
          "Customer SSE: Max reconnect attempts reached. Stopping."
        );
        return;
      }
      reconnectCount.current += 1;
      const delay = Math.min(3000 * reconnectCount.current, 15000);
      console.log(
        `Customer SSE: Reconnecting in ${delay / 1000}s (attempt ${reconnectCount.current}/${MAX_RECONNECT_ATTEMPTS})`
      );
      reconnectTimeout = setTimeout(connectSSE, delay);
    };

    connectSSE();

    return () => {
      cancelled = true;
      if (eventSource!) eventSource.close();
      if (reconnectTimeout!) clearTimeout(reconnectTimeout);
    };
  }, [enabled, queryClient]);
};

export default useCustomerSSE;
