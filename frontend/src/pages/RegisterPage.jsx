import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { userAPI } from "../utils/api";
import { useToast } from "../hooks/useToast";
import LoadingSpinner from "../components/UI/LoadingSpinner";
import Toast from "../components/UI/Toast";
import { UserPlus } from "lucide-react";

const RegisterPage = () => {
  const [formData, setFormData] = useState({
    username: "",
    userEmail: "",
    userPassword: "",
    userRole: "USER",
    userContact: "",
    userSalary: 0,
  });
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { toasts, showSuccess, showError, removeToast } = useToast();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({
      ...formData,
      [name]: name === "userSalary" ? parseFloat(value) || 0 : value,
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const response = await userAPI.register(formData);
      const data = response.data;
      if (!data || !data.username) {
        throw new Error("Registration failed");
      }
      showSuccess("Registration successful! Please login.");
      navigate("/login");
    } catch (error) {
      const msg =
        error?.response?.data?.message ||
        error?.message ||
        "Registration failed. Please try again.";
      showError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div>
          <div className="mx-auto h-12 w-12 flex items-center justify-center rounded-full bg-primary-100">
            <UserPlus className="h-6 w-6 text-primary-600" />
          </div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
            Create your account
          </h2>
          <p className="mt-2 text-center text-sm text-gray-600">
            Or{" "}
            <Link
              to="/login"
              className="font-medium text-primary-600 hover:text-primary-500"
            >
              sign in to your existing account
            </Link>
          </p>
        </div>
        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          <div className="space-y-4">
            <div>
              <label
                htmlFor="username"
                className="block text-sm font-medium text-gray-700"
              >
                Username
              </label>
              <input
                id="username"
                name="username"
                type="text"
                required
                className="input-field"
                value={formData.username}
                onChange={handleChange}
              />
            </div>

            <div>
              <label
                htmlFor="userEmail"
                className="block text-sm font-medium text-gray-700"
              >
                Email
              </label>
              <input
                id="userEmail"
                name="userEmail"
                type="email"
                required
                className="input-field"
                value={formData.userEmail}
                onChange={handleChange}
              />
            </div>

            <div>
              <label
                htmlFor="userPassword"
                className="block text-sm font-medium text-gray-700"
              >
                Password
              </label>
              <input
                id="userPassword"
                name="userPassword"
                type="password"
                required
                className="input-field"
                value={formData.userPassword}
                onChange={handleChange}
              />
            </div>

            <div>
              <label
                htmlFor="userContact"
                className="block text-sm font-medium text-gray-700"
              >
                Contact Number
              </label>
              <input
                id="userContact"
                name="userContact"
                type="tel"
                pattern="[0-9]{10}"
                required
                className="input-field"
                placeholder="10-digit phone number"
                value={formData.userContact}
                onChange={handleChange}
              />
            </div>

            <div>
              <label
                htmlFor="userRole"
                className="block text-sm font-medium text-gray-700"
              >
                Role
              </label>
              <select
                id="userRole"
                name="userRole"
                className="input-field"
                value={formData.userRole}
                onChange={handleChange}
              >
                <option value="USER">User</option>
                <option value="ADMIN">Admin</option>
              </select>
            </div>

            <div>
              <label
                htmlFor="userSalary"
                className="block text-sm font-medium text-gray-700"
              >
                Salary
              </label>
              <input
                id="userSalary"
                name="userSalary"
                type="number"
                min="0"
                step="0.01"
                className="input-field"
                value={formData.userSalary}
                onChange={handleChange}
              />
            </div>
          </div>

          <div>
            <button
              type="submit"
              disabled={loading}
              className="btn-primary w-full flex justify-center items-center"
            >
              {loading ? (
                <LoadingSpinner size="sm" />
              ) : (
                <>
                  <UserPlus className="w-4 h-4 mr-2" />
                  Create Account
                </>
              )}
            </button>
          </div>
        </form>
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
    </div>
  );
};

export default RegisterPage;
