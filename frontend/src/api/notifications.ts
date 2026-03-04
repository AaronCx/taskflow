import axiosClient from './axiosClient';

export interface Notification {
  id: number;
  eventType: 'TASK_CREATED' | 'TASK_UPDATED' | 'TASK_DELETED';
  taskId: number | null;
  taskTitle: string | null;
  message: string;
  read: boolean;
  createdAt: string;
}

export interface UnreadCountResponse {
  count: number;
}

export const notificationsApi = {
  /** Fetch the most-recent 20 notifications for the current user. */
  getAll: () =>
    axiosClient.get<Notification[]>('/notifications'),

  /** Unread count for the bell badge. */
  getUnreadCount: () =>
    axiosClient.get<UnreadCountResponse>('/notifications/unread-count'),

  /** Mark every notification as read. */
  markAllRead: () =>
    axiosClient.put<{ marked: number }>('/notifications/read-all'),
};
