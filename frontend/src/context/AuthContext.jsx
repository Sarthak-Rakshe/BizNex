import React, { createContext, useContext, useState, useEffect } from "react";

const AuthContext = createContext();

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem("authToken");
    const userData = localStorage.getItem("userData");
    const expireAt = (() => {
      try {
        return JSON.parse(userData || "{}").expireAt;
      } catch {
        return undefined;
      }
    })();

    if (token && userData) {
      const parsed = JSON.parse(userData);
      setUser(parsed);
      // optional auto-logout on expiry
      if (expireAt && Date.now() > expireAt) {
        localStorage.removeItem("authToken");
        localStorage.removeItem("userData");
        localStorage.removeItem("refreshToken");
        setUser(null);
      }
    }
    setLoading(false);
  }, []);

  const login = (userData, token) => {
    localStorage.setItem("authToken", token);
    localStorage.setItem("userData", JSON.stringify(userData));
    setUser(userData);
  };

  const logout = () => {
    localStorage.removeItem("authToken");
    localStorage.removeItem("userData");
    localStorage.removeItem("refreshToken");
    setUser(null);
  };

  const isExpired = () => {
    const exp = user?.expireAt;
    return exp ? Date.now() > exp : false;
  };

  const userRole = user?.userRole;
  const isAdmin = userRole === "ADMIN";
  const hasRole = (role) => userRole === role;

  const value = {
    user,
    login,
    logout,
    loading,
    isAuthenticated: !!user,
    isExpired,
    userRole,
    isAdmin,
    hasRole,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
