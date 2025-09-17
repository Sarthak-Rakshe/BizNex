import React from "react";
import {
  BrowserRouter as Router,
  Routes,
  Route,
  Navigate,
} from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";

// Pages
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import DashboardPage from "./pages/DashboardPage";
import CustomersPage from "./pages/CustomersPage";
import ProductsPage from "./pages/ProductsPage";
import BillingPage from "./pages/BillingPage";
import CreditsPage from "./pages/CreditsPage";
import BillHistoryPage from "./pages/BillHistoryPage";
import AdminUsersPage from "./pages/AdminUsersPage";
import FirstTimeSetupPage from "./pages/FirstTimeSetupPage";
import ForcePasswordChangePage from "./pages/ForcePasswordChangePage";

function App() {
  return (
    <AuthProvider>
      <Router>
        <div className="App">
          {/* ...existing code... */}
          <Routes>
            {/* Public Routes */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/first-time" element={<FirstTimeSetupPage />} />
            <Route
              path="/force-password"
              element={<ForcePasswordChangePage />}
            />
            <Route
              path="/register"
              element={
                <ProtectedRoute requireRole="ADMIN">
                  <RegisterPage />
                </ProtectedRoute>
              }
            />

            {/* Protected Routes */}
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <DashboardPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/customers"
              element={
                <ProtectedRoute>
                  <CustomersPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/products"
              element={
                <ProtectedRoute>
                  <ProductsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/billing"
              element={
                <ProtectedRoute>
                  <BillingPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/credits"
              element={
                <ProtectedRoute>
                  <CreditsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/bill-history"
              element={
                <ProtectedRoute>
                  <BillHistoryPage />
                </ProtectedRoute>
              }
            />

            {/* Admin */}
            <Route
              path="/admin/users"
              element={
                <ProtectedRoute requireRole="ADMIN">
                  <AdminUsersPage />
                </ProtectedRoute>
              }
            />

            {/* Default redirect */}
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            {/* Catch-all to guard against unknown paths */}
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </div>
      </Router>
    </AuthProvider>
  );
}

export default App;
