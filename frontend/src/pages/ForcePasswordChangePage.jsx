import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { userAPI } from "../utils/api";

export default function ForcePasswordChangePage() {
  const [pwd, setPwd] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const submit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      // Prefer dedicated first-login endpoint; will fallback internally if needed
      await userAPI.changePasswordFirstLogin(pwd);
      // After password change, force re-login for a clean session
      logout();
      navigate("/login", { replace: true });
    } catch (e2) {
      const hint =
        e2?.message ||
        "Change password at /api/v1/auth/first-login/password before accessing other endpoints.";
      setError(hint);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 p-4">
      <form
        onSubmit={submit}
        className="bg-white rounded-lg shadow-sm border p-6 w-full max-w-md space-y-4"
      >
        <h1 className="text-xl font-semibold">Update Password</h1>
        <p className="text-sm text-gray-600">
          You must set a new password to continue.
        </p>
        {error && (
          <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded p-2">
            {error}
          </div>
        )}
        <div>
          <label className="block text-sm mb-1">New Password</label>
          <input
            className="input-field"
            type="password"
            value={pwd}
            onChange={(e) => setPwd(e.target.value)}
            minLength={8}
            required
          />
        </div>
        <button type="submit" className="btn-primary w-full" disabled={loading}>
          {loading ? "Updating..." : "Update Password"}
        </button>
      </form>
    </div>
  );
}
