// ==============================
// Product & Option Types
// ==============================

export interface OptionDetail {
  optionName: string;
  extraPrice: number;
}

export interface OptionGroup {
  optionGroup: string;
  options: OptionDetail[];
}

export interface Category {
  id: number;
  name: string;
  sortOrder: number;
  active: boolean;
}

export interface Product {
  id: number;
  category: Category;
  name: string;
  basePrice: number;
  description: string;
  imageUrl: string;
  stockQuantity: number;
  optionGroups: OptionGroup[];
}

export interface OptionRequestItem {
  optionName: string;
  extraPrice: number;
}

export interface OptionGroupRequest {
  optionGroup: string;
  options: OptionRequestItem[];
}

export interface ProductRequest {
  categoryId: number;
  name: string;
  basePrice: number;
  description: string;
  imageUrl: string;
  stockQuantity: number;
  optionGroups: OptionGroupRequest[];
}

// ==============================
// Cart Types
// ==============================

export interface SelectedOption {
  optionGroup: string;
  optionName: string;
  extraPrice: number;
}

export interface CartItem {
  cartKey: string; // unique key: productId + sorted options
  productId: number;
  quantity: number;
  selectedOptions: SelectedOption[];
  unitPrice: number; // basePrice + sum of option extra prices
}

/** Generate a unique cart key from productId + selected options */
export function makeCartKey(
  productId: number,
  selectedOptions: SelectedOption[]
): string {
  const optionPart = [...selectedOptions]
    .sort((a, b) => a.optionGroup.localeCompare(b.optionGroup))
    .map((o) => `${o.optionGroup}:${o.optionName}`)
    .join("|");
  return `${productId}_${optionPart}`;
}

// ==============================
// Order Types
// ==============================

export interface OrderItem {
  productId: number;
  quantity: number;
  selectedOptions: { optionGroup: string; optionName: string }[];
}

export interface OrderRequest {
  customerName: string;
  customerPhone: string;
  customerEmail: string;
  items: OrderItem[];
  clientTotalAmount: number;
}

export interface OrderItemDetail {
  productId: number;
  quantity: number;
  priceAtOrder: number;
  selectedOptions: { optionGroup: string; optionName: string }[];
}

export interface AdminOrderResponse {
  orderId: string;
  customerName: string;
  customerPhone: string;
  totalAmount: number;
  status:
    | "PENDING"
    | "PAID"
    | "COOKING"
    | "READY"
    | "PICKED_UP"
    | "CANCELLED";
  items: OrderItemDetail[];
}

export interface UserOrderResponse {
  orderId: string;
  totalAmount: number;
  status:
    | "PENDING"
    | "PAID"
    | "COOKING"
    | "READY"
    | "PICKED_UP"
    | "CANCELLED";
  items: OrderItemDetail[];
}
