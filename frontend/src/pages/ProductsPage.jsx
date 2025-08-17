import React, { useState, useEffect } from "react";
import Layout from "../components/Layout/Layout";
import Modal from "../components/UI/Modal";
import LoadingSpinner from "../components/UI/LoadingSpinner";
import Toast from "../components/UI/Toast";
import { useToast } from "../hooks/useToast";
import { productAPI } from "../utils/api";
import Pagination from "../components/UI/Pagination";
import { useAuth } from "../context/AuthContext";
import {
  Plus,
  Search,
  Edit,
  Trash2,
  Package,
  AlertTriangle,
} from "lucide-react";

const ProductsPage = () => {
  const [products, setProducts] = useState([]);
  const [pageInfo, setPageInfo] = useState({
    page: 0,
    size: 12,
    totalPages: 0,
    totalElements: 0,
  });
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("");
  const [categories, setCategories] = useState([]);
  const [formData, setFormData] = useState({
    productName: "",
    productDescription: "",
    pricePerItem: 0,
    productQuantity: 0,
    productCategory: "",
    productCode: "",
  });
  const { toasts, showSuccess, showError, removeToast } = useToast();
  const { isAdmin } = useAuth();

  // Fetch products whenever paging or filters change
  useEffect(() => {
    fetchProducts(pageInfo.page);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pageInfo.page, categoryFilter, searchTerm]);

  // When filters change, reset page to 0 once (so pagination works with filters)
  useEffect(() => {
    setPageInfo((pi) => ({ ...pi, page: 0 }));
  }, [categoryFilter, searchTerm]);

  // Load all categories (independent of current page) so dropdown always shows all
  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const { data } = await productAPI.getAll();
        const cats = [
          ...new Set(
            (data || []).map((p) => p.productCategory).filter(Boolean)
          ),
        ].sort((a, b) => a.localeCompare(b));
        if (active) setCategories(cats);
      } catch (_) {
        // ignore category load errors in UI
      }
    })();
    return () => {
      active = false;
    };
  }, []);

  const fetchProducts = async (page = 0) => {
    try {
      // Prefer server-side filtering/search if available
      let pageData;
      if (searchTerm) {
        const { data } = await productAPI.searchByNamePaged(
          searchTerm,
          page,
          pageInfo.size
        );
        pageData = data;
      } else if (categoryFilter) {
        const { data } = await productAPI.getByCategoryPaged(
          categoryFilter,
          page,
          pageInfo.size
        );
        pageData = data;
      } else {
        const { data } = await productAPI.getPaged(page, pageInfo.size);
        pageData = data;
      }

      setProducts(pageData?.content || []);
      setPageInfo((pi) => ({
        ...pi,
        page: pageData?.page ?? page,
        size: pageData?.size ?? pi.size,
        totalPages: pageData?.totalPages ?? 0,
        totalElements: pageData?.totalElements ?? 0,
      }));
    } catch (error) {
      showError("Failed to fetch products");
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      if (!isAdmin) {
        showError("Not authorized");
        return;
      }
      if (editingProduct) {
        const response = await productAPI.update(
          editingProduct.productId,
          formData
        );
        showSuccess(response.data?.message || "Product updated");
      } else {
        const response = await productAPI.add(formData);
        showSuccess(response.data?.message || "Product added");
      }

      await fetchProducts(pageInfo.page);
      // Ensure new category appears in dropdown if newly added/edited
      if (formData.productCategory) {
        const cat = String(formData.productCategory);
        setCategories((prev) =>
          prev.includes(cat)
            ? prev
            : [...prev, cat].sort((a, b) => a.localeCompare(b))
        );
      }
      setIsModalOpen(false);
      resetForm();
    } catch (error) {
      const msg =
        error?.response?.data?.message || error?.message || "Operation failed";
      showError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = (product) => {
    if (!isAdmin) {
      showError("Not authorized");
      return;
    }
    setEditingProduct(product);
    setFormData({
      productId: product.productId,
      productName: product.productName,
      productDescription: product.productDescription || "",
      pricePerItem: product.pricePerItem,
      productQuantity: product.productQuantity,
      productCategory: product.productCategory || "",
      productCode: product.productCode,
    });
    setIsModalOpen(true);
  };

  const handleDelete = async (id) => {
    if (!isAdmin) {
      showError("Not authorized");
      return;
    }
    if (window.confirm("Are you sure you want to delete this product?")) {
      try {
        const response = await productAPI.delete(id);
        showSuccess(response.data?.message || "Product deleted");
        await fetchProducts(pageInfo.page);
      } catch (error) {
        const msg =
          error?.response?.data?.message ||
          error?.message ||
          "Failed to delete product";
        showError(msg);
      }
    }
  };

  const resetForm = () => {
    setFormData({
      productName: "",
      productDescription: "",
      pricePerItem: 0,
      productQuantity: 0,
      productCategory: "",
      productCode: "",
    });
    setEditingProduct(null);
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({
      ...formData,
      [name]: ["pricePerItem", "productQuantity"].includes(name)
        ? parseFloat(value) || 0
        : value,
    });
  };

  // categories provided by state loaded from all products

  if (loading && products.length === 0) {
    return (
      <Layout>
        <div className="flex justify-center items-center h-64">
          <LoadingSpinner size="lg" />
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex justify-between items-center">
          <h1 className="text-2xl font-bold text-gray-900">Products</h1>
          {isAdmin && (
            <button
              onClick={() => setIsModalOpen(true)}
              className="btn-primary flex items-center"
            >
              <Plus className="w-4 h-4 mr-2" />
              Add Product
            </button>
          )}
        </div>

        {/* Filters */}
        <div className="flex flex-col sm:flex-row gap-4">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
            <input
              type="text"
              placeholder="Search products..."
              className="input-field pl-10"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
          <select
            className="input-field sm:w-48"
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
          >
            <option value="">All Categories</option>
            {categories.map((category) => (
              <option key={category} value={category}>
                {category}
              </option>
            ))}
          </select>
        </div>

        {/* Products Grid (ordered by backend: low-stock first, then name) */}
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
          {products.map((product) => (
            <div key={product.productId} className="card p-3">
              <div className="flex items-start justify-between">
                <div className="flex items-center">
                  <div className="bg-primary-100 p-1 rounded-full">
                    <Package className="w-5 h-5 text-primary-600" />
                  </div>
                  <div className="ml-2">
                    <h3 className="text-base font-medium text-gray-900">
                      {product.productName}
                    </h3>
                    <p className="text-xs text-gray-500">
                      {product.productCode}
                    </p>
                  </div>
                </div>
                {isAdmin && (
                  <div className="flex space-x-2">
                    <button
                      onClick={() => handleEdit(product)}
                      className="text-gray-400 hover:text-primary-600 transition-colors duration-200"
                    >
                      <Edit className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => handleDelete(product.productId)}
                      className="text-gray-400 hover:text-red-600 transition-colors duration-200"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                )}
              </div>

              <div className="mt-2 space-y-1 text-sm text-gray-600">
                {product.productDescription && (
                  <p className="text-xs text-gray-600">
                    {product.productDescription}
                  </p>
                )}
                <p>
                  <span className="font-medium">Price:</span> ₹
                  {product.pricePerItem.toFixed(2)}
                </p>
                <div className="flex items-center justify-between">
                  <p className="text-xs text-gray-600">
                    <span className="font-medium">Stock:</span>{" "}
                    {product.productQuantity}
                  </p>
                  {product.productQuantity < 10 && (
                    <div className="flex items-center text-red-600">
                      <AlertTriangle className="w-4 h-4 mr-1" />
                      <span className="text-xs">Low</span>
                    </div>
                  )}
                </div>
                {product.productCategory && (
                  <p className="text-xs text-gray-600">
                    <span className="font-medium">Category:</span>{" "}
                    <span className="px-2 py-0.5 bg-gray-100 rounded-full text-xs">
                      {product.productCategory}
                    </span>
                  </p>
                )}
                <p className="text-sm font-medium text-gray-900">
                  Total: ₹{product.productTotalPrice.toFixed(2)}
                </p>
              </div>
            </div>
          ))}
        </div>

        <Pagination
          page={pageInfo.page}
          size={pageInfo.size}
          totalPages={pageInfo.totalPages}
          totalElements={pageInfo.totalElements}
          onPageChange={(p) => setPageInfo((pi) => ({ ...pi, page: p }))}
        />

        {products.length === 0 && (
          <div className="text-center py-8">
            <Package className="w-12 h-12 mx-auto text-gray-300 mb-4" />
            <p className="text-gray-500">No products found</p>
          </div>
        )}

        {/* Add/Edit Product Modal */}
        <Modal
          isOpen={isModalOpen}
          onClose={() => {
            setIsModalOpen(false);
            resetForm();
          }}
          title={editingProduct ? "Edit Product" : "Add New Product"}
        >
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Product Name *
              </label>
              <input
                type="text"
                name="productName"
                required
                className="input-field"
                value={formData.productName}
                onChange={handleChange}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Description
              </label>
              <textarea
                name="productDescription"
                rows="3"
                className="input-field"
                value={formData.productDescription}
                onChange={handleChange}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Price per Item *
                </label>
                <input
                  type="number"
                  name="pricePerItem"
                  min="0"
                  step="0.01"
                  required
                  className="input-field"
                  value={formData.pricePerItem}
                  onChange={handleChange}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Quantity *
                </label>
                <input
                  type="number"
                  name="productQuantity"
                  min="0"
                  required
                  className="input-field"
                  value={formData.productQuantity}
                  onChange={handleChange}
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Category
              </label>
              <input
                type="text"
                name="productCategory"
                className="input-field"
                value={formData.productCategory}
                onChange={handleChange}
                placeholder="e.g., Electronics, Furniture"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Product Code
              </label>
              <input
                type="text"
                name="productCode"
                className="input-field"
                value={formData.productCode}
                onChange={handleChange}
                placeholder="Leave empty to auto-generate"
              />
            </div>

            <div className="flex justify-end space-x-3 pt-4">
              <button
                type="button"
                onClick={() => {
                  setIsModalOpen(false);
                  resetForm();
                }}
                className="btn-secondary"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={loading}
                className="btn-primary flex items-center"
              >
                {loading ? (
                  <LoadingSpinner size="sm" />
                ) : (
                  <>{editingProduct ? "Update" : "Add"} Product</>
                )}
              </button>
            </div>
          </form>
        </Modal>
      </div>

      {toasts.map((toast) => (
        <Toast
          key={toast.id}
          message={toast.message}
          type={toast.type}
          duration={toast.duration}
          onClose={() => removeToast(toast.id)}
        />
      ))}
    </Layout>
  );
};

export default ProductsPage;
