import { useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { logout, type CurrentUser } from '../api';

/** Área interna: saudação ao usuário autenticado, acesso ao perfil e opção de sair. */
export default function HomePage({ user }: { user: CurrentUser }) {
  const navigate = useNavigate();
  const [leaving, setLeaving] = useState(false);
  const [error, setError] = useState<string>();

  async function handleLogout() {
    setLeaving(true);
    setError(undefined);
    const result = await logout();
    if (result.ok) {
      navigate('/login', { replace: true, state: { loggedOut: true } });
      return;
    }
    setLeaving(false);
    setError(result.problem.detail);
  }

  return (
    <main className="container">
      <h1>Olá, {user.name}!</h1>
      <p>
        Você entrou com o e-mail <strong>{user.email}</strong>.
      </p>
      <p>
        <Link to="/perfil">Meu perfil</Link>
      </p>

      {error && (
        <div role="alert" className="alert alert--error">
          {error}
        </div>
      )}

      <button type="button" className="button" onClick={handleLogout} disabled={leaving}>
        {leaving ? 'Saindo...' : 'Sair'}
      </button>
    </main>
  );
}
