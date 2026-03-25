import type { OrderTableProps } from "../types/admin.types";
import styles from "./Admin.module.css";

const OrderTable = ({ orders, products, onUpdateStatus, onCancelOrder }: OrderTableProps) => {

  const getProductName = (productId: number) => {
    const product = products.find((p) => p.id === productId);
    return product ? product.name : `Menu(${productId})`;
  };

  return (
    <div className={styles.tableContainer}>
      <div style={{ textAlign: "right", color: "#666", fontSize: "0.9rem", marginBottom: '10px' }}>
        💡 Changing status automatically sends notification email to customer.
      </div>
      <table className={styles.adminTable}>
        <thead>
        <tr>
          <th>Order ID</th>
          <th>Customer</th>
          <th>Order Details</th>
          <th>Total Amount</th>
          <th>Status</th>
          <th>Action</th>
        </tr>
      </thead>
      <tbody>
        {orders.map((order) => (
          <tr key={order.orderId}>
            <td>{order.orderId.slice(0, 8)}...</td>
            <td>
              <strong>{order.customerName}</strong>
              <br />
              <small>{order.customerPhone}</small>
            </td>
            <td>
              <ul style={{ margin: 0, paddingLeft: "20px" }}>
                {order.items.map((item, idx) => (
                  <li key={idx}>
                    {getProductName(item.productId)} x {item.quantity} EA
                  </li>
                ))}
              </ul>
            </td>
            <td>${order.totalAmount.toFixed(2)}</td>
            <td>
              <strong>{order.status}</strong>
            </td>
              <td>
                {order.status === "PAID" && (
                  <button className={styles.primaryBtn} onClick={() => onUpdateStatus(order.orderId, "COOKING")} style={{ marginRight: '5px' }}>Start Cooking</button>
                )}
                {order.status === "COOKING" && (
                  <button className={styles.primaryBtn} onClick={() => onUpdateStatus(order.orderId, "READY")} style={{ marginRight: '5px' }}>Ready</button>
                )}
                {order.status === "READY" && (
                  <button className={styles.primaryBtn} onClick={() => onUpdateStatus(order.orderId, "PICKED_UP")} style={{ marginRight: '5px' }}>Picked Up</button>
                )}
                <br />
                <button
                disabled={order.status !== "PENDING" && order.status !== "PAID"}
                onClick={() => {
                  if (window.confirm("Are you sure you want to cancel the order?")) {
                    onCancelOrder(order.orderId);
                  }
                }}
                className={styles.dangerBtn}
                style={{ marginTop: "5px" }}
              >
                Cancel Order
              </button>
            </td>
          </tr>
        ))}
      </tbody>
      </table>
    </div>
  );
};

export default OrderTable;