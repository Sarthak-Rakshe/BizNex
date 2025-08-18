import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { userAPI } from "../utils/api";
import { useToast } from "../hooks/useToast";
import LoadingSpinner from "../components/UI/LoadingSpinner";
import Toast from "../components/UI/Toast";
import { LogIn } from "lucide-react";

const LoginPage = () => {
  const [formData, setFormData] = useState({
    username: "",
    userPassword: "",
  });
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();
  const { toasts, showSuccess, showError, removeToast } = useToast();
  const [forgotLoading, setForgotLoading] = useState(false);

  // If backend says it's first-time (no users), redirect to setup flow
  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const { data } = await userAPI.checkFirstTime();
        if (!mounted) return;
        if (data === true) {
          navigate("/first-time", { replace: true });
        }
      } catch (_) {}
    })();
    return () => {
      mounted = false;
    };
  }, [navigate]);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const response = await userAPI.login(formData);
      const data = response.data || {};
      const token = data.accessToken;
      const refreshToken = data.refreshToken;
      const userData = {
        username: data.username || formData.username,
        userRole: data.userRole || "USER",
        expireAt: data.expireAt,
      };

      if (!token) {
        throw new Error("No access token returned");
      }

      // Detect first-time setup hint (optional): if backend indicates no users, send to /first-time
      if (data?.firstTime === true) {
        navigate("/first-time", { replace: true });
        setLoading(false);
        return;
      }

      login(userData, token);
      if (refreshToken) localStorage.setItem("refreshToken", refreshToken);
      showSuccess("Login successful!");
      if (data?.mustChangePassword) {
        // Persist flag to user state for ProtectedRoute as well
        const enriched = { ...userData, mustChangePassword: true };
        login(enriched, token);
        navigate("/force-password", { replace: true });
      } else {
        navigate("/dashboard");
      }
    } catch (error) {
      const msg =
        error?.response?.data?.message ||
        error?.message ||
        "Login failed. Please try again.";
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
            <LogIn className="h-6 w-6 text-primary-600" />
          </div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
            Sign in to BizApp
          </h2>
          <p className="mt-2 text-center text-sm text-gray-600">
            Please enter your credentials to continue.
          </p>
        </div>
        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          <div className="rounded-md shadow-sm -space-y-px">
            <div>
              <label htmlFor="username" className="sr-only">
                Username
              </label>
              <input
                id="username"
                name="username"
                type="text"
                required
                className="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-t-md focus:outline-none focus:ring-primary-500 focus:border-primary-500 focus:z-10 sm:text-sm"
                placeholder="Username"
                value={formData.username}
                onChange={handleChange}
              />
            </div>
            <div>
              <label htmlFor="userPassword" className="sr-only">
                Password
              </label>
              <input
                id="userPassword"
                name="userPassword"
                type="password"
                required
                className="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-b-md focus:outline-none focus:ring-primary-500 focus:border-primary-500 focus:z-10 sm:text-sm"
                placeholder="Password"
                value={formData.userPassword}
                onChange={handleChange}
              />
            </div>
          </div>

          <div>
            <button
              type="submit"
              disabled={loading}
              className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? (
                <LoadingSpinner size="sm" />
              ) : (
                <>
                  <LogIn className="w-4 h-4 mr-2" />
                  Sign in
                </>
              )}
            </button>
          </div>
        </form>
        <div className="text-center">
          <button
            type="button"
            disabled={forgotLoading || !formData.username}
            className="text-sm text-primary-600 hover:text-primary-700 disabled:opacity-50"
            onClick={async () => {
              try {
                setForgotLoading(true);
                const { data } = await userAPI.forgotPassword(
                  formData.username
                );
                showSuccess(
                  data || "If the user exists, reset instructions were sent."
                );
              } catch (e) {
                showError(e?.message || "Failed to start reset process");
              } finally {
                setForgotLoading(false);
              }
            }}
          >
            {forgotLoading ? "Sending..." : "Forgot password?"}
          </button>
        </div>
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

export default LoginPage;
