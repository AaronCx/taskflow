import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { NotificationsDropdown } from './NotificationsDropdown';

/**
 * App shell — top nav bar shown on authenticated pages.
 */
export function Layout({ children }: { children: React.ReactNode }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { isDark, toggle } = useTheme();
  const [menuOpen, setMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-900 transition-colors">
      {/* ── Navigation bar ───────────────────────────────────────────── */}
      <nav className="bg-white dark:bg-slate-900/80 backdrop-blur-lg border-b border-gray-200 dark:border-slate-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-14 sm:h-16">
            {/* Logo / brand */}
            <Link to="/dashboard" className="flex items-center gap-2 shrink-0">
              <span className="text-2xl">✅</span>
              <span className="text-xl font-bold text-blue-400">TaskFlow</span>
            </Link>

            {/* Desktop right side */}
            <div className="hidden sm:flex items-center gap-3">
              {user && (
                <Link to="/profile" className="text-sm text-gray-600 dark:text-slate-400 hover:text-blue-600 dark:hover:text-white transition-colors">
                  {user.firstName} {user.lastName}
                </Link>
              )}
              <button
                onClick={toggle}
                aria-label="Toggle dark mode"
                className="p-2 rounded-lg text-gray-500 dark:text-slate-400 hover:text-gray-800 dark:hover:text-white
                           hover:bg-gray-100 dark:hover:bg-slate-800 transition-colors"
              >
                {isDark ? (
                  <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round"
                          d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
                  </svg>
                ) : (
                  <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round"
                          d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
                  </svg>
                )}
              </button>
              <NotificationsDropdown />
              <Link
                to="/tasks/new"
                className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium
                           hover:bg-blue-500 transition-colors"
              >
                + New Task
              </Link>
              <button
                onClick={handleLogout}
                className="text-sm text-gray-500 dark:text-slate-500 hover:text-gray-800 dark:hover:text-white transition-colors"
              >
                Log out
              </button>
            </div>

            {/* Mobile right side — compact icons + hamburger */}
            <div className="flex sm:hidden items-center gap-1">
              <NotificationsDropdown />
              <Link
                to="/tasks/new"
                className="p-2 rounded-lg text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-slate-800 transition-colors"
                aria-label="New task"
              >
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v16m8-8H4" />
                </svg>
              </Link>
              <button
                onClick={() => setMenuOpen(!menuOpen)}
                aria-label="Toggle menu"
                className="p-2 rounded-lg text-gray-500 dark:text-slate-400 hover:bg-gray-100 dark:hover:bg-slate-800 transition-colors"
              >
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  {menuOpen ? (
                    <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                  ) : (
                    <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16" />
                  )}
                </svg>
              </button>
            </div>
          </div>
        </div>

        {/* Mobile dropdown menu */}
        {menuOpen && (
          <div className="sm:hidden border-t border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-800 px-4 py-3 space-y-2">
            {user && (
              <Link
                to="/profile"
                onClick={() => setMenuOpen(false)}
                className="block px-3 py-2 rounded-lg text-sm font-medium text-gray-700 dark:text-gray-200
                           hover:bg-gray-100 dark:hover:bg-slate-700"
              >
                {user.firstName} {user.lastName}
              </Link>
            )}
            <button
              onClick={() => { toggle(); setMenuOpen(false); }}
              className="w-full text-left px-3 py-2 rounded-lg text-sm text-gray-600 dark:text-slate-300
                         hover:bg-gray-100 dark:hover:bg-slate-700"
            >
              {isDark ? 'Light mode' : 'Dark mode'}
            </button>
            <button
              onClick={() => { handleLogout(); setMenuOpen(false); }}
              className="w-full text-left px-3 py-2 rounded-lg text-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20"
            >
              Log out
            </button>
          </div>
        )}
      </nav>

      {/* ── Page content ─────────────────────────────────────────────── */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
        {children}
      </main>
    </div>
  );
}
