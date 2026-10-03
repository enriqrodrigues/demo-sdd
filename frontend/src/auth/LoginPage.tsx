import { useEffect, useState, type FormEvent } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router';
import { login, me } from '../api';
import FormField from '../registration/FormField';
import { REQUIRED } from '../registration/validation';

type Field = 'email' | 'password';
type Message = { kind: 'error' | 'info'; text: string };

/** Login com e-mail e senha (RF06). Contas pendentes recebem a orientação de ativação (RN04). */
export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const loggedOut = (location.state as { loggedOut?: boolean } | null)?.loggedOut === true;
  const [values, setValues] = useState<Record<Field, string>>({ email: '', password: '' });
  const [errors, setErrors] = useState<Partial<Record<Field, string>>>({});
  const [message, setMessage] = useState<Message>();
  const [submitting, setSubmitting] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);

  // Quem já tem sessão vai direto para a área interna.
  useEffect(() => {
    let active = true;
    me().then((result) => {
      if (active && result.ok) setAuthenticated(true);
    });
    return () => {
      active = false;
    };
  }, []);

  function change(field: Field, value: string) {
    setValues((current) => ({ ...current, [field]: value }));
    if (errors[field] && value.trim()) {
      setErrors((current) => ({ ...current, [field]: undefined }));
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setMessage(undefined);

    const required: Partial<Record<Field, string>> = {};
    if (!values.email.trim()) required.email = REQUIRED;
    if (!values.password.trim()) required.password = REQUIRED;
    setErrors(required);
    const firstInvalid = (['email', 'password'] as const).find((field) => required[field]);
    if (firstInvalid) {
      document.getElementById(firstInvalid)?.focus();
      return;
    }

    setSubmitting(true);
    const result = await login(values.email, values.password);
    setSubmitting(false);

    if (result.ok) {
      navigate('/inicio', { replace: true });
      return;
    }
    const { problem } = result;
    switch (problem.code) {
      case 'ACCOUNT_PENDING':
        setMessage({ kind: 'info', text: problem.detail });
        break;
      case 'VALIDATION_ERROR':
        setErrors({ email: problem.fieldErrors.email?.join(' '), password: problem.fieldErrors.password?.join(' ') });
        break;
      default:
        setMessage({ kind: 'error', text: problem.detail });
    }
    setValues((current) => ({ ...current, password: '' }));
  }

  if (authenticated) {
    return <Navigate to="/inicio" replace />;
  }

  return (
    <main className="container">
      <h1>Entrar</h1>

      {loggedOut && !message && (
        <div role="status" className="alert alert--success">
          Você saiu da sua conta.
        </div>
      )}

      {message && (
        <div role="alert" className={`alert alert--${message.kind}`}>
          {message.text}
        </div>
      )}

      <form onSubmit={handleSubmit} noValidate>
        <FormField id="email" label="E-mail" error={errors.email}>
          {(a11y) => (
            <input
              {...a11y}
              name="email"
              type="email"
              inputMode="email"
              autoComplete="username"
              value={values.email}
              onChange={(event) => change('email', event.target.value)}
            />
          )}
        </FormField>

        <FormField id="password" label="Senha" error={errors.password}>
          {(a11y) => (
            <input
              {...a11y}
              name="password"
              type="password"
              autoComplete="current-password"
              value={values.password}
              onChange={(event) => change('password', event.target.value)}
            />
          )}
        </FormField>

        <button type="submit" className="button" disabled={submitting}>
          {submitting ? 'Entrando...' : 'Entrar'}
        </button>
      </form>

      <p className="form-footer">
        Ainda não tem conta? <Link to="/cadastro">Cadastre-se</Link>
      </p>
    </main>
  );
}
