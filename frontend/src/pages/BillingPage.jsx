import React, { useState, useEffect } from "react";
import Layout from "../components/Layout/Layout";
import Modal from "../components/UI/Modal";
import LoadingSpinner from "../components/UI/LoadingSpinner";
import Toast from "../components/UI/Toast";
import { useToast } from "../hooks/useToast";
import { billingAPI, customerAPI, productAPI } from "../utils/api";
import { toEnum } from "../utils/enumUtils";
import {
  Plus,
  Search,
  Receipt,
  Trash2,
  ShoppingCart,
  User,
  Package,
  Calculator,
  X,
} from "lucide-react";

const BillingPage = () => {
  const [customers, setCustomers] = useState([]);
  const [products, setProducts] = useState([]);
  const [selectedCustomer, setSelectedCustomer] = useState(null);
  const [billItems, setBillItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [isCustomerModalOpen, setIsCustomerModalOpen] = useState(false);
  const [isAddCustomerModalOpen, setIsAddCustomerModalOpen] = useState(false);
  const [isProductModalOpen, setIsProductModalOpen] = useState(false);
  const [searchCustomer, setSearchCustomer] = useState("");
  const [searchProduct, setSearchProduct] = useState("");
  const [nameSuggestions, setNameSuggestions] = useState([]);
  const [isSuggesting, setIsSuggesting] = useState(false);
  const [billType, setBillType] = useState("new");
  const [paymentMethod, setPaymentMethod] = useState("cash");
  const [originalBillNumber, setOriginalBillNumber] = useState("");
  const [showBillModal, setShowBillModal] = useState(false);
  const [createdBill, setCreatedBill] = useState(null);
  // One-time tip for returns
  const [showReturnTip, setShowReturnTip] = useState(false);
  const [neverShowReturnTip, setNeverShowReturnTip] = useState(false);
  const { toasts, showSuccess, showError, removeToast } = useToast();

  // Add Customer form state
  const [newCustomerName, setNewCustomerName] = useState("");
  const [newCustomerContact, setNewCustomerContact] = useState("");
  const [newCustomerEmail, setNewCustomerEmail] = useState("");

  useEffect(() => {
    fetchCustomers();
    fetchProducts();
  }, []);

  // Debounced name search for products
  useEffect(() => {
    if (!searchProduct || searchProduct.trim().length < 2) {
      setNameSuggestions([]);
      return;
    }

    let cancelled = false;
    setIsSuggesting(true);
    const t = setTimeout(async () => {
      try {
        const resp = await productAPI.searchByName(searchProduct.trim());
        if (!cancelled) {
          // backend may return 204 No Content; handle gracefully
          setNameSuggestions(resp.data || []);
        }
      } catch (err) {
        // if 204 or no results, ensure suggestions empty
        setNameSuggestions([]);
      } finally {
        if (!cancelled) setIsSuggesting(false);
      }
    }, 300);

    return () => {
      cancelled = true;
      clearTimeout(t);
    };
  }, [searchProduct]);

  // When switching to return flow, show a one-time tip unless hidden
  useEffect(() => {
    if (billType === "return") {
      try {
        const hidden = localStorage.getItem("bnx_hideReturnTip") === "true";
        if (!hidden) setShowReturnTip(true);
      } catch (_) {
        // localStorage might fail in some environments; ignore
        setShowReturnTip(true);
      }
    }
  }, [billType]);

  const fetchCustomers = async () => {
    try {
      const response = await customerAPI.getAll();
      setCustomers(response.data);
    } catch (error) {
      showError("Failed to fetch customers");
    }
  };

  const fetchProducts = async () => {
    try {
      const response = await productAPI.getAll();
      setProducts(response.data);
    } catch (error) {
      showError("Failed to fetch products");
    }
  };

  const addProductToBill = (product) => {
    const existingItem = billItems.find(
      (item) => item.billItemProduct.productId === product.productId
    );

    if (existingItem) {
      setBillItems(
        billItems.map((item) =>
          item.billItemProduct.productId === product.productId
            ? { ...item, billItemQuantity: item.billItemQuantity + 1 }
            : item
        )
      );
    } else {
      setBillItems([
        ...billItems,
        {
          billItemProduct: product,
          billItemQuantity: 1,
          pricePerUnit: product.pricePerItem,
          billItemDiscountPerUnit: 0,
        },
      ]);
    }
    setIsProductModalOpen(false);
  };

  const updateItemDiscount = (productId, discount) => {
    setBillItems(
      billItems.map((item) =>
        item.billItemProduct.productId === productId
          ? { ...item, billItemDiscountPerUnit: discount }
          : item
      )
    );
  };

  const updateItemQuantity = (productId, qty) => {
    setBillItems((prev) =>
      prev.map((item) => {
        if (item.billItemProduct.productId !== productId) return item;
        // Allow empty while typing, normalize on blur/valid
        if (qty === "" || qty === null || Number.isNaN(qty)) {
          return { ...item, billItemQuantity: "" };
        }
        let n = Number(qty);
        // enforce integer and minimum of 1
        n = Math.floor(n);
        if (n < 1) n = 1;
        // For returns, cap max quantity to the originally billed quantity
        if (billType === "return") {
          const maxQty = Math.max(
            item.billItemProduct.productQuantity || item.billItemQuantity || 1,
            1
          );
          n = Math.min(n, maxQty);
        }
        return { ...item, billItemQuantity: n };
      })
    );
  };

  const removeItem = (productId) => {
    setBillItems(
      billItems.filter((item) => item.billItemProduct.productId !== productId)
    );
  };

  const calculateTotal = () => {
    return billItems.reduce((total, item) => {
      // Guard against temporary empty/invalid qty while typing
      const qty = Number(item.billItemQuantity);
      const safeQty = Number.isFinite(qty) && qty > 0 ? qty : 0;
      // Apply discount per unit times quantity to match backend logic
      return (
        total +
        item.pricePerUnit * safeQty -
        item.billItemDiscountPerUnit * safeQty
      );
    }, 0);
  };

  const handleCreateBill = async () => {
    if (!selectedCustomer) {
      showError("Please select a customer");
      return;
    }

    if (billItems.length === 0) {
      showError("Please add at least one item to the bill");
      return;
    }

    setLoading(true);

    try {
      // Normalize and prepare payload to match backend DTOs
      const allowedBillTypes = ["new", "creditsPayment", "return"];
      const isReturn = billType === "return";
      let finalBillType = allowedBillTypes.includes(billType)
        ? billType
        : "new";
      // Per new requirement: send billType as 'new' for returns; backend will interpret appropriately
      if (isReturn) finalBillType = "new";
      if (paymentMethod === "credit") finalBillType = "credits_Payment";

      // derive bill status automatically
      let finalBillStatus = "complete";
      if (finalBillType === "new") finalBillStatus = "complete";
      if (isReturn) finalBillStatus = "returned";
      // fallback to complete for any unexpected value

      const billItemsDto = billItems.map((item) => ({
        billItemProduct: { productId: item.billItemProduct.productId },
        billItemQuantity: item.billItemQuantity,
        pricePerUnit: item.pricePerUnit,
        billItemDiscountPerUnit: item.billItemDiscountPerUnit,
      }));

      const billData = {
        customer: { customerId: selectedCustomer.customerId },
        billItems: billItemsDto,
        billType: toEnum(finalBillType),
        billStatus: toEnum(finalBillStatus),
        paymentMethod: toEnum(paymentMethod),
        billTotalAmount: calculateTotal(),
      };

      let response;
      if (isReturn) {
        // For returns, send billDto with billNumber, items and type/status
        const returnDto = {
          billNumber: originalBillNumber,
          billItems: billItemsDto,
          paymentMethod: toEnum(paymentMethod),
          billType: toEnum(finalBillType),
          billStatus: toEnum(finalBillStatus),
        };
        response = await billingAPI.returnBill(returnDto);
        showSuccess(
          `Return processed. New Bill Number: ${response.data.billNumber}`
        );
      } else {
        response = await billingAPI.createBill(billData);
        showSuccess(
          `Bill created successfully! Bill Number: ${response.data.billNumber}`
        );
      }

      // show created/returned bill in modal
      setCreatedBill(response.data);
      setShowBillModal(true);

      // Reset form
      setSelectedCustomer(null);
      setBillItems([]);
      setBillType("new");
      setPaymentMethod("cash");

      // Refresh products to update stock
      await fetchProducts();
    } catch (error) {
      const msg =
        error?.response?.data?.message ||
        error?.message ||
        "Failed to create bill";
      showError(msg);
    } finally {
      setLoading(false);
    }
  };

  const filteredCustomers = customers.filter(
    (customer) =>
      customer.customerName
        .toLowerCase()
        .includes(searchCustomer.toLowerCase()) ||
      customer.customerContact.includes(searchCustomer)
  );

  const filteredProducts = products.filter(
    (product) =>
      product.productName.toLowerCase().includes(searchProduct.toLowerCase()) ||
      product.productCode.toLowerCase().includes(searchProduct.toLowerCase())
  );

  return (
    <Layout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex justify-between items-center">
          <h1 className="text-2xl font-bold text-gray-900">Create New Bill</h1>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-3 px-4 lg:px-6">
          {/* Left: Customer + Bill Settings combined */}
          <div className="card flex flex-col lg:sticky lg:top-6 lg:self-start">
            {/* Customer (moved from right) */}
            <div className="relative mb-4">
              <h2 className="text-lg font-medium text-gray-900 mb-4 flex items-center">
                <User className="w-4 h-4 mr-2" />
                Customer
              </h2>

              {selectedCustomer ? (
                <div className="flex items-center justify-between">
                  <button
                    type="button"
                    onClick={() => setSelectedCustomer(null)}
                    className="absolute top-2 right-2 p-1 rounded text-gray-400 hover:text-gray-600 hover:bg-gray-100"
                    aria-label="Remove customer"
                    title="Remove customer"
                  >
                    <X className="w-4 h-4" />
                  </button>

                  <div className="flex items-center gap-3">
                    <div className="h-10 w-10 rounded-full bg-primary-50 flex items-center justify-center">
                      <User className="w-4 h-4 text-primary-600" />
                    </div>
                    <div>
                      <p className="text-sm font-medium text-primary-900">
                        {selectedCustomer.customerName}
                      </p>
                      <p className="text-xs text-gray-500">
                        {selectedCustomer.customerContact}
                      </p>
                    </div>
                  </div>
                  <div className="flex flex-col items-end">
                    <p className="text-xs text-gray-500">Credits</p>
                    <p className="text-sm font-medium text-primary-600">
                      ₹{(selectedCustomer.customerCredits || 0).toFixed(2)}
                    </p>
                  </div>
                </div>
              ) : (
                <div className="flex gap-2">
                  <button
                    onClick={() => setIsCustomerModalOpen(true)}
                    className="btn-primary btn-sm flex items-center justify-center"
                  >
                    <User className="w-4 h-4 mr-2" />
                    Select Customer
                  </button>
                  <button
                    onClick={() => setIsAddCustomerModalOpen(true)}
                    className="btn-secondary btn-sm flex items-center justify-center"
                    title="Add a new customer"
                  >
                    <Plus className="w-4 h-4 mr-2" />
                    Add Customer
                  </button>
                </div>
              )}
            </div>
            {/* Bill settings card */}
            <h3 className="text-lg font-medium text-gray-900 mb-4 flex items-center">
              <Receipt className="w-5 h-5 mr-2" />
              Bill Settings
            </h3>

            <div className="space-y-4 flex-1 overflow-auto">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Payment Method
                </label>
                <div className="flex flex-wrap gap-2 ml-1">
                  <button
                    type="button"
                    onClick={() => setPaymentMethod("cash")}
                    aria-pressed={paymentMethod === "cash"}
                    className={`px-3 py-1 rounded-md text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-primary-300 ${
                      paymentMethod === "cash"
                        ? "bg-primary-600 text-white"
                        : "bg-white border border-gray-200 text-gray-700"
                    }`}
                  >
                    Cash
                  </button>

                  <button
                    type="button"
                    onClick={() => setPaymentMethod("card")}
                    aria-pressed={paymentMethod === "card"}
                    className={`px-3 py-1 rounded-md text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-primary-300 ${
                      paymentMethod === "card"
                        ? "bg-primary-600 text-white"
                        : "bg-white border border-gray-200 text-gray-700"
                    }`}
                  >
                    Card
                  </button>

                  <button
                    type="button"
                    onClick={() => setPaymentMethod("online")}
                    aria-pressed={paymentMethod === "online"}
                    className={`px-3 py-1 rounded-md text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-primary-300 ${
                      paymentMethod === "online"
                        ? "bg-primary-600 text-white"
                        : "bg-white border border-gray-200 text-gray-700"
                    }`}
                  >
                    Online
                  </button>

                  <button
                    type="button"
                    onClick={() => setPaymentMethod("credit")}
                    aria-pressed={paymentMethod === "credit"}
                    className={`px-3 py-1 rounded-md text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-primary-300 ${
                      paymentMethod === "credit"
                        ? "bg-primary-600 text-white"
                        : "bg-white border border-gray-200 text-gray-700"
                    }`}
                  >
                    Credit
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Bill Type
                </label>
                <div className="flex flex-wrap gap-2 mb-2 ml-1">
                  <button
                    type="button"
                    onClick={() => setBillType("new")}
                    aria-pressed={billType === "new"}
                    className={`px-3 py-1 rounded-md text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-primary-300 ${
                      billType === "new"
                        ? "bg-primary-600 text-white"
                        : "bg-white border border-gray-200 text-gray-700"
                    }`}
                  >
                    New
                  </button>

                  <button
                    type="button"
                    onClick={() => setBillType("return")}
                    aria-pressed={billType === "return"}
                    className={`px-3 py-1 rounded-md text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-primary-300 ${
                      billType === "return"
                        ? "bg-primary-600 text-white"
                        : "bg-white border border-gray-200 text-gray-700"
                    }`}
                  >
                    Return
                  </button>
                </div>
              </div>

              {/* Bill status is computed automatically from bill type/payment method */}
              {billType === "return" && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Original Bill Number
                  </label>
                  <div className="flex items-stretch">
                    <input
                      type="text"
                      className="input-field flex-1 rounded-r-none border-r-0 focus:ring-1 h-10 mb-4 ml-2"
                      value={originalBillNumber}
                      onChange={(e) => setOriginalBillNumber(e.target.value)}
                      placeholder="Enter original bill number"
                    />
                    <button
                      type="button"
                      className="btn-secondary btn-sm rounded-l-none h-10 px-3 whitespace-nowrap mr-2"
                      onClick={async () => {
                        if (!originalBillNumber) {
                          showError("Enter a bill number to load");
                          return;
                        }
                        try {
                          setLoading(true);
                          const resp = await billingAPI.getBillByNumber(
                            originalBillNumber
                          );
                          const bill = resp.data;
                          // populate billItems from response (map to editable billItems state)
                          const items = (bill.billItems || []).map((it) => ({
                            billItemProduct: {
                              productId: it.productId,
                              productName: it.productName,
                              // For returns, cap max quantity to the original billed quantity
                              productQuantity: it.billItemQuantity,
                            },
                            billItemQuantity: it.billItemQuantity,
                            pricePerUnit: it.billItemPricePerUnit || 0,
                            billItemDiscountPerUnit: it.discountPerUnit || 0,
                          }));
                          setBillItems(items);
                          // set a shallow selectedCustomer for display purposes
                          setSelectedCustomer({
                            customerId: null,
                            customerName: bill.customerName,
                            customerContact:
                              bill.customerPhone || bill.customerEmail || "",
                            customerCredits: 0,
                          });
                          showSuccess(
                            "Loaded original bill. Adjust items/quantities to return."
                          );
                        } catch (err) {
                          const msg =
                            err?.response?.data?.message ||
                            err?.message ||
                            "Failed to load bill";
                          showError(msg);
                        } finally {
                          setLoading(false);
                        }
                      }}
                    >
                      Load
                    </button>
                  </div>
                </div>
              )}
            </div>

            {/* Total & Create Bill */}
            <div className="space-y-1 mt-4 flex-auto ">
              <div className="flex items-center justify-between mb-4">
                <span className="text-lg font-semibold text-gray-900 flex items-center">
                  <Calculator className="w-5 h-5 mr-2" />
                  Total
                </span>
                <span className="text-2xl font-bold text-primary-700">
                  ₹{calculateTotal().toFixed(2)}
                </span>
              </div>
              <button
                className="btn-primary w-full flex items-center justify-center"
                onClick={handleCreateBill}
                disabled={loading}
              >
                {loading ? (
                  <LoadingSpinner className="w-5 h-5 mr-2" />
                ) : (
                  <Receipt className="w-5 h-5 mr-2" />
                )}
                {billType === "return" ? "Process Return" : "Create Bill"}
              </button>
            </div>
          </div>

          {/* Center: Bill Items area (span 2 columns on large screens) */}
          <div className="lg:col-span-2 space-y-6 flex flex-col h-full">
            {/* Product search + Add Product (moved from Actions) */}
            <div className="card flex items-center justify-between">
              <div className="relative w-full mr-4">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
                <input
                  type="text"
                  placeholder="Search products to add..."
                  className="input-field pl-10"
                  value={searchProduct}
                  onChange={(e) => setSearchProduct(e.target.value)}
                />
                {nameSuggestions.length > 0 && (
                  <ul className="absolute left-0 right-0 mt-1 bg-white border border-gray-200 rounded shadow z-20 max-h-48 overflow-y-auto">
                    {nameSuggestions.map((p) => (
                      <li
                        key={p.productId}
                        className="p-2 hover:bg-gray-50 cursor-pointer flex justify-between items-center"
                        onClick={() => {
                          addProductToBill(p);
                          setSearchProduct("");
                          setNameSuggestions([]);
                        }}
                      >
                        <div>
                          <div className="text-sm font-medium text-gray-900">
                            {p.productName}
                          </div>
                          <div className="text-xs text-gray-500">
                            {p.productCode}
                          </div>
                        </div>
                        <div className="text-sm text-gray-900">
                          ₹{(p.pricePerItem || p.price || 0).toFixed(2)}
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
              <div className="flex-shrink-0">
                <button
                  onClick={() => setIsProductModalOpen(true)}
                  className="btn-secondary flex items-center"
                >
                  <Plus className="w-4 h-4 mr-2" />
                  Add Product
                </button>
              </div>
            </div>

            {/* Bill Items */}
            <div className="card flex flex-col flex-1 min-h-0">
              <h3 className="text-lg font-medium text-gray-900 mb-4 flex items-center">
                <ShoppingCart className="w-5 h-5 mr-2" />
                Bill Items ({billItems.length})
              </h3>

              {billItems.length === 0 ? (
                <div className="flex-1 flex flex-col items-center justify-center gap-3 py-12 text-gray-500">
                  <Package className="w-14 h-14 text-gray-300" />
                  <p className="text-sm">No items added to bill</p>
                </div>
              ) : (
                <div className="overflow-auto flex-1 min-h-0">
                  <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider max-w-xs">
                          Product
                        </th>
                        <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-20">
                          Price
                        </th>
                        <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-24">
                          Quantity
                        </th>
                        <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-24">
                          Discount
                        </th>
                        <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-24">
                          Total
                        </th>
                        <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-12">
                          Actions
                        </th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                      {billItems.map((item) => (
                        <tr key={item.billItemProduct.productId}>
                          <td className="px-3 py-2 whitespace-nowrap max-w-xs">
                            <div>
                              <p className="text-sm font-medium text-gray-900 truncate max-w-xs">
                                {item.billItemProduct.productName}
                              </p>
                              <p className="text-sm text-gray-500 truncate max-w-xs">
                                {item.billItemProduct.productCode}
                              </p>
                            </div>
                          </td>
                          <td className="px-3 py-2 whitespace-nowrap text-sm text-gray-900">
                            ₹{item.pricePerUnit.toFixed(2)}
                          </td>
                          <td className="px-3 py-2 whitespace-nowrap">
                            <input
                              type="number"
                              min="1"
                              step="1"
                              value={
                                item.billItemQuantity === ""
                                  ? ""
                                  : item.billItemQuantity
                              }
                              onChange={(e) => {
                                const val = e.target.value;
                                updateItemQuantity(
                                  item.billItemProduct.productId,
                                  val === "" ? "" : Number(val)
                                );
                              }}
                              onBlur={(e) => {
                                let val = e.target.value;
                                if (val === "") val = "1";
                                let n = Math.floor(Number(val));
                                if (!Number.isFinite(n) || n < 1) n = 1;
                                // Only clamp to max on returns
                                if (billType === "return") {
                                  const max =
                                    item.billItemProduct.productQuantity ||
                                    item.billItemQuantity ||
                                    1;
                                  n = Math.min(n, Math.max(max, 1));
                                }
                                updateItemQuantity(
                                  item.billItemProduct.productId,
                                  n
                                );
                              }}
                              className="w-20 px-2 py-1 border border-gray-300 rounded text-sm"
                            />
                          </td>
                          <td className="px-3 py-2 whitespace-nowrap">
                            <input
                              type="number"
                              min="0"
                              step="1"
                              inputMode="numeric"
                              value={String(item.billItemDiscountPerUnit ?? 0)}
                              onChange={(e) => {
                                // Strip non-digits and leading zeros
                                const raw = e.target.value.replace(
                                  /[^0-9]/g,
                                  ""
                                );
                                const cleaned = raw.replace(/^0+(\d)/, "$1");
                                const next =
                                  cleaned === ""
                                    ? 0
                                    : Math.floor(Number(cleaned));
                                updateItemDiscount(
                                  item.billItemProduct.productId,
                                  next < 0 ? 0 : next
                                );
                              }}
                              onBlur={(e) => {
                                // Final normalize to integer without leading zeros
                                const raw = e.target.value.replace(
                                  /[^0-9]/g,
                                  ""
                                );
                                const cleaned =
                                  raw === ""
                                    ? "0"
                                    : raw.replace(/^0+(\d)/, "$1");
                                const next = Math.floor(Number(cleaned));
                                updateItemDiscount(
                                  item.billItemProduct.productId,
                                  next < 0 ? 0 : next
                                );
                              }}
                              className="w-16 px-2 py-1 border border-gray-300 rounded text-sm"
                            />
                          </td>
                          <td className="px-3 py-2 whitespace-nowrap text-sm font-medium text-gray-900">
                            ₹
                            {(
                              item.pricePerUnit *
                                (Number(item.billItemQuantity) > 0
                                  ? Number(item.billItemQuantity)
                                  : 0) -
                              item.billItemDiscountPerUnit *
                                (Number(item.billItemQuantity) > 0
                                  ? Number(item.billItemQuantity)
                                  : 0)
                            ).toFixed(2)}
                          </td>
                          <td className="px-3 py-2 whitespace-nowrap">
                            <button
                              onClick={() =>
                                removeItem(item.billItemProduct.productId)
                              }
                              className="text-red-600 hover:text-red-900"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>

          {/* Right column removed - Customer/Total/Create moved to left card */}
        </div>

        {/* Customer Selection Modal */}
        <Modal
          isOpen={isCustomerModalOpen}
          onClose={() => setIsCustomerModalOpen(false)}
          title="Select Customer"
          size="lg"
        >
          <div className="space-y-4">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
              <input
                type="text"
                placeholder="Search customers..."
                className="input-field pl-10"
                value={searchCustomer}
                onChange={(e) => setSearchCustomer(e.target.value)}
              />
            </div>

            <div className="max-h-96 overflow-y-auto">
              <ul className="divide-y divide-gray-100">
                {filteredCustomers.map((customer) => (
                  <li
                    key={customer.customerId}
                    className="flex items-center justify-between p-2 hover:bg-gray-50 cursor-pointer"
                    onClick={() => {
                      setSelectedCustomer(customer);
                      setIsCustomerModalOpen(false);
                    }}
                  >
                    <div>
                      <p className="text-sm font-medium text-gray-900">
                        {customer.customerName}
                      </p>
                      <p className="text-xs text-gray-500">
                        {customer.customerContact} • {customer.customerEmail}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="text-xs text-gray-500">Credits</p>
                      <p className="text-sm font-medium text-primary-600">
                        ₹{(customer.customerCredits || 0).toFixed(2)}
                      </p>
                    </div>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </Modal>
        {/* Add Customer Modal */}
        <Modal
          isOpen={isAddCustomerModalOpen}
          onClose={() => setIsAddCustomerModalOpen(false)}
          title="Add Customer"
          size="md"
        >
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Name
              </label>
              <input
                type="text"
                className="input-field w-full"
                value={newCustomerName}
                onChange={(e) => setNewCustomerName(e.target.value)}
                placeholder="Customer name"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Contact
              </label>
              <input
                type="text"
                className="input-field w-full"
                value={newCustomerContact}
                onChange={(e) => setNewCustomerContact(e.target.value)}
                placeholder="Phone or contact"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Email (optional)
              </label>
              <input
                type="email"
                className="input-field w-full"
                value={newCustomerEmail}
                onChange={(e) => setNewCustomerEmail(e.target.value)}
                placeholder="email@example.com"
              />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button
                className="btn-secondary"
                onClick={() => setIsAddCustomerModalOpen(false)}
              >
                Cancel
              </button>
              <button
                className="btn-primary"
                onClick={async () => {
                  if (!newCustomerName.trim() || !newCustomerContact.trim()) {
                    showError("Name and Contact are required");
                    return;
                  }
                  try {
                    setLoading(true);
                    const payload = {
                      customerName: newCustomerName.trim(),
                      customerContact: newCustomerContact.trim(),
                      customerEmail: newCustomerEmail.trim() || undefined,
                    };
                    const resp = await customerAPI.add(payload);
                    const created = resp.data;
                    showSuccess("Customer added");
                    await fetchCustomers();
                    setSelectedCustomer(created || payload);
                    setIsAddCustomerModalOpen(false);
                    setNewCustomerName("");
                    setNewCustomerContact("");
                    setNewCustomerEmail("");
                  } catch (err) {
                    const msg =
                      err?.response?.data?.message ||
                      err?.message ||
                      "Failed to add customer";
                    showError(msg);
                  } finally {
                    setLoading(false);
                  }
                }}
              >
                Save
              </button>
            </div>
          </div>
        </Modal>

        {/* Product Selection Modal */}
        <Modal
          isOpen={isProductModalOpen}
          onClose={() => setIsProductModalOpen(false)}
          title="Add Product"
          size="lg"
        >
          <div className="space-y-4">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
              <input
                type="text"
                placeholder="Search products..."
                className="input-field pl-10"
                value={searchProduct}
                onChange={(e) => setSearchProduct(e.target.value)}
              />
            </div>

            <div className="max-h-96 overflow-y-auto space-y-2">
              {filteredProducts.map((product) => (
                <div
                  key={product.productId}
                  onClick={() => addProductToBill(product)}
                  className="p-3 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer"
                >
                  <div className="flex justify-between items-start">
                    <div>
                      <p className="font-medium text-gray-900">
                        {product.productName}
                      </p>
                      <p className="text-sm text-gray-500">
                        {product.productCode}
                      </p>
                      {product.productDescription && (
                        <p className="text-sm text-gray-500">
                          {product.productDescription}
                        </p>
                      )}
                    </div>
                    <div className="text-right">
                      <p className="font-medium text-gray-900">
                        ₹{product.pricePerItem.toFixed(2)}
                      </p>
                      <p className="text-sm text-gray-500">
                        Stock: {product.productQuantity}
                      </p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
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
      {/* Return Tip Modal */}
      <Modal
        isOpen={showReturnTip}
        onClose={() => setShowReturnTip(false)}
        title="Return Tip"
        size="md"
      >
        <div className="space-y-4">
          <p className="text-sm text-gray-700">
            Only keep those items that you want to return.
          </p>
          <label className="flex items-center gap-2 text-sm text-gray-700">
            <input
              type="checkbox"
              className="rounded border-gray-300"
              checked={neverShowReturnTip}
              onChange={(e) => setNeverShowReturnTip(e.target.checked)}
            />
            Never show again
          </label>
          <div className="flex justify-end gap-2 pt-2">
            <button
              className="btn-secondary"
              onClick={() => setShowReturnTip(false)}
            >
              Close
            </button>
            <button
              className="btn-primary"
              onClick={() => {
                if (neverShowReturnTip) {
                  try {
                    localStorage.setItem("bnx_hideReturnTip", "true");
                  } catch (_) {}
                }
                setShowReturnTip(false);
              }}
            >
              Got it
            </button>
          </div>
        </div>
      </Modal>
      {/* Created / Returned Bill Modal */}
      <Modal
        isOpen={showBillModal}
        onClose={() => {
          setShowBillModal(false);
          setCreatedBill(null);
        }}
        title={createdBill ? `Bill: ${createdBill.billNumber}` : "Bill"}
        size="lg"
      >
        {createdBill ? (
          <div className="space-y-4">
            <div>
              <p className="text-sm text-gray-600">
                Customer: {createdBill.customerName} (
                {createdBill.customerPhone || createdBill.customerEmail})
              </p>
              <p className="text-sm text-gray-600">
                Bill Type: {createdBill.billType}
              </p>
              <p className="text-sm text-gray-600">
                Bill Status: {createdBill.billStatus}
              </p>
              <p className="text-sm text-gray-600">
                Total: ₹{createdBill.totalAmount}
              </p>
            </div>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                      Product
                    </th>
                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                      Qty
                    </th>
                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                      Price
                    </th>
                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                      Total
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {createdBill.billItems &&
                    createdBill.billItems.map((it, idx) => (
                      <tr key={idx}>
                        <td className="px-4 py-2 text-sm text-gray-900">
                          {it.productName || it.product?.productName}
                        </td>
                        <td className="px-4 py-2 text-sm text-gray-900">
                          {it.billItemQuantity}
                        </td>
                        <td className="px-4 py-2 text-sm text-gray-900">
                          ₹{it.billItemPricePerUnit || it.pricePerUnit}
                        </td>
                        <td className="px-4 py-2 text-sm text-gray-900">
                          ₹
                          {it.totalPrice ||
                            it.billItemPricePerUnit * it.billItemQuantity}
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>
            <div className="flex justify-end">
              <button
                className="btn-primary"
                onClick={() => {
                  setShowBillModal(false);
                  setCreatedBill(null);
                }}
              >
                Close
              </button>
            </div>
          </div>
        ) : (
          <p>No bill data</p>
        )}
      </Modal>
    </Layout>
  );
};

export default BillingPage;
