import React from "react";
import { type AdminOrderResponse, type Product } from "../../../shared/types";

export interface OrderTableProps {
  orders: AdminOrderResponse[];
  products: Product[];
  onUpdateStatus: (id: string, status: string) => void;
  onCancelOrder: (id: string) => void;
}

export interface OrderPaginationProps {
  page: number;
  totalPages: number;
  setPage: React.Dispatch<React.SetStateAction<number>>;
}