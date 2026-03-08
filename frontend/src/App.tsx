import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Login }      from './pages/Login';
import { Register }   from './pages/Register';
import { Dashboard }  from './pages/Dashboard';
import { TaskDetail } from './pages/TaskDetail';
import { Profile } from './pages/Profile';

/**
 * Root application component.
 *
 * Route layout:
 *   /                  → redirect to /dashboard
 *   /login             → public
 *   /register          → public
 *   /dashboard         → protected (requires auth)
 *   /tasks/new         → protected
 *   /tasks/:id         → protected
 */
function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public routes */}
          <Route path="/login"    element={<Login />}    />
          <Route path="/register" element={<Register />} />

          {/* Protected routes */}
          <Route element={<ProtectedRoute />}>
            <Route path="/dashboard"  element={<Dashboard />}  />
            <Route path="/tasks/:id"  element={<TaskDetail />}  />
            <Route path="/profile"    element={<Profile />}     />
          </Route>

          {/* Default redirect */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
