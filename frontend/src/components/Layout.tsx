import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { NotificationsDropdown } from './NotificationsDropdown';

/**
 * App shell — top nav bar shown on authenticated pages.
 */
export function Layout({ children }: { children: React.ReactNode }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* ── Navigation bar ───────────────────────────────────────────── */}
      <nav className="bg-white border-b border-gray-200 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            {/* Logo / brand */}
            <Link to="/dashboard" className="flex items-center gap-2">
              <span className="text-2xl">✅</span>
              <span className="text-xl font-bold text-blue-600">TaskFlow</span>
            </Link>

            {/* Right side */}
            <div className="flex items-center gap-3">
              {user && (
                <span className="text-sm text-gray-600 hidden sm:block">
                  {user.firstName} {user.lastName}
                </span>
              )}
              {/* Notification bell — polls the notifications service */}
              <NotificationsDropdown />
              <Link
                to="/tasks/new"
                className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium
                           hover:bg-blue-700 transition-colors"
              >
                + New Task
              </Link>
              <button
                onClick={handleLogout}
                className="text-sm text-gray-500 hover:text-gray-800 transition-colors"
              >
                Log out
              </button>
            </div>
          </div>
        </div>
      </nav>

      {/* ── Page content ─────────────────────────────────────────────── */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {children}
      </main>
    </div>
  );
}
