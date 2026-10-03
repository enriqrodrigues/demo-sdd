import { useLocation } from 'react-router';

/** Confirmação do cadastro: orienta o usuário a ativar a conta pelo e-mail (RF04). */
export default function RegistrationSuccessPage() {
  const location = useLocation();
  const email = (location.state as { email?: string } | null)?.email;

  return (
    <main className="container">
      <h1>Cadastro realizado!</h1>
      <p>
        Enviamos um e-mail de ativação para {email ? <strong>{email}</strong> : 'o endereço cadastrado'}.
      </p>
      <p>Abra o e-mail e use o link para ativar sua conta. O link expira em 24 horas.</p>
    </main>
  );
}
