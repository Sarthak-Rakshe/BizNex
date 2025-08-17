import React, { useEffect, useMemo, useState } from "react";
import Layout from "../components/Layout/Layout";
import { billingAPI } from "../utils/api";
import LoadingSpinner from "../components/UI/LoadingSpinner";
import Modal from "../components/UI/Modal";
import { Eye } from "lucide-react";

const FieldCell = ({ value }) => {
  if (value === null || value === undefined)
    return <span className="text-gray-400">-</span>;
  const type = typeof value;
  if (type === "string" || type === "number" || type === "boolean") {
    return <span className="break-words">{String(value)}</span>;
  }
  try {
    const str = JSON.stringify(value);
    return <span className="text-gray-700 break-words">{str}</span>;
  } catch (e) {
    return <span className="text-gray-700 break-words">[object]</span>;
  }
};

export default function BillHistoryPage() {
  const [bills, setBills] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [query, setQuery] = useState("");
  const [isViewOpen, setIsViewOpen] = useState(false);
  const [selectedBill, setSelectedBill] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [allBills, setAllBills] = useState([]); // legacy fallback (kept minimal)
  const [serverPaged, setServerPaged] = useState(true);

  const fetchBills = async () => {
    setLoading(true);
    setError("");
    try {
      // Use server-side search when query present, otherwise normal paged list.
      const res = query
        ? await billingAPI.searchPaged(query, page, size, "billDate,desc")
        : await billingAPI.getPaged(page, size, "billDate,desc");
      const content = res?.data?.content || [];
      setServerPaged(true);
      setBills(content);
      setTotalPages(res?.data?.totalPages || 1);
      setTotalElements(res?.data?.totalElements || content.length);
    } catch (err) {
      console.error("Failed to fetch bills", err);
      setError(
        err?.response?.data?.message || err?.message || "Failed to fetch bills"
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBills();
  }, [page, size, query]);

  // No client-side pagination needed when serverPaged

  const rows = useMemo(() => bills, [bills]);

  // View counts (derived for client-side pagination)
  const totalPagesView = useMemo(
    () => Math.max(totalPages || 0, 1),
    [totalPages]
  );

  const totalElementsView = useMemo(() => totalElements || 0, [totalElements]);

  // Reset to first page on query change
  useEffect(() => {
    setPage(0);
  }, [query]);

  const columns = useMemo(() => {
    const preferredOrder = [
      "billNumber",
      "billDate",
      "customerName",
      "customerPhone",
      "billType",
      "paymentMethod",
      "totalAmount",
      "totalDiscount",
      "billStatus",
      "originalBillNumber",
    ];
    const keys = new Set();
    (rows || []).forEach((b) =>
      Object.keys(b || {}).forEach((k) => keys.add(k))
    );
    const all = Array.from(keys);
    const preferred = preferredOrder.filter((k) => keys.has(k));
    // exclude heavy/verbose fields from list view
    const blacklist = new Set(["billItems", "billitems", "customerEmail"]);
    const filtered = [
      ...preferred,
      ...all.filter((k) => !preferred.includes(k)),
    ].filter((k) => !blacklist.has(k));
    return filtered;
  }, [rows]);

  const openView = async (bill) => {
    setSelectedBill(bill);
    setIsViewOpen(true);
    setDetailError("");
    if (bill?.billNumber) {
      try {
        setDetailLoading(true);
        const res = await billingAPI.getBillByNumber(bill.billNumber);
        const data = res?.data || bill;
        setSelectedBill(data);
      } catch (err) {
        setDetailError(
          err?.response?.data?.message ||
            err?.message ||
            "Failed to load bill details"
        );
      } finally {
        setDetailLoading(false);
      }
    }
  };

  const closeView = () => {
    setIsViewOpen(false);
    setSelectedBill(null);
    setDetailLoading(false);
    setDetailError("");
  };

  const formatDateTime = (val) => {
    if (!val) return "-";
    try {
      const d = new Date(val);
      if (isNaN(d.getTime())) return String(val);
      const pad = (n) => String(n).padStart(2, "0");
      const hh = pad(d.getHours());
      const mm = pad(d.getMinutes());
      const ss = pad(d.getSeconds());
      const dd = pad(d.getDate());
      const MM = pad(d.getMonth() + 1);
      const yy = pad(d.getFullYear() % 100);
      return `${hh}:${mm}:${ss}-${dd}-${MM}-${yy}`;
    } catch {
      return String(val);
    }
  };

  const getBillItems = (bill) => {
    const raw = bill?.billItems || bill?.billitems || [];
    if (!Array.isArray(raw)) return [];
    return raw.map((it, idx) => ({
      key: it.billItemId || it.id || idx,
      productName:
        it.productName || it.product?.productName || it.product?.name || "-",
      productCode:
        it.productCode || it.product?.productCode || it.product?.code,
      quantity: it.billItemQuantity ?? it.quantity ?? it.qty ?? 0,
      pricePerUnit:
        it.billItemPricePerUnit ??
        it.pricePerUnit ??
        it.unitPrice ??
        it.price ??
        0,
      discountPerUnit:
        it.discountPerUnit ?? it.billItemDiscountPerUnit ?? it.discount ?? 0,
      totalPrice:
        it.totalPrice ??
        it.lineTotal ??
        (it.billItemQuantity ?? it.quantity ?? 0) *
          (it.billItemPricePerUnit ?? it.pricePerUnit ?? it.unitPrice ?? 0) -
          (it.discountPerUnit ?? it.billItemDiscountPerUnit ?? 0),
    }));
  };

  return (
    <Layout>
      <div className="p-0">
        <div className="mb-4 flex flex-col md:flex-row md:items-center md:justify-between gap-3">
          <h1 className="text-2xl font-semibold">Bill History</h1>
          <div className="flex items-center gap-2 w-full md:w-auto">
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search bills (matches any field)"
              className="w-full md:w-72 h-10 px-3 rounded border border-gray-300 focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            <button
              onClick={fetchBills}
              className="h-10 px-3 rounded bg-primary-600 text-white hover:bg-primary-700 text-sm"
            >
              Refresh
            </button>
          </div>
        </div>

        {error && (
          <div className="mb-3 text-sm text-red-600 bg-red-50 border border-red-200 rounded px-3 py-2">
            {error}
          </div>
        )}

        {loading ? (
          <div className="py-16">
            <LoadingSpinner />
          </div>
        ) : (
          <div className="overflow-auto border border-gray-200 rounded bg-white">
            <table className="min-w-full text-sm">
              <thead className="bg-gray-50">
                <tr>
                  {columns.map((col) => (
                    <th
                      key={col}
                      className="text-left font-medium text-gray-700 px-3 py-2 border-b "
                    >
                      {col}
                    </th>
                  ))}
                  <th className="text-left font-medium text-gray-700 px-3 py-2 border-b">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody>
                {rows.length === 0 ? (
                  <tr>
                    <td
                      colSpan={columns.length + 1}
                      className="px-3 py-6 text-center text-gray-500"
                    >
                      No bills found.
                    </td>
                  </tr>
                ) : (
                  rows.map((b, i) => (
                    <tr
                      key={b.billNumber || b.billId || i}
                      className="odd:bg-white even:bg-gray-50"
                    >
                      {columns.map((col) => (
                        <td
                          key={col}
                          className="align-top px-3 py-2 border-b max-w-[28rem] "
                        >
                          {col === "billDate" ||
                          col === "createdAt" ||
                          col === "date" ? (
                            <span>{formatDateTime(b?.[col])}</span>
                          ) : (
                            <FieldCell value={b?.[col]} />
                          )}
                        </td>
                      ))}
                      <td className="align-top px-3 py-2 border-b whitespace-nowrap">
                        <button
                          onClick={() => openView(b)}
                          className="inline-flex items-center gap-2 px-3 py-1.5 rounded border border-gray-300 text-gray-700 hover:bg-gray-50"
                          title="View bill"
                        >
                          <Eye className="w-4 h-4" />
                          View
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
            {/* Pagination controls */}
            <div className="flex flex-col sm:flex-row items-center justify-between gap-3 px-3 py-2">
              <div className="text-sm text-gray-600">
                Page {page + 1} of {totalPagesView}
                {totalElementsView ? ` • ${totalElementsView} total` : ""}
              </div>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page <= 0}
                  className="px-3 py-1.5 rounded border border-gray-300 text-gray-700 disabled:opacity-50"
                >
                  Prev
                </button>
                <button
                  onClick={() =>
                    setPage((p) =>
                      totalPagesView
                        ? Math.min(totalPagesView - 1, p + 1)
                        : p + 1
                    )
                  }
                  disabled={page >= totalPagesView - 1}
                  className="px-3 py-1.5 rounded border border-gray-300 text-gray-700 disabled:opacity-50"
                >
                  Next
                </button>
                <select
                  value={size}
                  onChange={(e) => {
                    setPage(0);
                    setSize(parseInt(e.target.value, 10));
                  }}
                  className="h-9 px-2 rounded border border-gray-300"
                >
                  <option value={10}>10 / page</option>
                  <option value={20}>20 / page</option>
                  <option value={50}>50 / page</option>
                  <option value={100}>100 / page</option>
                </select>
              </div>
            </div>
          </div>
        )}
      </div>
      {/* View Bill Modal */}
      <Modal
        isOpen={isViewOpen}
        onClose={closeView}
        title={`Bill ${selectedBill?.billNumber || "Details"}`}
        size="xl"
      >
        {detailError && (
          <div className="mb-3 text-sm text-red-600 bg-red-50 border border-red-200 rounded px-3 py-2">
            {detailError}
          </div>
        )}
        {detailLoading ? (
          <div className="py-8">
            <LoadingSpinner />
          </div>
        ) : (
          <div className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="bg-gray-50 rounded p-3">
                <h3 className="font-medium text-gray-800 mb-2">Bill</h3>
                <dl className="text-sm text-gray-700 grid grid-cols-2 gap-2">
                  <dt className="font-medium">Number</dt>
                  <dd>{selectedBill?.billNumber || "-"}</dd>
                  <dt className="font-medium">Date</dt>
                  <dd>{formatDateTime(selectedBill?.billDate)}</dd>
                  <dt className="font-medium">Type</dt>
                  <dd>{selectedBill?.billType || "-"}</dd>
                  <dt className="font-medium">Payment</dt>
                  <dd>{selectedBill?.paymentMethod || "-"}</dd>
                  <dt className="font-medium">Status</dt>
                  <dd>{selectedBill?.billStatus || "-"}</dd>
                  {selectedBill?.originalBillNumber && (
                    <>
                      <dt className="font-medium">Original Bill</dt>
                      <dd>{selectedBill.originalBillNumber}</dd>
                    </>
                  )}
                </dl>
              </div>
              <div className="bg-gray-50 rounded p-3">
                <h3 className="font-medium text-gray-800 mb-2">Customer</h3>
                <dl className="text-sm text-gray-700 grid grid-cols-2 gap-2">
                  <dt className="font-medium">Name</dt>
                  <dd>
                    {selectedBill?.customerName ||
                      selectedBill?.customer?.customerName ||
                      "-"}
                  </dd>
                  <dt className="font-medium">Phone</dt>
                  <dd>
                    {selectedBill?.customerPhone ||
                      selectedBill?.customer?.customerContact ||
                      "-"}
                  </dd>
                </dl>
              </div>
            </div>
            <div className="bg-white rounded border">
              <div className="px-3 py-2 border-b font-medium text-gray-800">
                Items
              </div>
              <div className="overflow-auto">
                <table className="min-w-full text-sm">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="text-left px-3 py-2 border-b">#</th>
                      <th className="text-left px-3 py-2 border-b">Product</th>
                      <th className="text-left px-3 py-2 border-b">Code</th>
                      <th className="text-right px-3 py-2 border-b">Qty</th>
                      <th className="text-right px-3 py-2 border-b">
                        Price/Unit
                      </th>
                      <th className="text-right px-3 py-2 border-b">
                        Discount/Unit
                      </th>
                      <th className="text-right px-3 py-2 border-b">Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {getBillItems(selectedBill).length === 0 ? (
                      <tr>
                        <td
                          colSpan={7}
                          className="px-3 py-4 text-center text-gray-500"
                        >
                          No items
                        </td>
                      </tr>
                    ) : (
                      getBillItems(selectedBill).map((it, idx) => (
                        <tr
                          key={it.key}
                          className="odd:bg-white even:bg-gray-50"
                        >
                          <td className="px-3 py-2 border-b">{idx + 1}</td>
                          <td className="px-3 py-2 border-b">
                            {it.productName}
                          </td>
                          <td className="px-3 py-2 border-b">
                            {it.productCode || "-"}
                          </td>
                          <td className="px-3 py-2 border-b text-right">
                            {it.quantity}
                          </td>
                          <td className="px-3 py-2 border-b text-right">
                            {Number(it.pricePerUnit).toFixed(2)}
                          </td>
                          <td className="px-3 py-2 border-b text-right">
                            {Number(it.discountPerUnit || 0).toFixed(2)}
                          </td>
                          <td className="px-3 py-2 border-b text-right">
                            {Number(it.totalPrice).toFixed(2)}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
              <div className="px-3 py-3 flex flex-col items-end gap-1">
                <div className="text-sm text-gray-700">
                  Total Discount:{" "}
                  <span className="font-medium">
                    {Number(selectedBill?.totalDiscount || 0).toFixed(2)}
                  </span>
                </div>
                <div className="text-base text-gray-900">
                  Grand Total:{" "}
                  <span className="font-semibold">
                    {Number(
                      selectedBill?.totalAmount ||
                        selectedBill?.billTotalAmount ||
                        0
                    ).toFixed(2)}
                  </span>
                </div>
              </div>
            </div>
          </div>
        )}
      </Modal>
    </Layout>
  );
}
