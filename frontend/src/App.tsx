import { Navigate, Route, Routes } from 'react-router';
import ActivationPage from './activation/ActivationPage';
import LoginPage from './auth/LoginPage';
import RequireAuth from './auth/RequireAuth';
import HomePage from './home/HomePage';
import ProfilePage from './profile/ProfilePage';
import RegistrationPage from './registration/RegistrationPage';
import RegistrationSuccessPage from './registration/RegistrationSuccessPage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/inicio" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/inicio" element={<RequireAuth>{(user) => <HomePage user={user} />}</RequireAuth>} />
      <Route path="/perfil" element={<RequireAuth>{() => <ProfilePage />}</RequireAuth>} />
      <Route path="/cadastro" element={<RegistrationPage />} />
      <Route path="/cadastro/sucesso" element={<RegistrationSuccessPage />} />
      <Route path="/ativar" element={<ActivationPage />} />
      <Route path="*" element={<Navigate to="/cadastro" replace />} />
    </Routes>
  );
}
