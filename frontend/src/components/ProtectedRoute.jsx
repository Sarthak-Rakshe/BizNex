import React from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import LoadingSpinner from "./UI/LoadingSpinner";

const ProtectedRoute = ({ children, requireRole }) => {
  const { isAuthenticated, loading, isExpired, hasRole } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (!isAuthenticated || isExpired()) {
    return <Navigate to="/login" replace />;
  }

  if (requireRole && !hasRole(requireRole)) {
    // 403 equivalent UX
    return (
      <div className="min-h-screen flex items-center justify-center text-red-600">
        Not authorized
      </div>
    );
  }

  return children;
};

export default ProtectedRoute;
