import { useEffect, useState, FormEvent } from 'react';
import { AxiosError } from 'axios';
import { profileApi } from '../api/profile';
import { Layout } from '../components/Layout';
import { useAuth } from '../context/AuthContext';
import { ApiError } from '../types';

export function Profile() {
  const { user } = useAuth();

  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
  });
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });

  const [profileSaving,  setProfileSaving]  = useState(false);
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [profileMsg,     setProfileMsg]     = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [passwordMsg,    setPasswordMsg]    = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    profileApi.get()
      .then((res) => {
        setForm({
          firstName: res.data.firstName,
          lastName: res.data.lastName,
          email: res.data.email,
        });
      })
      .catch(() => {
        if (user) {
          setForm({
            firstName: user.firstName,
            lastName: user.lastName,
            email: user.email,
          });
        }
      });
  }, [user]);

  const handleProfileSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setProfileMsg(null);
    setProfileSaving(true);
    try {
      await profileApi.update(form);
      setProfileMsg({ type: 'success', text: 'Profile updated successfully.' });
    } catch (err) {
      const axiosErr = err as AxiosError<ApiError>;
      setProfileMsg({
        type: 'error',
        text: axiosErr.response?.data?.message ?? 'Failed to update profile.',
      });
    } finally {
      setProfileSaving(false);
    }
  };

  const handlePasswordSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setPasswordMsg(null);

    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setPasswordMsg({ type: 'error', text: 'New passwords do not match.' });
      return;
    }

    setPasswordSaving(true);
    try {
      await profileApi.changePassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      });
      setPasswordMsg({ type: 'success', text: 'Password changed successfully.' });
      setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      const axiosErr = err as AxiosError<ApiError>;
      setPasswordMsg({
        type: 'error',
        text: axiosErr.response?.data?.message ?? 'Failed to change password.',
      });
    } finally {
      setPasswordSaving(false);
    }
  };

  const inputClass = "w-full px-3 py-2 bg-white dark:bg-slate-900/50 border border-gray-300 dark:border-slate-600 rounded-lg text-sm text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 placeholder:text-slate-500";

  return (
    <Layout>
      <div className="max-w-2xl mx-auto space-y-8">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Profile Settings</h1>

        {/* ── Profile Info ──────────────────────────────────────── */}
        <div className="bg-white dark:bg-slate-800/50 rounded-2xl border border-gray-200 dark:border-slate-700 shadow-sm p-8">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Personal Information</h2>

          {profileMsg && (
            <div className={`mb-4 p-3 rounded-lg text-sm ${
              profileMsg.type === 'success'
                ? 'bg-green-50 dark:bg-green-900/30 text-green-700 dark:text-green-300 border border-green-200 dark:border-green-800'
                : 'bg-red-50 dark:bg-red-900/30 text-red-700 dark:text-red-300 border border-red-200 dark:border-red-800'
            }`}>
              {profileMsg.text}
            </div>
          )}

          <form onSubmit={handleProfileSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-1">First name</label>
                <input
                  type="text"
                  required
                  value={form.firstName}
                  onChange={(e) => setForm((p) => ({ ...p, firstName: e.target.value }))}
                  className={inputClass}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-1">Last name</label>
                <input
                  type="text"
                  required
                  value={form.lastName}
                  onChange={(e) => setForm((p) => ({ ...p, lastName: e.target.value }))}
                  className={inputClass}
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-1">Email address</label>
              <input
                type="email"
                required
                value={form.email}
                onChange={(e) => setForm((p) => ({ ...p, email: e.target.value }))}
                className={inputClass}
              />
            </div>

            <button
              type="submit"
              disabled={profileSaving}
              className="py-2.5 px-6 bg-blue-600 text-white rounded-lg text-sm font-semibold
                         hover:bg-blue-500 transition-colors disabled:opacity-60"
            >
              {profileSaving ? 'Saving...' : 'Save changes'}
            </button>
          </form>
        </div>

        {/* ── Change Password ──────────────────────────────────── */}
        <div className="bg-white dark:bg-slate-800/50 rounded-2xl border border-gray-200 dark:border-slate-700 shadow-sm p-8">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Change Password</h2>

          {passwordMsg && (
            <div className={`mb-4 p-3 rounded-lg text-sm ${
              passwordMsg.type === 'success'
                ? 'bg-green-50 dark:bg-green-900/30 text-green-700 dark:text-green-300 border border-green-200 dark:border-green-800'
                : 'bg-red-50 dark:bg-red-900/30 text-red-700 dark:text-red-300 border border-red-200 dark:border-red-800'
            }`}>
              {passwordMsg.text}
            </div>
          )}

          <form onSubmit={handlePasswordSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-1">Current password</label>
              <input
                type="password"
                required
                value={passwordForm.currentPassword}
                onChange={(e) => setPasswordForm((p) => ({ ...p, currentPassword: e.target.value }))}
                className={inputClass}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-1">New password</label>
                <input
                  type="password"
                  required
                  minLength={8}
                  value={passwordForm.newPassword}
                  onChange={(e) => setPasswordForm((p) => ({ ...p, newPassword: e.target.value }))}
                  className={inputClass}
                  placeholder="Min. 8 characters"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-1">Confirm new password</label>
                <input
                  type="password"
                  required
                  minLength={8}
                  value={passwordForm.confirmPassword}
                  onChange={(e) => setPasswordForm((p) => ({ ...p, confirmPassword: e.target.value }))}
                  className={inputClass}
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={passwordSaving}
              className="py-2.5 px-6 bg-gray-800 dark:bg-slate-700 text-white rounded-lg text-sm font-semibold
                         hover:bg-gray-900 dark:hover:bg-slate-600 transition-colors disabled:opacity-60"
            >
              {passwordSaving ? 'Changing...' : 'Change password'}
            </button>
          </form>
        </div>
      </div>
    </Layout>
  );
}
