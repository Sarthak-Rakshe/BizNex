import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { userAPI } from "../utils/api";
import LoadingSpinner from "../components/UI/LoadingSpinner";

export default function FirstTimeSetupPage() {
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState({
    username: "admin",
    userEmail: "",
    userPassword: "",
  });
  const [error, setError] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    (async () => {
      try {
        const { data } = await userAPI.checkFirstTime();
        if (!data) {
          navigate("/login", { replace: true });
        }
      } catch (_) {}
      setLoading(false);
    })();
  }, [navigate]);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    try {
      await userAPI.bootstrapFirstAdmin({
        username: form.username,
        userEmail: form.userEmail,
        userPassword: form.userPassword,
        userRole: "ADMIN",
      });
      navigate("/login", { replace: true });
    } catch (e2) {
      setError(e2?.message || "Setup failed");
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 p-4">
      <form
        onSubmit={submit}
        className="bg-white rounded-lg shadow-sm border p-6 w-full max-w-md space-y-4"
      >
        <h1 className="text-xl font-semibold">First-time Setup</h1>
        <p className="text-sm text-gray-600">Create the first admin account</p>
        {error && (
          <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded p-2">
            {error}
          </div>
        )}
        <div>
          <label className="block text-sm mb-1">Username</label>
          <input
            className="input-field"
            value={form.username}
            onChange={(e) => setForm({ ...form, username: e.target.value })}
            required
          />
        </div>
        <div>
          <label className="block text-sm mb-1">Email</label>
          <input
            className="input-field"
            type="email"
            value={form.userEmail}
            onChange={(e) => setForm({ ...form, userEmail: e.target.value })}
            required
          />
        </div>
        <div>
          <label className="block text-sm mb-1">Password</label>
          <input
            className="input-field"
            type="password"
            value={form.userPassword}
            onChange={(e) => setForm({ ...form, userPassword: e.target.value })}
            required
          />
        </div>
        <button type="submit" className="btn-primary w-full">
          Create Admin
        </button>
      </form>
    </div>
  );
}
