import { render, screen, act } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { AuthProvider, useAuth } from '../context/AuthContext';

// Mock the API and axios client
vi.mock('../api/auth', () => ({
  authApi: {
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    refresh: vi.fn().mockRejectedValue(new Error('no token')),
  },
}));

vi.mock('../api/axiosClient', () => ({
  setAccessToken: vi.fn(),
  setRefreshToken: vi.fn(),
  getRefreshToken: vi.fn().mockReturnValue(null),
  setOnForceLogout: vi.fn(),
}));

function TestConsumer() {
  const { isAuthenticated, isLoading, user } = useAuth();
  return (
    <div>
      <span data-testid="loading">{isLoading.toString()}</span>
      <span data-testid="auth">{isAuthenticated.toString()}</span>
      <span data-testid="user">{user?.firstName ?? 'none'}</span>
    </div>
  );
}

describe('AuthContext', () => {
  it('starts unauthenticated when no refresh token exists', async () => {
    await act(async () => {
      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      );
    });

    expect(screen.getByTestId('auth').textContent).toBe('false');
    expect(screen.getByTestId('user').textContent).toBe('none');
  });

  it('throws when useAuth is used outside provider', () => {
    expect(() => {
      render(<TestConsumer />);
    }).toThrow('useAuth must be used within <AuthProvider>');
  });
});
