import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import api from "../../../shared/api/axios";
import { type Product } from "../../../shared/types";
import AdminProductManager from "../../products/components/AdminProductManager";
import useCancelOrder from "../../orders/hooks/useCancelOrder";
import useAdminSSE from "../hooks/useAdminSSE";
import OrderTable from "./OrderTable";
import OrderPagination from "./OrderPagination";
import styles from "./Admin.module.css";

const AdminDashboard = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<"orders" | "products">("orders");
  const [page, setPage] = useState(0);

  const { mutate: cancelOrder } = useCancelOrder();

  // 1. SSE connection for real-time order updates
  useAdminSSE(activeTab);

  // 2. Order Data Fetch with pagination
  const { data: orderPage, isLoading, isError } = useQuery({
    queryKey: ["adminOrders", page],
    queryFn: async () => {
      const response = await api.get(`/admin/orders?page=${page}&size=10`);
      return response.data;
    },
    enabled: activeTab === "orders",
  });

  // 3. Product Data Fetch for displaying product names in orders
  const { data: products = [] } = useQuery<Product[]>({
    queryKey: ["products"],
    queryFn: async () => (await api.get("/products")).data,
  });

  // 4. Mutation for updating order status
  const statusMutation = useMutation({
    mutationFn: async ({ id, status }: { id: string; status: string }) => {
      await api.patch(`/admin/orders/${id}/status`, { status });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["adminOrders"] });
    },
  });

 const handleLogout = async () => {
    try {
      await api.post("/auth/logout");
    } catch (e) {
      console.error("Logout failed", e);
    } finally {
      navigate("/admin/login");
    }
  };

  const orders = orderPage?.content || [];
  const totalPages = orderPage?.totalPages || 0;

  if (isLoading && activeTab === "orders") return <div>Loading data...</div>;
  if (isError) return <div>Failed to load data or unauthorized admin access.</div>;

  return (
    <div className={styles.adminPage}>
      <div className={styles.adminCard}>
        <h1 className={styles.pageTitle}>Nice Chicken Admin Center</h1>
        <div className={styles.tabList}>
          <button 
            className={`${styles.tabBtn} ${activeTab === "orders" ? styles.tabBtnActive : ''}`} 
            onClick={() => setActiveTab("orders")}
          >
            Order Management
          </button>
          <button 
            className={`${styles.tabBtn} ${activeTab === "products" ? styles.tabBtnActive : ''}`} 
            onClick={() => setActiveTab("products")}
          >
            Product Operations
          </button>
        </div>

        {activeTab === "orders" && (
          <div>
            <div className={styles.headerRow}>
              <h2 className={styles.sectionTitle}>Kitchen Control Room</h2>
              <button className={styles.dangerBtn} onClick={handleLogout}>Logout</button>
            </div>

            <OrderTable 
              orders={orders} 
              products={products} 
              onUpdateStatus={(id, status) => statusMutation.mutate({ id, status })}
              onCancelOrder={cancelOrder}
            />

            <OrderPagination page={page} totalPages={totalPages} setPage={setPage} />
          </div>
        )}

        {activeTab === "products" && <AdminProductManager />}
      </div>
    </div>
  );
};

export default AdminDashboard;