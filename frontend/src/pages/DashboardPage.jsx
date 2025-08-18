import React, { useState, useEffect } from "react";
import Layout from "../components/Layout/Layout";
import { useAuth } from "../context/AuthContext";
import { customerAPI, productAPI, billingAPI } from "../utils/api";
import {
  Users,
  Package,
  Receipt,
  CreditCard,
  TrendingUp,
  AlertCircle,
} from "lucide-react";
import LoadingSpinner from "../components/UI/LoadingSpinner";

const DashboardPage = () => {
  const { user, isAdmin } = useAuth();
  const [stats, setStats] = useState({
    totalCustomers: 0,
    totalProducts: 0,
    customersWithCredits: 0,
    lowStockProducts: 0,
  });
  const [loading, setLoading] = useState(true);
  const [recentBills, setRecentBills] = useState([]);
  const [productCategories, setProductCategories] = useState([]);
  const [revenueSeries, setRevenueSeries] = useState([]);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const [customersRes, productsRes, creditsRes, billsRes] =
          await Promise.all([
            customerAPI.getAll(),
            productAPI.getAll(),
            customerAPI.getWithCredits(),
            billingAPI.getAll(),
          ]);

        const products = productsRes.data || [];
        const lowStock = products.filter((p) => p.productQuantity < 10).length;

        // compute category counts
        const catMap = {};
        products.forEach((product) => {
          const key =
            product.productCategory ||
            product.category ||
            product.productType ||
            "Uncategorized";
          catMap[key] = (catMap[key] || 0) + 1;
        });
        const palette = [
          "#ef4444",
          "#f59e0b",
          "#10b981",
          "#3b82f6",
          "#8b5cf6",
          "#ec4899",
          "#06b6d4",
        ];
        const categories = Object.entries(catMap).map(([name, count], i) => ({
          name,
          value: count,
          color: palette[i % palette.length],
        }));
        setProductCategories(categories);

        // compute revenue over last 7 days from bills
        const bills = billsRes.data || [];
        const days = 7;
        const seriesMap = {};
        const labels = [];
        for (let i = days - 1; i >= 0; i--) {
          const d = new Date();
          d.setDate(d.getDate() - i);
          const key = d.toISOString().slice(0, 10);
          seriesMap[key] = 0;
          labels.push(key);
        }
        bills.forEach((b) => {
          const date = new Date(
            b.billDate ||
              b.createdAt ||
              b.billCreatedAt ||
              b.createdDate ||
              b.date
          );
          if (isNaN(date)) return;
          const key = date.toISOString().slice(0, 10);
          if (seriesMap.hasOwnProperty(key)) {
            const amt = Number(
              b.totalAmount || b.billTotalAmount || b.billTotal || 0
            );
            seriesMap[key] += isNaN(amt) ? 0 : amt;
          }
        });
        const revenue = labels.map((lbl) => ({
          date: lbl,
          amount: seriesMap[lbl] || 0,
        }));
        setRevenueSeries(revenue);

        setStats({
          totalCustomers: customersRes.data.length,
          totalProducts: products.length,
          customersWithCredits: creditsRes.data?.length || 0,
          lowStockProducts: lowStock,
        });

        // recent bills (latest 6)
        const latest = bills.slice().reverse().slice(0, 6);
        setRecentBills(latest);
      } catch (error) {
        console.error("Error fetching dashboard stats:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchStats();
  }, []);

  const statCards = [
    {
      title: "Total Customers",
      value: stats.totalCustomers,
      icon: Users,
      color: "bg-blue-500",
      bgColor: "bg-blue-50",
      textColor: "text-blue-600",
    },
    {
      title: "Total Products",
      value: stats.totalProducts,
      icon: Package,
      color: "bg-green-500",
      bgColor: "bg-green-50",
      textColor: "text-green-600",
    },
    {
      title: "Customers with Credits",
      value: stats.customersWithCredits,
      icon: CreditCard,
      color: "bg-purple-500",
      bgColor: "bg-purple-50",
      textColor: "text-purple-600",
    },
    {
      title: "Low Stock Products",
      value: stats.lowStockProducts,
      icon: AlertCircle,
      color: "bg-red-500",
      bgColor: "bg-red-50",
      textColor: "text-red-600",
    },
  ];

  // Small SVG Pie chart component (no deps)
  const PieChart = ({ data = [], size = 140 }) => {
    const cx = size / 2;
    const cy = size / 2;
    const r = size / 2 - 4;
    const total = Math.max(
      data.reduce((s, d) => s + Math.max(d.value, 0), 0),
      1
    );
    let acc = 0;

    const polarToCartesian = (cx, cy, r, angleDeg) => {
      const angleRad = ((angleDeg - 90) * Math.PI) / 180.0;
      return { x: cx + r * Math.cos(angleRad), y: cy + r * Math.sin(angleRad) };
    };

    const describeArc = (startPct, endPct) => {
      const startAngle = (startPct / total) * 360;
      const endAngle = (endPct / total) * 360;
      const start = polarToCartesian(cx, cy, r, endAngle);
      const end = polarToCartesian(cx, cy, r, startAngle);
      const largeArcFlag = endAngle - startAngle <= 180 ? "0" : "1";
      return `M ${cx} ${cy} L ${start.x} ${start.y} A ${r} ${r} 0 ${largeArcFlag} 0 ${end.x} ${end.y} Z`;
    };

    return (
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
        {data.map((d, i) => {
          const start = acc;
          acc += Math.max(d.value, 0);
          const path = describeArc(start, acc);
          return (
            <path
              key={i}
              d={path}
              fill={d.color}
              stroke="white"
              strokeWidth="1"
            />
          );
        })}
      </svg>
    );
  };

  const Legend = ({ data = [] }) => (
    <div className="grid grid-cols-2 lg:grid-cols-3 gap-x-6 gap-y-2 text-sm">
      {data.map((d, i) => (
        <div key={i} className="flex items-center">
          <span
            className="inline-block w-3 h-3 rounded-sm mr-2"
            style={{ backgroundColor: d.color }}
          />
          <span className="text-gray-800 truncate mr-2">{d.name}</span>
          <span className="text-gray-500 tabular-nums">{d.value}</span>
        </div>
      ))}
    </div>
  );

  // Simple horizontal Bar chart for small stats
  const BarChart = ({ labels = [], values = [], colors = [] }) => {
    const max = Math.max(...values.map((v) => v || 0), 1);
    return (
      <div className="space-y-3">
        {values.map((v, idx) => {
          const pct = Math.round(((v || 0) / max) * 100);
          return (
            <div key={idx}>
              <div className="flex items-center justify-between text-xs text-gray-600 mb-1">
                <span>{labels[idx]}</span>
                <span className="font-medium">{v}</span>
              </div>
              <div className="w-full bg-gray-100 rounded-full h-2">
                <div
                  className={`${
                    colors[idx] || "bg-primary-600"
                  } h-2 rounded-full`}
                  style={{ width: `${pct}%` }}
                />
              </div>
            </div>
          );
        })}
      </div>
    );
  };

  // Line chart with axes & labels
  const LineChart = ({
    data = [],
    width = 520,
    height = 200,
    margin = { top: 10, right: 18, bottom: 34, left: 64 },
  }) => {
    if (!data || data.length === 0)
      return <div className="text-sm">No data</div>;
    const innerW = width - margin.left - margin.right;
    const innerH = height - margin.top - margin.bottom;
    const max = Math.max(...data.map((d) => d.amount || 0), 1);
    const xStep = innerW / Math.max(data.length - 1, 1);
    const fmtCompact = (n) => {
      const abs = Math.abs(n);
      if (abs >= 1_000_000) return `₹${(n / 1_000_000).toFixed(1)}M`;
      if (abs >= 1_000) return `₹${(n / 1_000).toFixed(0)}k`;
      return `₹${Math.round(n)}`;
    };
    const points = data
      .map((d, i) => {
        const x = margin.left + i * xStep;
        const y = margin.top + innerH - ((d.amount || 0) / max) * innerH;
        return `${x},${y}`;
      })
      .join(" ");
    const areaPoints = `${margin.left},${margin.top + innerH} ${points} ${
      margin.left + innerW
    },${margin.top + innerH}`;
    const yTicks = 4;
    return (
      <svg width={width} height={height}>
        {/* Grid */}
        {Array.from({ length: yTicks + 1 }).map((_, i) => {
          const y = margin.top + (innerH / yTicks) * i;
          return (
            <line
              key={`g-${i}`}
              x1={margin.left}
              y1={y}
              x2={margin.left + innerW}
              y2={y}
              stroke="#f3f4f6"
            />
          );
        })}
        {/* Axes */}
        <line
          x1={margin.left}
          y1={margin.top}
          x2={margin.left}
          y2={margin.top + innerH}
          stroke="#e5e7eb"
        />
        <line
          x1={margin.left}
          y1={margin.top + innerH}
          x2={margin.left + innerW}
          y2={margin.top + innerH}
          stroke="#e5e7eb"
        />
        {/* Y ticks */}
        {Array.from({ length: yTicks + 1 }).map((_, i) => {
          const y = margin.top + (innerH / yTicks) * i;
          const val = max - (max / yTicks) * i;
          return (
            <g key={i}>
              <line
                x1={margin.left - 4}
                y1={y}
                x2={margin.left}
                y2={y}
                stroke="#d1d5db"
              />
              <text
                x={margin.left - 8}
                y={y + 4}
                fontSize="10"
                textAnchor="end"
                fill="#6b7280"
              >
                {fmtCompact(val)}
              </text>
            </g>
          );
        })}
        {/* X labels */}
        {data.map((d, i) => {
          const x = margin.left + i * xStep;
          const lbl = d.date?.slice(5); // MM-DD
          return (
            <text
              key={i}
              x={x}
              y={margin.top + innerH + 16}
              fontSize="10"
              textAnchor="middle"
              fill="#6b7280"
            >
              {lbl}
            </text>
          );
        })}
        {/* Data (use currentColor so parent text color controls theme) */}
        <polyline
          fill="none"
          stroke="currentColor"
          strokeWidth={2}
          points={points}
        />
        {data.map((d, i) => {
          const x = margin.left + i * xStep;
          const y = margin.top + innerH - ((d.amount || 0) / max) * innerH;
          return (
            <circle key={`pt-${i}`} cx={x} cy={y} r={2.5} fill="currentColor" />
          );
        })}
        <polygon fill="currentColor" fillOpacity="0.18" points={areaPoints} />
        {/* Axis labels */}
        <text
          x={margin.left + innerW / 2}
          y={height - 6}
          textAnchor="middle"
          fontSize="11"
          fill="#6b7280"
        >
          Days
        </text>
        <text
          x={12}
          y={margin.top + innerH / 2}
          transform={`rotate(-90 12 ${margin.top + innerH / 2})`}
          textAnchor="middle"
          fontSize="11"
          fill="#6b7280"
        >
          Revenue (₹)
        </text>
      </svg>
    );
  };

  if (loading) {
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
        {/* Welcome Section */}
        <div className="bg-gradient-to-r from-primary-600 to-primary-700 rounded-lg shadow-sm p-6 text-white">
          <h1 className="text-3xl font-bold mb-2">
            Welcome back, {user?.username}!
          </h1>
          <p className="text-primary-100">
            Here's what's happening with your business today.
          </p>
        </div>

        {/* Stats Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {statCards.map((stat, index) => {
            const Icon = stat.icon;
            return (
              <div key={index} className="card">
                <div className="flex items-center">
                  <div className={`p-3 rounded-lg ${stat.bgColor}`}>
                    <Icon className={`w-6 h-6 ${stat.textColor}`} />
                  </div>
                  <div className="ml-4">
                    <p className="text-sm font-medium text-gray-600">
                      {stat.title}
                    </p>
                    <p className="text-2xl font-semibold text-gray-900">
                      {stat.value}
                    </p>
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {/* Charts (ADMIN only) */}
        {isAdmin && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="card">
              <h3 className="text-lg font-medium text-gray-900 mb-4">
                Products by Category
              </h3>
              <div className="flex items-start gap-6">
                <PieChart size={160} data={productCategories} />
                <div className="flex-1">
                  {/* Bars removed as requested; legend only for clarity */}
                  <div className="hidden md:block">
                    <div className="grid grid-cols-2 lg:grid-cols-3 gap-x-6 gap-y-2 text-sm">
                      {productCategories.map((c, i) => (
                        <div key={i} className="flex items-center">
                          <span
                            className="inline-block w-3 h-3 rounded-sm mr-2"
                            style={{ backgroundColor: c.color }}
                          />
                          <span className="text-gray-800 mr-2">{c.name}</span>
                          <span className="text-gray-500">{c.value}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                  <div className="md:hidden mt-2">
                    <Legend data={productCategories} />
                  </div>
                </div>
              </div>
            </div>

            <div className="card">
              <h3 className="text-lg font-medium text-gray-900 mb-4">
                Revenue (last 7 days)
              </h3>
              <div>
                <div className="text-primary-600">
                  <LineChart data={revenueSeries} width={540} height={210} />
                </div>
                <div className="mt-2 text-xs text-gray-500">
                  Total: ₹
                  {revenueSeries
                    .reduce((s, r) => s + (r.amount || 0), 0)
                    .toFixed(2)}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Quick Actions (Admin only) */}
        {isAdmin && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <div className="card">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-lg font-medium text-gray-900">
                    Quick Billing
                  </h3>
                  <p className="text-sm text-gray-500">Create a new bill</p>
                </div>
                <Receipt className="w-8 h-8 text-primary-600" />
              </div>
              <div className="mt-4">
                <a href="/billing" className="btn-primary w-full text-center">
                  Start Billing
                </a>
              </div>
            </div>

            <div className="card">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-lg font-medium text-gray-900">
                    Manage Inventory
                  </h3>
                  <p className="text-sm text-gray-500">
                    Add or update products
                  </p>
                </div>
                <Package className="w-8 h-8 text-primary-600" />
              </div>
              <div className="mt-4">
                <a href="/products" className="btn-primary w-full text-center">
                  Manage Products
                </a>
              </div>
            </div>

            <div className="card">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-lg font-medium text-gray-900">
                    Customer Credits
                  </h3>
                  <p className="text-sm text-gray-500">
                    View and manage credits
                  </p>
                </div>
                <CreditCard className="w-8 h-8 text-primary-600" />
              </div>
              <div className="mt-4">
                <a href="/credits" className="btn-primary w-full text-center">
                  View Credits
                </a>
              </div>
            </div>
          </div>
        )}

        {/* Recent Transactions */}
        <div className="card">
          <h3 className="text-lg font-medium text-gray-900 mb-4">
            Recent Transactions
          </h3>
          {recentBills.length === 0 ? (
            <div className="text-center py-8 text-gray-500">
              <TrendingUp className="w-12 h-12 mx-auto mb-4 text-gray-300" />
              <p>No recent transactions</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                      Bill #
                    </th>
                    <th className="px-4 py-2 text-center text-xs font-medium text-gray-500 uppercase">
                      Customer
                    </th>
                    <th className="px-4 py-2 text-center text-xs font-medium text-gray-500 uppercase">
                      Amount
                    </th>
                    <th className="px-4 py-2 text-center text-xs font-medium text-gray-500 uppercase">
                      Status
                    </th>
                    <th className="px-4 py-2 text-center text-xs font-medium text-gray-500 uppercase">
                      Type
                    </th>
                    <th className="px-4 py-2 text-center text-xs font-medium text-gray-500 uppercase">
                      Date
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {recentBills.map((b) => (
                    <tr key={b.billNumber} className="hover:bg-gray-50">
                      <td className="px-4 py-2 text-sm text-left text-gray-900">
                        {b.billNumber}
                      </td>
                      <td className="px-4 py-2 text-sm text-center text-gray-700">
                        {b.customerName || b.customer?.customerName}
                      </td>
                      <td className="px-4 py-2 text-sm text-center text-gray-900">
                        ₹{b.totalAmount}
                      </td>
                      <td className="px-4 py-2 text-sm text-center text-gray-700">
                        {b.billStatus}
                      </td>
                      <td className="px-4 py-2 text-sm text-center text-gray-700">
                        {b.billType}
                      </td>
                      <td className="px-4 py-2 text-sm text-gray-500 text-center">
                        {b.billDate || b.billDate}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </Layout>
  );
};

export default DashboardPage;
