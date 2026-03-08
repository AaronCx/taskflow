import axiosClient from './axiosClient';
import { UserSummary } from './users';

export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
  email: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export const profileApi = {
  get: () => axiosClient.get<UserSummary>('/users/me'),

  update: (data: UpdateProfileRequest) =>
    axiosClient.put<UserSummary>('/users/me', data),

  changePassword: (data: ChangePasswordRequest) =>
    axiosClient.put<{ message: string }>('/users/me/password', data),
};
