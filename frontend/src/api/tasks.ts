import axiosClient from './axiosClient';
import { Task, TaskRequest, TaskStatus } from '../types';

export const tasksApi = {
  /** Fetch all tasks, optionally filtered by status and/or search query. */
  getAll: (status?: TaskStatus, search?: string) =>
    axiosClient.get<Task[]>('/tasks', {
      params: {
        ...(status ? { status } : {}),
        ...(search ? { search } : {}),
      },
    }),

  getById: (id: number) =>
    axiosClient.get<Task>(`/tasks/${id}`),

  create: (data: TaskRequest) =>
    axiosClient.post<Task>('/tasks', data),

  update: (id: number, data: TaskRequest) =>
    axiosClient.put<Task>(`/tasks/${id}`, data),

  remove: (id: number) =>
    axiosClient.delete(`/tasks/${id}`),

  bulkUpdateStatus: (ids: number[], status: string) =>
    axiosClient.put<{ updated: number }>('/tasks/bulk-update', { ids, status }),

  bulkDelete: (ids: number[]) =>
    axiosClient.delete<{ deleted: number }>('/tasks/bulk-delete', { data: { ids } }),
};
