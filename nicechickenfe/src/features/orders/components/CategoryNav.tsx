import type { CategoryNavProps } from "../types/order.types";
import styles from "./OrderPage.module.css";

const CategoryNav = ({
  categories,
  activeCategoryId,
  onCategoryClick,
}: CategoryNavProps) => {
  if (categories.length === 0) return null;

  return (
    <nav className={styles.categoryNav}>
      {categories.map((cat) => (
        <button
          key={cat.id}
          className={`${styles.categoryTab} ${
            activeCategoryId === cat.id ? styles.categoryTabActive : ""
          }`}
          onClick={() => onCategoryClick(cat.id)}
        >
          {cat.name}
        </button>
      ))}
    </nav>
  );
};

export default CategoryNav;
