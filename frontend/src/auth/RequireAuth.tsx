import { useEffect, useState, type ReactNode } from 'react';
import { Navigate } from 'react-router';
import { me, type CurrentUser } from '../api';

type State =
  | { kind: 'loading' }
  | { kind: 'authenticated'; user: CurrentUser }
  | { kind: 'unauthenticated' }
  | { kind: 'error'; message: string };

/**
 * Protege a área interna (RN04): sem sessão válida (401), leva ao login. O
 * usuário da sessão é repassado à página; não há contexto global (design D9).
 */
export default function RequireAuth({ children }: { children: (user: CurrentUser) => ReactNode }) {
  const [state, setState] = useState<State>({ kind: 'loading' });

  useEffect(() => {
    let active = true;
    me().then((result) => {
      if (!active) return;
      if (result.ok) {
        setState({ kind: 'authenticated', user: result.data });
      } else if (result.problem.status === 401) {
        setState({ kind: 'unauthenticated' });
      } else {
        setState({ kind: 'error', message: result.problem.detail });
      }
    });
    return () => {
      active = false;
    };
  }, []);

  switch (state.kind) {
    case 'loading':
      return (
        <main className="container">
          <p role="status">Carregando...</p>
        </main>
      );
    case 'unauthenticated':
      return <Navigate to="/login" replace />;
    case 'error':
      return (
        <main className="container">
          <div role="alert" className="alert alert--error">
            {state.message}
          </div>
        </main>
      );
    case 'authenticated':
      return children(state.user);
  }
}
