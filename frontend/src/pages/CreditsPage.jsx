import React, { useState, useEffect } from "react";
import Layout from "../components/Layout/Layout";
import Modal from "../components/UI/Modal";
import LoadingSpinner from "../components/UI/LoadingSpinner";
import Toast from "../components/UI/Toast";
import { useToast } from "../hooks/useToast";
import { customerAPI, billingAPI } from "../utils/api";
import Pagination from "../components/UI/Pagination";
import { CreditCard, User, DollarSign, Receipt } from "lucide-react";

const CreditsPage = () => {
  const [customersWithCredits, setCustomersWithCredits] = useState([]);
  const [pageInfo, setPageInfo] = useState({
    page: 0,
    size: 10,
    totalPages: 0,
    totalElements: 0,
  });
  const [loading, setLoading] = useState(true);
  const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false);
  const [selectedCustomer, setSelectedCustomer] = useState(null);
  const [paymentAmount, setPaymentAmount] = useState(0);
  const [paymentMethod, setPaymentMethod] = useState("cash");
  const { toasts, showSuccess, showError, removeToast } = useToast();
  const [search, setSearch] = useState("");
  const [globalCreditsTotal, setGlobalCreditsTotal] = useState(0);
  const [globalAverageCredits, setGlobalAverageCredits] = useState(0);

  useEffect(() => {
    fetchCustomersWithCredits(pageInfo.page);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pageInfo.page, search]);

  const fetchCustomersWithCredits = async (page = 0) => {
    try {
      const { data } = search
        ? await customerAPI.searchCreditsPaged(search, page, pageInfo.size)
        : await customerAPI.getCreditsPaged(page, pageInfo.size);
      setCustomersWithCredits(data?.content || []);
      // v1.4.0: /customers/credits returns totalCredits & averageCredits across ALL matches
      if (!search) {
        const totalCredits =
          typeof data?.totalCredits === "number" ? data.totalCredits : 0;
        const avgCredits =
          typeof data?.averageCredits === "number" ? data.averageCredits : 0;
        setGlobalCreditsTotal(totalCredits);
        setGlobalAverageCredits(avgCredits);
      } else {
        // Search endpoint doesn't include aggregates; compute via one capped fetch for accurate global totals
        const total = data?.totalElements ?? 0;
        const pageLen = data?.content?.length ?? 0;
        if (total > 0) {
          const cappedTotal = Math.min(total, 5000);
          const fullRes = await customerAPI.searchCreditsPaged(
            search,
            0,
            cappedTotal
          );
          const list = fullRes?.data?.content || [];
          const sum = list.reduce((s, c) => s + (c?.customerCredits || 0), 0);
          setGlobalCreditsTotal(sum);
          setGlobalAverageCredits(list.length ? sum / (list.length || 1) : 0);
        } else {
          setGlobalCreditsTotal(0);
          setGlobalAverageCredits(0);
        }
      }
      setPageInfo((pi) => ({
        ...pi,
        page: data?.page ?? page,
        size: data?.size ?? pi.size,
        totalPages: data?.totalPages ?? 0,
        totalElements: data?.totalElements ?? 0,
      }));
    } catch (error) {
      showError("Failed to fetch customers with credits");
    } finally {
      setLoading(false);
    }
  };

  // Reset to first page on search term change
  useEffect(() => {
    setPageInfo((pi) => ({ ...pi, page: 0 }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search]);

  const handlePaymentSubmit = async (e) => {
    e.preventDefault();

    if (paymentAmount <= 0) {
      showError("Payment amount must be greater than 0");
      return;
    }

    if (paymentAmount > selectedCustomer.customerCredits) {
      showError("Payment amount cannot exceed available credits");
      return;
    }

    setLoading(true);

    try {
      const billData = {
        customer: selectedCustomer,
        billTotalAmount: paymentAmount,
        paymentMethod: paymentMethod,
        billType: "creditsPayment",
        billStatus: "paid",
      };

      const response = await billingAPI.creditBill(billData);
      showSuccess(
        `Payment processed successfully! Bill Number: ${response.data.billNumber}`
      );

      // Refresh the credits list
      await fetchCustomersWithCredits(pageInfo.page);

      // Close modal and reset form
      setIsPaymentModalOpen(false);
      setSelectedCustomer(null);
      setPaymentAmount(0);
      setPaymentMethod("cash");
    } catch (error) {
      const msg =
        error?.response?.data?.message ||
        error?.message ||
        "Failed to process payment";
      showError(msg);
    } finally {
      setLoading(false);
    }
  };

  const openPaymentModal = (customer) => {
    setSelectedCustomer(customer);
    setPaymentAmount(customer.customerCredits); // Default to full amount
    setIsPaymentModalOpen(true);
  };

  // Use global totals/averages for display (server-provided in v1.4.0 for non-search)
  const totalCredits = globalCreditsTotal;
  const averageCredits = globalAverageCredits;

  const filtered = customersWithCredits; // server-side search handles filtering

  if (loading && customersWithCredits.length === 0) {
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
          <h1 className="text-2xl font-bold text-gray-900">Customer Credits</h1>
          <div className="text-right">
            <p className="text-sm text-gray-500">Total Outstanding Credits</p>
            <p className="text-2xl font-bold text-red-600">
              ₹{totalCredits.toFixed(2)}
            </p>
          </div>
        </div>

        {/* Summary Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="card">
            <div className="flex items-center">
              <div className="p-3 rounded-lg bg-blue-100">
                <User className="w-6 h-6 text-blue-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">
                  Customers with Credits
                </p>
                <p className="text-2xl font-semibold text-gray-900">
                  {pageInfo.totalElements}
                </p>
              </div>
            </div>
            <Pagination
              page={pageInfo.page}
              size={pageInfo.size}
              totalPages={pageInfo.totalPages}
              totalElements={pageInfo.totalElements}
              onPageChange={(p) => setPageInfo((pi) => ({ ...pi, page: p }))}
            />
          </div>

          <div className="card">
            <div className="flex items-center">
              <div className="p-3 rounded-lg bg-red-100">
                <DollarSign className="w-6 h-6 text-red-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">
                  Total Credits
                </p>
                <p className="text-2xl font-semibold text-gray-900">
                  ₹{totalCredits.toFixed(2)}
                </p>
              </div>
            </div>
          </div>

          <div className="card">
            <div className="flex items-center">
              <div className="p-3 rounded-lg bg-green-100">
                <CreditCard className="w-6 h-6 text-green-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">
                  Average Credit
                </p>
                <p className="text-2xl font-semibold text-gray-900">
                  ₹{averageCredits.toFixed(2)}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Customers with Credits */}
        <div className="card">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-4">
            <h3 className="text-lg font-medium text-gray-900">
              Customers with Outstanding Credits
            </h3>
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search by name, contact or email"
              className="input-field sm:w-72"
            />
          </div>

          {customersWithCredits.length === 0 ? (
            <div className="text-center py-8">
              <CreditCard className="w-12 h-12 mx-auto text-gray-300 mb-4" />
              <p className="text-gray-500">
                No customers have outstanding credits
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Customer
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Contact
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Credits
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {filtered.map((customer) => (
                    <tr key={customer.customerId} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center">
                          <div className="bg-primary-100 p-2 rounded-full">
                            <User className="w-5 h-5 text-primary-600" />
                          </div>
                          <div className="ml-3">
                            <p className="text-sm font-medium text-gray-900">
                              {customer.customerName}
                            </p>
                            <p className="text-sm text-gray-500">
                              {customer.customerEmail}
                            </p>
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {customer.customerContact}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className="text-lg font-semibold text-red-600">
                          ₹{customer.customerCredits.toFixed(2)}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
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
                      <td className="px-6 py-4 whitespace-nowrap">
                        <button
                          onClick={() => openPaymentModal(customer)}
                          className="btn-primary text-sm flex items-center"
                        >
                          <Receipt className="w-4 h-4 mr-1" />
                          Collect Payment
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Payment Modal */}
        <Modal
          isOpen={isPaymentModalOpen}
          onClose={() => {
            setIsPaymentModalOpen(false);
            setSelectedCustomer(null);
            setPaymentAmount(0);
            setPaymentMethod("cash");
          }}
          title="Collect Credit Payment"
        >
          {selectedCustomer && (
            <form onSubmit={handlePaymentSubmit} className="space-y-4">
              <div className="bg-gray-50 p-4 rounded-lg">
                <h4 className="font-medium text-gray-900 mb-2">
                  Customer Details
                </h4>
                <p className="text-sm text-gray-600">
                  <span className="font-medium">Name:</span>{" "}
                  {selectedCustomer.customerName}
                </p>
                <p className="text-sm text-gray-600">
                  <span className="font-medium">Contact:</span>{" "}
                  {selectedCustomer.customerContact}
                </p>
                <p className="text-sm text-gray-600">
                  <span className="font-medium">Total Credits:</span>{" "}
                  <span className="font-semibold text-red-600">
                    ₹{selectedCustomer.customerCredits.toFixed(2)}
                  </span>
                </p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Payment Amount *
                </label>
                <input
                  type="number"
                  min="0.01"
                  max={selectedCustomer.customerCredits}
                  step="0.01"
                  required
                  className="input-field"
                  value={paymentAmount}
                  onChange={(e) =>
                    setPaymentAmount(parseFloat(e.target.value) || 0)
                  }
                />
                <p className="text-xs text-gray-500 mt-1">
                  Maximum: ₹{selectedCustomer.customerCredits.toFixed(2)}
                </p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Payment Method *
                </label>
                <select
                  className="input-field"
                  value={paymentMethod.toLowerCase()}
                  onChange={(e) => setPaymentMethod(e.target.value)}
                  required
                >
                  <option value="cash">Cash</option>
                  <option value="card">Card</option>
                  <option value="online">Online</option>
                </select>
              </div>

              <div className="flex justify-end space-x-3 pt-4">
                <button
                  type="button"
                  onClick={() => {
                    setIsPaymentModalOpen(false);
                    setSelectedCustomer(null);
                    setPaymentAmount(0);
                    setPaymentMethod("cash");
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
                    <>
                      <Receipt className="w-4 h-4 mr-2" />
                      Process Payment
                    </>
                  )}
                </button>
              </div>
            </form>
          )}
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

export default CreditsPage;
