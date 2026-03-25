import {
  type Product,
  type Category,
  type CartItem,
  type SelectedOption,
} from "../../../shared/types";

export interface MenuListProps {
  products: Product[];
  categories: Category[];
  onAddToCart: (
    product: Product,
    selectedOptions: SelectedOption[],
    unitPrice: number
  ) => void;
}

export interface CategoryNavProps {
  categories: Category[];
  activeCategoryId: number | null;
  onCategoryClick: (categoryId: number) => void;
}

export interface CartProps {
  products: Product[];
  cartItems: CartItem[];
  totalAmount: number;
  onUpdateQuantity: (cartKey: string, delta: number) => void;
  onRemoveItem: (cartKey: string) => void;
}

export interface OrderFormProps {
  user: { name?: string; email?: string } | null | undefined;
  cartItems: CartItem[];
  totalAmount: number;
  onClearCart: () => void;
}

export interface OrderHistoryProps {
  products: Product[];
}

export interface ProductOptionModalProps {
  product: Product;
  onClose: () => void;
  onAddToCart: (
    product: Product,
    selectedOptions: SelectedOption[],
    unitPrice: number
  ) => void;
}
