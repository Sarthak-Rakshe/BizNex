import React, { useEffect, useState } from "react";
import Layout from "../components/Layout/Layout";
import LoadingSpinner from "../components/UI/LoadingSpinner";
import Modal from "../components/UI/Modal";
import Toast from "../components/UI/Toast";
import { useToast } from "../hooks/useToast";
import { userAPI } from "../utils/api";
import { useAuth } from "../context/AuthContext";
import { UserPlus, Users, Trash2 } from "lucide-react";

const AdminUsersPage = () => {
  const { isAdmin } = useAuth();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({
    username: "",
    userEmail: "",
    userPassword: "",
    userRole: "USER",
    userContact: "",
    userSalary: 0,
  });
  const { toasts, showSuccess, showError, removeToast } = useToast();

  const fetchUsers = async () => {
    try {
      // Assuming backend exposes an admin-only list endpoint
      const resp = await userAPI.list?.();
      setUsers(resp?.data || []);
    } catch (err) {
      // optional; keep page working even if list not available
      setUsers([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const onChange = (e) => {
    const { name, value } = e.target;
    setFormData((p) => ({
      ...p,
      [name]: name === "userSalary" ? parseFloat(value) || 0 : value,
    }));
  };

  const onCreate = async (e) => {
    e.preventDefault();
    try {
      if (!isAdmin) {
        showError("Not authorized");
        return;
      }
      setLoading(true);
      const resp = await userAPI.register(formData);
      const data = resp.data;
      if (!data || !data.username) throw new Error("Create user failed");
      showSuccess("User created");
      setIsModalOpen(false);
      setFormData({
        username: "",
        userEmail: "",
        userPassword: "",
        userRole: "USER",
        userContact: "",
        userSalary: 0,
      });
      fetchUsers();
    } catch (err) {
      const msg =
        err?.response?.data?.message || err?.message || "Create user failed";
      showError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold text-gray-900 flex items-center">
            <Users className="w-5 h-5 mr-2" />
            Users
          </h1>
          <button
            className="btn-primary flex items-center"
            onClick={() => setIsModalOpen(true)}
            disabled={!isAdmin}
            title={isAdmin ? "Create user" : "Admin only"}
          >
            <UserPlus className="w-4 h-4 mr-2" /> New User
          </button>
        </div>

        {loading ? (
          <div className="flex justify-center items-center h-64">
            <LoadingSpinner size="lg" />
          </div>
        ) : (
          <div className="card overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                    Username
                  </th>
                  <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                    Role
                  </th>
                  <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                    Email
                  </th>
                  <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                    Contact
                  </th>
                  <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                    Salary
                  </th>
                  <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {users.length === 0 ? (
                  <tr>
                    <td
                      className="px-3 py-4 text-center text-gray-500"
                      colSpan={6}
                    >
                      No users
                    </td>
                  </tr>
                ) : (
                  users.map((u) => (
                    <tr key={u.username}>
                      <td className="px-3 py-2 text-sm">{u.username}</td>
                      <td className="px-3 py-2 text-sm">{u.userRole}</td>
                      <td className="px-3 py-2 text-sm">{u.userEmail}</td>
                      <td className="px-3 py-2 text-sm">{u.userContact}</td>
                      <td className="px-3 py-2 text-sm">{u.userSalary}</td>
                      <td className="px-3 py-2 text-sm">
                        <button
                          className="text-red-600 hover:text-red-800 inline-flex items-center disabled:opacity-50"
                          disabled={!isAdmin}
                          onClick={async () => {
                            if (!isAdmin) {
                              showError("Not authorized");
                              return;
                            }
                            if (!window.confirm(`Delete user ${u.username}?`))
                              return;
                            try {
                              setLoading(true);
                              await userAPI.delete(u.username);
                              showSuccess("User deleted");
                              await fetchUsers();
                            } catch (err) {
                              const msg =
                                err?.message || "Failed to delete user";
                              showError(msg);
                            } finally {
                              setLoading(false);
                            }
                          }}
                        >
                          <Trash2 className="w-4 h-4 mr-1" />
                          Delete
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}

        <Modal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          title="Create User"
          size="md"
        >
          <form className="space-y-4" onSubmit={onCreate}>
            <div>
              <label className="block text-sm font-medium">Username</label>
              <input
                className="input-field"
                required
                name="username"
                value={formData.username}
                onChange={onChange}
              />
            </div>
            <div>
              <label className="block text-sm font-medium">Email</label>
              <input
                className="input-field"
                type="email"
                required
                name="userEmail"
                value={formData.userEmail}
                onChange={onChange}
              />
            </div>
            <div>
              <label className="block text-sm font-medium">Password</label>
              <input
                className="input-field"
                type="password"
                required
                name="userPassword"
                value={formData.userPassword}
                onChange={onChange}
              />
            </div>
            <div>
              <label className="block text-sm font-medium">Role</label>
              <select
                className="input-field"
                name="userRole"
                value={formData.userRole}
                onChange={onChange}
              >
                <option value="USER">User</option>
                <option value="ADMIN">Admin</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium">Contact</label>
              <input
                className="input-field"
                name="userContact"
                value={formData.userContact}
                onChange={onChange}
              />
            </div>
            <div>
              <label className="block text-sm font-medium">Salary</label>
              <input
                className="input-field"
                type="number"
                min="0"
                step="0.01"
                name="userSalary"
                value={formData.userSalary}
                onChange={onChange}
              />
            </div>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                className="btn-secondary"
                onClick={() => setIsModalOpen(false)}
              >
                Cancel
              </button>
              <button type="submit" className="btn-primary" disabled={loading}>
                {loading ? <LoadingSpinner size="sm" /> : "Create"}
              </button>
            </div>
          </form>
        </Modal>
      </div>
      {toasts.map((t) => (
        <Toast
          key={t.id}
          message={t.message}
          type={t.type}
          duration={t.duration}
          onClose={() => removeToast(t.id)}
        />
      ))}
    </Layout>
  );
};

export default AdminUsersPage;
