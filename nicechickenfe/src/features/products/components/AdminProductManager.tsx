import React, { useRef, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../../shared/api/axios';
import {
  type Product,
  type ProductRequest,
  type Category,
} from '../../../shared/types';
import styles from '../../admin/components/Admin.module.css';

const EMPTY_FORM: ProductRequest = {
  categoryId: 0,
  name: '',
  basePrice: 0,
  description: '',
  imageUrl: '',
  stockQuantity: 0,
  optionGroups: [],
};

const AdminProductManager: React.FC = () => {
  const queryClient = useQueryClient();
  const [editingId, setEditingId] = useState<number | null>(null);
  const [imagePreview, setImagePreview] = useState<string>('');
  const [isUploading, setIsUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [formData, setFormData] = useState<ProductRequest>({ ...EMPTY_FORM });

  const { data: categories = [] } = useQuery<Category[]>({
    queryKey: ['adminCategories'],
    queryFn: async () => (await api.get('/admin/categories')).data,
    staleTime: 60000,
  });

  const { data: products = [], isLoading } = useQuery<Product[]>({
    queryKey: ['products'],
    queryFn: async () => (await api.get('/products')).data,
  });

  const createMutation = useMutation({
    mutationFn: (data: ProductRequest) => api.post('/admin/products', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
      resetForm();
      alert('Menu item added.');
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: ProductRequest }) =>
      api.put(`/admin/products/${id}`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
      resetForm();
      alert('Menu item updated.');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => api.delete(`/admin/products/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
      alert('Menu item deleted.');
    },
  });

  const resetForm = () => {
    setEditingId(null);
    setFormData({ ...EMPTY_FORM, categoryId: categories[0]?.id ?? 0 });
    setImagePreview('');
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleEdit = (product: Product) => {
    setEditingId(product.id);
    setFormData({
      categoryId: product.category.id,
      name: product.name,
      basePrice: product.basePrice,
      description: product.description,
      imageUrl: product.imageUrl || '',
      stockQuantity: product.stockQuantity || 0,
      optionGroups: product.optionGroups.map((g) => ({
        optionGroup: g.optionGroup,
        options: g.options.map((o) => ({
          optionName: o.optionName,
          extraPrice: o.extraPrice,
        })),
      })),
    });
    setImagePreview(product.imageUrl || '');
  };

  const handleImageChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setImagePreview(URL.createObjectURL(file));
    setIsUploading(true);
    try {
      const payload = new FormData();
      payload.append('file', file);
      const res = await api.post('/admin/upload/image', payload, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setFormData((prev) => ({ ...prev, imageUrl: res.data.imageUrl }));
    } catch {
      alert('Image upload failed. Please try again.');
      setImagePreview('');
    } finally {
      setIsUploading(false);
    }
  };

  // ========== Option Group Handlers ==========

  const addOptionGroup = () => {
    setFormData((prev) => ({
      ...prev,
      optionGroups: [
        ...prev.optionGroups,
        { optionGroup: '', options: [{ optionName: '', extraPrice: 0 }] },
      ],
    }));
  };

  const removeOptionGroup = (groupIdx: number) => {
    setFormData((prev) => ({
      ...prev,
      optionGroups: prev.optionGroups.filter((_, i) => i !== groupIdx),
    }));
  };

  const updateGroupName = (groupIdx: number, name: string) => {
    setFormData((prev) => {
      const groups = [...prev.optionGroups];
      groups[groupIdx] = { ...groups[groupIdx], optionGroup: name };
      return { ...prev, optionGroups: groups };
    });
  };

  const addOption = (groupIdx: number) => {
    setFormData((prev) => {
      const groups = [...prev.optionGroups];
      groups[groupIdx] = {
        ...groups[groupIdx],
        options: [...groups[groupIdx].options, { optionName: '', extraPrice: 0 }],
      };
      return { ...prev, optionGroups: groups };
    });
  };

  const removeOption = (groupIdx: number, optIdx: number) => {
    setFormData((prev) => {
      const groups = [...prev.optionGroups];
      groups[groupIdx] = {
        ...groups[groupIdx],
        options: groups[groupIdx].options.filter((_, i) => i !== optIdx),
      };
      // Remove the group entirely if no options left
      if (groups[groupIdx].options.length === 0) {
        groups.splice(groupIdx, 1);
      }
      return { ...prev, optionGroups: groups };
    });
  };

  const updateOption = (
    groupIdx: number,
    optIdx: number,
    field: 'optionName' | 'extraPrice',
    value: string | number
  ) => {
    setFormData((prev) => {
      const groups = [...prev.optionGroups];
      const options = [...groups[groupIdx].options];
      options[optIdx] = { ...options[optIdx], [field]: value };
      groups[groupIdx] = { ...groups[groupIdx], options };
      return { ...prev, optionGroups: groups };
    });
  };

  // ========== Submit ==========

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.categoryId) return alert('Please select a category.');

    // Validate option groups: group name and option names must not be empty
    for (const group of formData.optionGroups) {
      if (!group.optionGroup.trim()) return alert('Option group name cannot be empty.');
      for (const opt of group.options) {
        if (!opt.optionName.trim()) return alert(`Option name in "${group.optionGroup}" cannot be empty.`);
      }
    }

    if (editingId) {
      updateMutation.mutate({ id: editingId, data: formData });
    } else {
      createMutation.mutate(formData);
    }
  };

  if (isLoading) return <div>Loading menu data...</div>;

  return (
    <div>
      <h2 className={styles.sectionTitle} style={{ marginTop: '20px' }}>Product Management</h2>

      <form onSubmit={handleSubmit} className={styles.formGrid}>
        {/* Category dropdown */}
        <select
          value={formData.categoryId}
          onChange={(e) => setFormData({ ...formData, categoryId: Number(e.target.value) })}
          className={styles.inputField}
          required
        >
          <option value={0} disabled>Select Category</option>
          {categories.map((cat) => (
            <option key={cat.id} value={cat.id}>{cat.name}</option>
          ))}
        </select>

        <input type="text" placeholder="Menu Name" value={formData.name}
          onChange={(e) => setFormData({ ...formData, name: e.target.value })}
          className={styles.inputField} required />
        <input type="number" step="0.01" placeholder="Price (Base Price)" value={formData.basePrice}
          onChange={(e) => setFormData({ ...formData, basePrice: parseFloat(e.target.value) })}
          className={styles.inputField} required />
        <input type="number" placeholder="Stock Quantity" value={formData.stockQuantity}
          onChange={(e) => setFormData({ ...formData, stockQuantity: parseInt(e.target.value, 10) || 0 })}
          className={styles.inputField} required />
        <input type="text" placeholder="Description" value={formData.description}
          onChange={(e) => setFormData({ ...formData, description: e.target.value })}
          className={`${styles.inputField} ${styles.fullWidth}`} required />

        {/* Image uploader */}
        <div className={`${styles.formActions} ${styles.fullWidth}`} style={{ alignItems: 'center' }}>
          <input ref={fileInputRef} type="file" accept="image/*" onChange={handleImageChange} style={{ display: 'none' }} />
          <button type="button" onClick={() => fileInputRef.current?.click()} disabled={isUploading}
            className={styles.secondaryBtn} style={{ marginRight: '1rem' }}>
            {isUploading ? 'Uploading...' : 'Choose Image'}
          </button>
          {imagePreview
            ? <img src={imagePreview} alt="preview" className={styles.productImg} />
            : <span style={{ color: '#999', fontSize: '0.9rem' }}>No image selected</span>}
        </div>

        {/* ========== Option Groups Editor ========== */}
        <div className={`${styles.fullWidth} ${styles.optionEditor}`}>
          <div className={styles.optionEditorHeader}>
            <h3 className={styles.optionEditorTitle}>Product Options</h3>
            <button type="button" onClick={addOptionGroup} className={styles.addGroupBtn}>
              + Add Option Group
            </button>
          </div>

          {formData.optionGroups.length === 0 && (
            <p className={styles.optionHint}>
              No options yet. Add groups like "Sauce", "Spice Level", "Sides" etc.
            </p>
          )}

          {formData.optionGroups.map((group, gIdx) => (
            <div key={gIdx} className={styles.optionGroupCard}>
              <div className={styles.optionGroupHeader}>
                <input
                  type="text"
                  placeholder="Group name (e.g. Sauce)"
                  value={group.optionGroup}
                  onChange={(e) => updateGroupName(gIdx, e.target.value)}
                  className={styles.optionGroupInput}
                />
                <button type="button" onClick={() => removeOptionGroup(gIdx)} className={styles.removeGroupBtn}>
                  Remove Group
                </button>
              </div>

              {group.options.map((opt, oIdx) => (
                <div key={oIdx} className={styles.optionRow}>
                  <input
                    type="text"
                    placeholder="Option name"
                    value={opt.optionName}
                    onChange={(e) => updateOption(gIdx, oIdx, 'optionName', e.target.value)}
                    className={styles.optionNameInput}
                  />
                  <div className={styles.optionPriceWrapper}>
                    <span className={styles.optionPriceLabel}>+$</span>
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      placeholder="0.00"
                      value={opt.extraPrice}
                      onChange={(e) => updateOption(gIdx, oIdx, 'extraPrice', parseFloat(e.target.value) || 0)}
                      className={styles.optionPriceInput}
                    />
                  </div>
                  <button type="button" onClick={() => removeOption(gIdx, oIdx)} className={styles.removeOptionBtn}>
                    ×
                  </button>
                </div>
              ))}

              <button type="button" onClick={() => addOption(gIdx)} className={styles.addOptionBtn}>
                + Add Option
              </button>
            </div>
          ))}
        </div>

        {/* Submit buttons */}
        <div className={`${styles.formActions} ${styles.fullWidth}`}>
          <button type="submit" disabled={createMutation.isPending || updateMutation.isPending || isUploading}
            className={styles.primaryBtn}>
            {editingId ? 'Update Menu' : 'Add New Menu'}
          </button>
          {editingId && (
            <button type="button" onClick={resetForm} className={styles.secondaryBtn}>Cancel</button>
          )}
        </div>
      </form>

      {/* Product table */}
      <div className={styles.tableContainer}>
        <table className={styles.adminTable}>
          <thead>
            <tr>
              <th>ID</th>
              <th>Image</th>
              <th>Category</th>
              <th>Name</th>
              <th>Price</th>
              <th>Options</th>
              <th>Stock</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {products.map((product) => (
              <tr key={product.id}>
                <td>{product.id}</td>
                <td>
                  <img src={product.imageUrl || 'https://placehold.co/50x50?text=No+Image'}
                    alt={product.name} className={styles.productImg} />
                </td>
                <td style={{ color: '#666', fontSize: '0.9rem' }}>{product.category?.name}</td>
                <td style={{ fontWeight: 'bold' }}>{product.name}</td>
                <td>${product.basePrice.toFixed(2)}</td>
                <td className={styles.optionBadges}>
                  {product.optionGroups.length === 0
                    ? <span style={{ color: '#999' }}>—</span>
                    : product.optionGroups.map((g) => (
                      <span key={g.optionGroup} className={styles.optionBadge}>
                        {g.optionGroup} ({g.options.length})
                      </span>
                    ))}
                </td>
                <td className={product.stockQuantity <= 0 ? styles.outOfStock : styles.inStock}>
                  {product.stockQuantity <= 0 ? 'Out of Stock' : `${product.stockQuantity} units`}
                </td>
                <td>
                  <div className={styles.actionLinks}>
                    <button onClick={() => handleEdit(product)} className={styles.editLink}>Edit</button>
                    <button
                      onClick={() => { if (window.confirm('Are you sure you want to delete this?')) deleteMutation.mutate(product.id); }}
                      className={styles.deleteLink}>Delete</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default AdminProductManager;
