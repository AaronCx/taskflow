import { useEffect, useRef, useState, useCallback } from 'react';
import { Notification, notificationsApi } from '../api/notifications';
import { getAccessToken } from '../api/axiosClient';
import { formatDistanceToNow } from 'date-fns';

/**
 * Notification bell icon with:
 *   - Real-time push via SSE (with polling fallback)
 *   - Unread count badge
 *   - Click-to-open dropdown showing the last 20 notifications
 *   - Auto-marks all as read when the dropdown is opened
 */
export function NotificationsDropdown() {
  const [open,          setOpen]          = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unreadCount,   setUnreadCount]   = useState(0);
  const [loading,       setLoading]       = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const sseRef = useRef<EventSource | null>(null);

  // ── Fetch unread count ──────────────────────────────────────────
  const fetchUnreadCount = useCallback(() => {
    notificationsApi.getUnreadCount()
      .then((res) => setUnreadCount(res.data.count))
      .catch(() => { /* silently ignore */ });
  }, []);

  // ── Connect to SSE stream ──────────────────────────────────────
  useEffect(() => {
    const baseUrl = import.meta.env.VITE_NOTIFICATIONS_URL || '/api';
    const token = getAccessToken();

    // Try SSE connection
    const connectSse = () => {
      if (!token) return null;

      const url = `${baseUrl}/notifications/stream?token=${encodeURIComponent(token)}`;
      const eventSource = new EventSource(url);

      eventSource.addEventListener('notification', (event) => {
        try {
          const notification: Notification = JSON.parse(event.data);
          setNotifications((prev) => [notification, ...prev].slice(0, 20));
          setUnreadCount((prev) => prev + 1);
        } catch {
          // ignore parse errors
        }
      });

      eventSource.onerror = () => {
        eventSource.close();
        sseRef.current = null;
        // Fall back to polling
      };

      return eventSource;
    };

    sseRef.current = connectSse();

    // Polling fallback: poll every 30s regardless (SSE may not be available in prod)
    fetchUnreadCount();
    const interval = setInterval(fetchUnreadCount, 30_000);

    return () => {
      clearInterval(interval);
      sseRef.current?.close();
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

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
        className="relative p-2 rounded-lg text-gray-500 dark:text-slate-400 hover:text-gray-800 dark:hover:text-white
                   hover:bg-gray-100 dark:hover:bg-slate-800 transition-colors"
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
        <div className="absolute right-0 mt-2 w-80 bg-white dark:bg-slate-800 rounded-xl shadow-lg border
                        border-gray-200 dark:border-slate-700 z-50 overflow-hidden">
          {/* Header */}
          <div className="px-4 py-3 border-b border-gray-100 dark:border-slate-700 flex items-center justify-between">
            <h3 className="text-sm font-semibold text-gray-900 dark:text-white">Notifications</h3>
            {notifications.length > 0 && (
              <span className="text-xs text-gray-400 dark:text-slate-500">{notifications.length} recent</span>
            )}
          </div>

          {/* Body */}
          <div className="max-h-96 overflow-y-auto divide-y divide-gray-50 dark:divide-slate-700/50">
            {loading && (
              <div className="flex justify-center py-8">
                <div className="w-5 h-5 border-2 border-blue-500 border-t-transparent
                                rounded-full animate-spin" />
              </div>
            )}

            {!loading && notifications.length === 0 && (
              <div className="py-10 text-center">
                <p className="mt-2 text-sm text-gray-500 dark:text-slate-400">No notifications yet</p>
                <p className="text-xs text-gray-400 dark:text-slate-500 mt-0.5">
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
    TASK_CREATED: 'green',
    TASK_UPDATED: 'blue',
    TASK_DELETED: 'red',
  }[n.eventType] ?? 'gray';

  const iconLabel = {
    TASK_CREATED: 'Created',
    TASK_UPDATED: 'Updated',
    TASK_DELETED: 'Deleted',
  }[n.eventType] ?? 'Event';

  const timeAgo = n.createdAt
    ? formatDistanceToNow(new Date(n.createdAt), { addSuffix: true })
    : '';

  return (
    <div className={`px-4 py-3 hover:bg-gray-50 dark:hover:bg-slate-700/50 transition-colors ${
      !n.read ? 'bg-blue-50/50 dark:bg-blue-900/20' : ''
    }`}>
      <div className="flex gap-3 items-start">
        <span className={`inline-flex items-center justify-center w-6 h-6 rounded-full text-[10px] font-bold text-white mt-0.5 shrink-0 bg-${icon}-500`}>
          {iconLabel.charAt(0)}
        </span>
        <div className="flex-1 min-w-0">
          <p className="text-xs text-gray-800 dark:text-slate-300 leading-relaxed">{n.message}</p>
          <p className="text-[11px] text-gray-400 dark:text-slate-500 mt-1">{timeAgo}</p>
        </div>
        {!n.read && (
          <span className="w-2 h-2 rounded-full bg-blue-500 shrink-0 mt-1.5" />
        )}
      </div>
    </div>
  );
}
