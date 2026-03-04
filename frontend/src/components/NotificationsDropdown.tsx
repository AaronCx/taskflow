import { useEffect, useRef, useState } from 'react';
import { Notification, notificationsApi } from '../api/notifications';
import { formatDistanceToNow } from 'date-fns';

/**
 * Notification bell icon with:
 *   - Unread count badge (polled every 30 s)
 *   - Click-to-open dropdown showing the last 20 notifications
 *   - Auto-marks all as read when the dropdown is opened
 */
export function NotificationsDropdown() {
  const [open,          setOpen]          = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unreadCount,   setUnreadCount]   = useState(0);
  const [loading,       setLoading]       = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // ── Poll unread count every 30 s ─────────────────────────────────
  useEffect(() => {
    const fetchCount = () => {
      notificationsApi.getUnreadCount()
        .then((res) => setUnreadCount(res.data.count))
        .catch(() => { /* silently ignore — notifications are non-critical */ });
    };

    fetchCount();
    const interval = setInterval(fetchCount, 30_000);
    return () => clearInterval(interval);
  }, []);

  // ── Close dropdown on outside click ──────────────────────────────
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  // ── Open dropdown: fetch list + mark all read ─────────────────────
  const handleOpen = async () => {
    const wasOpen = open;
    setOpen(!wasOpen);

    if (!wasOpen) {
      setLoading(true);
      try {
        const [notifRes] = await Promise.all([
          notificationsApi.getAll(),
          unreadCount > 0 ? notificationsApi.markAllRead() : Promise.resolve(null),
        ]);
        setNotifications(notifRes.data);
        setUnreadCount(0);
      } catch {
        // Non-critical — fail silently
      } finally {
        setLoading(false);
      }
    }
  };

  return (
    <div className="relative" ref={dropdownRef}>
      {/* ── Bell button ──────────────────────────────────────────── */}
      <button
        onClick={handleOpen}
        aria-label={`Notifications${unreadCount > 0 ? ` (${unreadCount} unread)` : ''}`}
        className="relative p-2 rounded-lg text-gray-500 hover:text-gray-800
                   hover:bg-gray-100 transition-colors"
      >
        {/* Bell icon */}
        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path strokeLinecap="round" strokeLinejoin="round"
                d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6 6 0 10-12 0v3.159c0
                   .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
        </svg>

        {/* Unread badge */}
        {unreadCount > 0 && (
          <span className="absolute -top-0.5 -right-0.5 flex h-4 w-4 items-center justify-center
                           rounded-full bg-red-500 text-[10px] font-bold text-white leading-none">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {/* ── Dropdown panel ───────────────────────────────────────── */}
      {open && (
        <div className="absolute right-0 mt-2 w-80 bg-white rounded-xl shadow-lg border
                        border-gray-200 z-50 overflow-hidden">
          {/* Header */}
          <div className="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
            <h3 className="text-sm font-semibold text-gray-900">Notifications</h3>
            {notifications.length > 0 && (
              <span className="text-xs text-gray-400">{notifications.length} recent</span>
            )}
          </div>

          {/* Body */}
          <div className="max-h-96 overflow-y-auto divide-y divide-gray-50">
            {loading && (
              <div className="flex justify-center py-8">
                <div className="w-5 h-5 border-2 border-blue-500 border-t-transparent
                                rounded-full animate-spin" />
              </div>
            )}

            {!loading && notifications.length === 0 && (
              <div className="py-10 text-center">
                <span className="text-3xl">🔔</span>
                <p className="mt-2 text-sm text-gray-500">No notifications yet</p>
                <p className="text-xs text-gray-400 mt-0.5">
                  Events appear when tasks change
                </p>
              </div>
            )}

            {!loading && notifications.map((n) => (
              <NotificationItem key={n.id} notification={n} />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

// ── Single notification row ───────────────────────────────────────────────────

function NotificationItem({ notification: n }: { notification: Notification }) {
  const icon = {
    TASK_CREATED: '✅',
    TASK_UPDATED: '🔄',
    TASK_DELETED: '🗑️',
  }[n.eventType] ?? '🔔';

  const timeAgo = n.createdAt
    ? formatDistanceToNow(new Date(n.createdAt), { addSuffix: true })
    : '';

  return (
    <div className={`px-4 py-3 hover:bg-gray-50 transition-colors ${
      !n.read ? 'bg-blue-50/50' : ''
    }`}>
      <div className="flex gap-3 items-start">
        <span className="text-lg mt-0.5 shrink-0">{icon}</span>
        <div className="flex-1 min-w-0">
          <p className="text-xs text-gray-800 leading-relaxed">{n.message}</p>
          <p className="text-[11px] text-gray-400 mt-1">{timeAgo}</p>
        </div>
        {!n.read && (
          <span className="w-2 h-2 rounded-full bg-blue-500 shrink-0 mt-1.5" />
        )}
      </div>
    </div>
  );
}
