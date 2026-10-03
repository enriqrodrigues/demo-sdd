import { useState } from 'react';
import { Link, useSearchParams } from 'react-router';
import { activate } from '../api';

type Outcome =
  | { kind: 'idle' }
  | { kind: 'loading' }
  | { kind: 'success'; email: string }
  | { kind: 'expired' }
  | { kind: 'used' }
  | { kind: 'invalid' }
  | { kind: 'error'; message: string };

/**
 * Ativação da conta (RF05). Abrir o link não ativa nada: a ativação só ocorre
 * quando o usuário clica no botão, o que evita que verificadores automáticos
 * de e-mail ativem a conta.
 */
export default function ActivationPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token')?.trim() ?? '';
  const [outcome, setOutcome] = useState<Outcome>(token ? { kind: 'idle' } : { kind: 'invalid' });

  async function handleActivate() {
    setOutcome({ kind: 'loading' });
    const result = await activate(token);
    if (result.ok) {
      setOutcome({ kind: 'success', email: result.data.email });
      return;
    }
    switch (result.problem.code) {
      case 'TOKEN_EXPIRED':
        setOutcome({ kind: 'expired' });
        break;
      case 'TOKEN_ALREADY_USED':
        setOutcome({ kind: 'used' });
        break;
      case 'INVALID_TOKEN':
      case 'VALIDATION_ERROR':
        setOutcome({ kind: 'invalid' });
        break;
      default:
        setOutcome({ kind: 'error', message: result.problem.detail });
    }
  }

  return (
    <main className="container">
      <h1>Ativação da conta</h1>

      {(outcome.kind === 'idle' || outcome.kind === 'loading' || outcome.kind === 'error') && (
        <>
          {outcome.kind === 'error' && (
            <div role="alert" className="alert alert--error">
              {outcome.message}
            </div>
          )}
          <p>Clique no botão abaixo para confirmar a ativação da sua conta.</p>
          <button type="button" className="button" onClick={handleActivate} disabled={outcome.kind === 'loading'}>
            {outcome.kind === 'loading' ? 'Ativando...' : 'Ativar minha conta'}
          </button>
        </>
      )}

      {outcome.kind === 'success' && (
        <>
          <div role="status" className="alert alert--success">
            Conta ativada com sucesso! O e-mail <strong>{outcome.email}</strong> está confirmado.
          </div>
          <Link to="/login" className="button">
            Entrar
          </Link>
        </>
      )}

      {outcome.kind === 'expired' && (
        <div role="alert" className="alert alert--error">
          <p>Este link de ativação expirou.</p>
          <p>
            Você pode fazer um <Link to="/cadastro">novo cadastro</Link> com os mesmos dados.
          </p>
        </div>
      )}

      {outcome.kind === 'used' && (
        <div role="alert" className="alert alert--info">
          Este link de ativação já foi utilizado.
        </div>
      )}

      {outcome.kind === 'invalid' && (
        <div role="alert" className="alert alert--error">
          Este link de ativação é inválido. Verifique se você copiou o link completo do e-mail.
        </div>
      )}
    </main>
  );
}
