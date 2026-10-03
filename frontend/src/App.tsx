import { Navigate, Route, Routes } from 'react-router';
import RegistrationPage from './registration/RegistrationPage';
import RegistrationSuccessPage from './registration/RegistrationSuccessPage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/cadastro" replace />} />
      <Route path="/cadastro" element={<RegistrationPage />} />
      <Route path="/cadastro/sucesso" element={<RegistrationSuccessPage />} />
      <Route path="*" element={<Navigate to="/cadastro" replace />} />
    </Routes>
  );
}
