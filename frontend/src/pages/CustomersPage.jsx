import React, { useState, useEffect } from "react";
import Layout from "../components/Layout/Layout";
import Modal from "../components/UI/Modal";
import LoadingSpinner from "../components/UI/LoadingSpinner";
import Toast from "../components/UI/Toast";
import { useToast } from "../hooks/useToast";
import { customerAPI } from "../utils/api";
import Pagination from "../components/UI/Pagination";
import { Plus, Search, Edit, Trash2, User } from "lucide-react";

const CustomersPage = () => {
  const [customers, setCustomers] = useState([]);
  const [pageInfo, setPageInfo] = useState({
    page: 0,
    size: 10,
    totalPages: 0,
    totalElements: 0,
  });
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingCustomer, setEditingCustomer] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [formData, setFormData] = useState({
    customerName: "",
    customerEmail: "",
    customerContact: "",
    customerAddress: "",
    customerActiveStatus: "active",
    customerCredits: 0,
  });
  const { toasts, showSuccess, showError, removeToast } = useToast();

  useEffect(() => {
    fetchCustomers(pageInfo.page);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pageInfo.page, searchTerm]);

  const fetchCustomers = async (page = 0) => {
    try {
      const { data } = searchTerm
        ? await customerAPI.searchPaged(searchTerm, page, pageInfo.size)
        : await customerAPI.getPaged(page, pageInfo.size);
      setCustomers(data?.content || []);
      setPageInfo((pi) => ({
        ...pi,
        page: data?.page ?? page,
        size: data?.size ?? pi.size,
        totalPages: data?.totalPages ?? 0,
        totalElements: data?.totalElements ?? 0,
      }));
    } catch (error) {
      showError("Failed to fetch customers");
    } finally {
      setLoading(false);
    }
  };

  // Reset to first page when search query changes
  useEffect(() => {
    setPageInfo((pi) => ({ ...pi, page: 0 }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchTerm]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      if (editingCustomer) {
        const response = await customerAPI.update(
          editingCustomer.customerContact,
          formData
        );
        showSuccess(response.data?.message || "Customer updated");
      } else {
        const response = await customerAPI.add(formData);
        showSuccess(response.data?.message || "Customer added");
      }

      await fetchCustomers(pageInfo.page);
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

  const handleEdit = (customer) => {
    setEditingCustomer(customer);
    setFormData({
      customerId: customer.customerId,
      customerName: customer.customerName,
      customerEmail: customer.customerEmail,
      customerContact: customer.customerContact,
      customerAddress: customer.customerAddress || "",
      customerActiveStatus: customer.customerActiveStatus,
      customerCredits: customer.customerCredits,
    });
    setIsModalOpen(true);
  };

  const handleDelete = async (contact) => {
    if (window.confirm("Are you sure you want to delete this customer?")) {
      try {
        const response = await customerAPI.delete(contact);
        showSuccess(response.data?.message || "Customer deleted");
        await fetchCustomers(pageInfo.page);
      } catch (error) {
        const msg =
          error?.response?.data?.message ||
          error?.message ||
          "Failed to delete customer";
        showError(msg);
      }
    }
  };

  const resetForm = () => {
    setFormData({
      customerName: "",
      customerEmail: "",
      customerContact: "",
      customerAddress: "",
      customerActiveStatus: "active",
      customerCredits: 0,
    });
    setEditingCustomer(null);
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({
      ...formData,
      [name]: name === "customerCredits" ? parseFloat(value) || 0 : value,
    });
  };

  const filteredCustomers = customers; // server-side search handles filtering

  if (loading && customers.length === 0) {
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
          <h1 className="text-2xl font-bold text-gray-900">Customers</h1>
          <button
            onClick={() => setIsModalOpen(true)}
            className="btn-primary flex items-center"
          >
            <Plus className="w-4 h-4 mr-2" />
            Add Customer
          </button>
        </div>

        {/* Search */}
        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
          <input
            type="text"
            placeholder="Search customers..."
            className="input-field pl-10"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>

        {/* Customers Table (list with columns) */}
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Name
                </th>
                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Contact
                </th>
                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Email
                </th>
                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Address
                </th>
                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-4 py-2 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Credits
                </th>
                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Registered
                </th>
                <th className="px-4 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {filteredCustomers.map((customer) => (
                <tr key={customer.customerId} className="hover:bg-gray-50">
                  <td className="px-4 py-3 whitespace-nowrap">
                    <div className="flex items-center gap-3">
                      <div className="h-8 w-8 rounded-full bg-primary-50 flex items-center justify-center">
                        <User className="w-4 h-4 text-primary-600" />
                      </div>
                      <div>
                        <p className="text-sm font-medium text-gray-900">
                          {customer.customerName}
                        </p>
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-700">
                    {customer.customerContact}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-700">
                    {customer.customerEmail}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-700">
                    {customer.customerAddress || "-"}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    <span
                      className={`px-2 py-1 rounded-full text-xs ${
                        customer.customerActiveStatus === "active"
                          ? "bg-green-100 text-green-800"
                          : "bg-red-100 text-red-800"
                      }`}
                    >
                      {customer.customerActiveStatus}
                    </span>
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-900 text-right">
                    ₹{(customer.customerCredits || 0).toFixed(2)}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                    {customer.customerRegistrationDate || "-"}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap text-center">
                    <div className="flex items-center justify-center gap-2">
                      <button
                        onClick={() => handleEdit(customer)}
                        className="text-gray-400 hover:text-primary-600"
                      >
                        <Edit className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => handleDelete(customer.customerContact)}
                        className="text-gray-400 hover:text-red-600"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <Pagination
          page={pageInfo.page}
          size={pageInfo.size}
          totalPages={pageInfo.totalPages}
          totalElements={pageInfo.totalElements}
          onPageChange={(p) => setPageInfo((pi) => ({ ...pi, page: p }))}
        />

        {filteredCustomers.length === 0 && (
          <div className="text-center py-8">
            <User className="w-12 h-12 mx-auto text-gray-300 mb-4" />
            <p className="text-gray-500">No customers found</p>
          </div>
        )}

        {/* Add/Edit Customer Modal */}
        <Modal
          isOpen={isModalOpen}
          onClose={() => {
            setIsModalOpen(false);
            resetForm();
          }}
          title={editingCustomer ? "Edit Customer" : "Add New Customer"}
        >
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Name *
              </label>
              <input
                type="text"
                name="customerName"
                required
                className="input-field"
                value={formData.customerName}
                onChange={handleChange}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Email
              </label>
              <input
                type="email"
                name="customerEmail"
                className="input-field"
                value={formData.customerEmail}
                onChange={handleChange}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Contact *
              </label>
              <input
                type="tel"
                name="customerContact"
                pattern="[0-9]{10}"
                required
                className="input-field"
                placeholder="10-digit phone number"
                value={formData.customerContact}
                onChange={handleChange}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Address
              </label>
              <textarea
                name="customerAddress"
                rows="3"
                className="input-field"
                value={formData.customerAddress}
                onChange={handleChange}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Status
              </label>
              <select
                name="customerActiveStatus"
                className="input-field"
                value={formData.customerActiveStatus}
                onChange={handleChange}
              >
                <option value="active">Active</option>
                <option value="inactive">Inactive</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Credits
              </label>
              <input
                type="number"
                name="customerCredits"
                min="0"
                step="0.01"
                className="input-field"
                value={formData.customerCredits}
                onChange={handleChange}
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
                  <>{editingCustomer ? "Update" : "Add"} Customer</>
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

export default CustomersPage;
