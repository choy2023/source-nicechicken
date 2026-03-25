import type { OrderPaginationProps } from "../types/admin.types";
import styles from "./Admin.module.css";

const OrderPagination = ({ page, totalPages, setPage }: OrderPaginationProps) => {

  return (
    <div className={styles.pagination}>
      <button 
        className={styles.secondaryBtn}
        disabled={page === 0} 
        onClick={() => setPage((p) => Math.max(0, p - 1))}
      >
        Previous
      </button>
      <span>
        Page {page + 1} of {totalPages === 0 ? 1 : totalPages}
      </span>
      <button 
        className={styles.secondaryBtn}
        disabled={page >= totalPages - 1} 
        onClick={() => setPage((p) => p + 1)}
      >
        Next
      </button>
    </div>
  );
};

export default OrderPagination;