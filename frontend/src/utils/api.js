// OpenAPI generated client wrappers
import { BizAppApi } from "../apiClient";

// Create a single API client instance with dynamic JWT token resolution
const apiClient = new BizAppApi({
  BASE: "http://localhost:8081",
  TOKEN: async () => localStorage.getItem("authToken") || "",
});

// Helper: normalize errors to keep UI behavior consistent
const handleError = (err) => {
  const status = err?.status || err?.response?.status;
  if (status === 401) {
    localStorage.removeItem("authToken");
    localStorage.removeItem("userData");
    localStorage.removeItem("refreshToken");
    if (typeof window !== "undefined") window.location.href = "/login";
  }
  const message =
    err?.body?.message ||
    err?.message ||
    err?.response?.data?.message ||
    "Request failed";
  const error = new Error(message);
  // Attach original for callers if needed
  error.original = err;
  throw error;
};

// Return shape compatibility: mimic axios by returning { data }
const ok = (data) => ({ data });

// Auth API (JWT)
export const userAPI = {
  login: async (credentials) => {
    try {
      const data = await apiClient.auth.login(credentials);
      return ok(data);
    } catch (err) {
      handleError(err);
    }
  },
  // First-time bootstrap check (no auth)
  checkFirstTime: async () => {
    try {
      const data = await apiClient.request.request({
        method: "GET",
        url: "/api/v1/auth/first-time",
      });
      return ok(data);
    } catch (err) {
      // Treat protected/missing endpoints as not-first-time to avoid login page loops
      if (err?.status === 401 || err?.status === 403 || err?.status === 404) {
        return ok(false);
      }
      // Do not hard-redirect from here; surface error instead
      const error = new Error(
        err?.body?.message || err?.message || "first-time check failed"
      );
      error.original = err;
      throw error;
    }
  },
  // Bootstrap first admin (public in v1.5.x); tries primary then a fallback path
  bootstrapFirstAdmin: async (payload) => {
    try {
      const data = await apiClient.request.request({
        method: "POST",
        url: "/api/v1/auth/first-time/setup",
        body: payload,
        mediaType: "application/json",
      });
      return ok(data);
    } catch (err) {
      if (err?.status === 404) {
        // fallback path for some deployments
        const data = await apiClient.request.request({
          method: "POST",
          url: "/api/v1/auth/bootstrap-admin",
          body: payload,
          mediaType: "application/json",
        });
        return ok(data);
      }
      handleError(err);
    }
  },
  forgotPassword: async (username) => {
    try {
      const data = await apiClient.auth.forgotPassword(String(username || ""));
      return ok(data);
    } catch (err) {
      handleError(err);
    }
  },
  changeOwnPassword: async (username, newPassword) => {
    try {
      const data = await apiClient.users.updatePassword(username, {
        newPassword,
      });
      return ok(data);
    } catch (err) {
      handleError(err);
    }
  },
  // First-login forced password change endpoint (auth scope)
  changePasswordFirstLogin: async (newPassword) => {
    const variants = [
      { method: "PATCH", body: { newPassword } },
      { method: "POST", body: { newPassword } },
      { method: "PATCH", body: { password: newPassword } },
      { method: "POST", body: { password: newPassword } },
    ];
    for (const v of variants) {
      try {
        const data = await apiClient.request.request({
          method: v.method,
          url: "/api/v1/auth/first-login/password",
          body: v.body,
          mediaType: "application/json",
        });
        return ok(data);
      } catch (err) {
        if (err?.status === 404 || err?.status === 405 || err?.status === 415) {
          // try next variant
          continue;
        }
        // Other errors: rethrow
        handleError(err);
      }
    }
    // Fallback: users endpoint
    try {
      const userData = JSON.parse(localStorage.getItem("userData") || "{}");
      if (userData?.username) {
        const data = await apiClient.users.updatePassword(userData.username, {
          newPassword,
        });
        return ok(data);
      }
    } catch (_) {
      /* ignore */
    }
    throw new Error(
      "Change password at /api/v1/auth/first-login/password before accessing other endpoints."
    );
  },
  register: async (userData) => {
    try {
      const data = await apiClient.auth.register(userData);
      return ok(data);
    } catch (err) {
      handleError(err);
    }
  },
  // Admin list users (spec: GET /api/v1/users)
  list: async () => {
    try {
      const data = await apiClient.users.getAllUsers();
      return ok(data);
    } catch (err) {
      handleError(err);
    }
  },
  // Delete user by username (204 idempotent)
  delete: async (username) => {
    try {
      await apiClient.users.deleteUserByUsername(username);
      return ok(undefined);
    } catch (err) {
      handleError(err);
    }
  },
};

// Customer API (spec-aligned)
export const customerAPI = {
  // Convenience: get all as array (request a large page)
  getAll: async () => {
    try {
      const page = await apiClient.customerController.getAllCustomers(0, 1000);
      return ok(page?.content || []);
    } catch (err) {
      handleError(err);
    }
  },
  // Paged list
  getPaged: async (page = 0, size = 20, sort) => {
    try {
      const data = await apiClient.customerController.getAllCustomers(
        page,
        size,
        sort || "customerId,asc"
      );
      return ok(data);
    } catch (err) {
      handleError(err);
    }
  },
  getById: async (id) => {
    try {
      const data = await apiClient.customerController.getCustomerById(id);
      return ok(data);
    } catch (err) {
      handleError(err);
    }
  },
  getByContact: async (contact) => {
    try {
      const data = await apiClient.customerController.getCustomerByContact(
        contact
      );
      return ok(data);
    } catch (err) {
      handleError(err);
    }
  },
  add: async (customerData) => {
    try {
      const data = await apiClient.customerController.addCustomer(customerData);
      return ok(data);
    } catch (err) {
      handleError(err);
    }
  },
  update: async (_contact, customerData) => {
    try {
      const data = await apiClient.customerController.updateCustomer(
        customerData
      );
      return ok(data);
    } catch (err) {
      handleError(err);
    }
  },
  delete: async (contact) => {
    try {
      const data = await apiClient.customerController.deleteCustomerById(
        contact
      );
      return ok(data);
    } catch (err) {
      handleError(err);
    }
  },
  // Credits (paged in spec) – return array for current UI
  getWithCredits: async () => {
    try {
      const page =
        await apiClient.customerController.getCustomersWithCreditsPaged(
          0,
          1000
        );
      return ok(page?.content || []);
    } catch (err) {
      handleError(err);
    }
  },
  // Credits paged
  getCreditsPaged: async (page = 0, size = 20, sort) => {
    try {
      const data =
        await apiClient.customerController.getCustomersWithCreditsPaged(
          page,
          size,
          sort || "customerId,asc"
        );
      return ok(data);
    } catch (err) {
      handleError(err);
    }
  },
  // Search customers (paged)
  searchPaged: async (query, page = 0, size = 20, sort) => {
    try {
      const data = await apiClient.request.request({
        method: "GET",
        url: "/api/v1/customers/search",
        query: {
          query: String(query || ""),
          page,
          size,
          sort: sort || "customerId,asc",
        },
      });
      return ok(data);
    } catch (err) {
      if (err?.status === 204)
        return ok({
          content: [],
          page,
          size,
          totalPages: 0,
          totalElements: 0,
          last: true,
        });
      handleError(err);
    }
  },
  // Search customers with credits (paged)
  searchCreditsPaged: async (query, page = 0, size = 20, sort) => {
    try {
      const data = await apiClient.request.request({
        method: "GET",
        url: "/api/v1/customers/credits/search",
        query: {
          query: String(query || ""),
          page,
          size,
          sort: sort || "customerId,asc",
        },
      });
      return ok(data);
    } catch (err) {
      if (err?.status === 204)
        return ok({
          content: [],
          page,
          size,
          totalPages: 0,
          totalElements: 0,
          last: true,
        });
      handleError(err);
    }
  },
};

// Product API (spec-aligned)
export const productAPI = {
  // get all as array for UI filters
  getAll: async () => {
    try {
      const page = await apiClient.productController.getAllProducts(0, 1000);
      return ok(page?.content || []);
    } catch (err) {
      handleError(err);
    }
  },
  // Paged list
  getPaged: async (page = 0, size = 20, sort) => {
    try {
      const data = await apiClient.productController.getAllProducts(
        page,
        size,
        sort || "productId,asc"
      );
      return ok(data);
    } catch (err) {
      handleError(err);
    }
  },
  getById: async (id) => {
    try {
      const data = await apiClient.productController.getProductById(id);
      return ok(data);
    } catch (err) {
      handleError(err);
    }
  },
  // Search by name (paged in spec) – return array of items
  searchByName: async (productName) => {
    try {
      const page = await apiClient.productController.searchProductsByNamePaged(
        String(productName || ""),
        0,
        20
      );
      return ok(page?.content || []);
    } catch (err) {
      // 204 No Content -> treat as empty list
      if (err?.status === 204) return ok([]);
      handleError(err);
    }
  },
  // Search with pagination (full page)
  searchByNamePaged: async (productName, page = 0, size = 20, sort) => {
    try {
      const data = await apiClient.productController.searchProductsByNamePaged(
        String(productName || ""),
        page,
        size,
        sort || "productId,asc"
      );
      return ok(data);
    } catch (err) {
      if (err?.status === 204)
        return ok({
          content: [],
          page,
          size,
          totalPages: 0,
          totalElements: 0,
          last: true,
        });
      handleError(err);
    }
  },
  getByCategory: async (category) => {
    try {
      // Legacy helper returning flattened array (kept for backward compat)
      const page = await apiClient.productController.getProductByCategoryPaged(
        String(category || ""),
        0,
        1000
      );
      return ok(page?.content || []);
    } catch (err) {
      handleError(err);
    }
  },
  // Paged by category (preferred)
  getByCategoryPaged: async (category, page = 0, size = 20, sort) => {
    try {
      const data = await apiClient.productController.getProductByCategoryPaged(
        String(category || ""),
        page,
        size,
        sort
      );
      return ok(data);
    } catch (err) {
      handleError(err);
    }
  },
  add: async (productData) => {
    try {
      const data = await apiClient.productController.addProduct(productData);
      return ok(data);
    } catch (err) {
      handleError(err);
    }
  },
  update: async (id, productData) => {
    try {
      const data = await apiClient.productController.updateProduct(
        id,
        productData
      );
      return ok(data);
    } catch (err) {
      handleError(err);
    }
  },
  delete: async (id) => {
    try {
      const data = await apiClient.productController.deleteProduct(id);
      return ok(data);
    } catch (err) {
      handleError(err);
    }
  },
};

// Billing API (spec-aligned)
export const billingAPI = {
  createBill: async (billData) => {
    try {
      const data = await apiClient.billingController.createBill(billData);
      return ok(data);
    } catch (err) {
      handleError(err);
    }
  },
  getBillByNumber: async (billNumber) => {
    try {
      const data = await apiClient.billingController.getBillByBillNumber(
        billNumber
      );
      return ok(data);
    } catch (err) {
      handleError(err);
    }
  },
  returnBill: async (billDto) => {
    try {
      const data = await apiClient.billingController.updateBill(billDto);
      return ok(data);
    } catch (err) {
      handleError(err);
    }
  },
  creditBill: async (billData) => {
    try {
      const data = await apiClient.billingController.creditBill(billData);
      return ok(data);
    } catch (err) {
      handleError(err);
    }
  },
  // Convenience: get all as array for dashboards
  getAll: async () => {
    try {
      const page = await apiClient.billingController.getAllBills(0, 100);
      return ok(page?.content || []);
    } catch (err) {
      handleError(err);
    }
  },
  // Paged results for history screen
  getPaged: async (page = 0, size = 20, sort) => {
    try {
      const data = await apiClient.billingController.getAllBills(
        page,
        size,
        sort || "billId,asc"
      );
      return ok(data);
    } catch (err) {
      handleError(err);
    }
  },
  // Search bills (paged)
  searchPaged: async (query, page = 0, size = 20, sort = "billDate,desc") => {
    try {
      const data = await apiClient.request.request({
        method: "GET",
        url: "/api/v1/billing/search",
        query: {
          query: String(query || ""),
          page,
          size,
          sort,
        },
      });
      return ok(data);
    } catch (err) {
      if (err?.status === 204)
        return ok({
          content: [],
          page,
          size,
          totalPages: 0,
          totalElements: 0,
          last: true,
        });
      handleError(err);
    }
  },
  delete: async (id) => {
    try {
      const data = await apiClient.billingController.deleteBill(id);
      return ok(data);
    } catch (err) {
      handleError(err);
    }
  },
};

export default apiClient;
