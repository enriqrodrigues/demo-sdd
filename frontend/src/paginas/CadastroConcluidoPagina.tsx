import { Link, useLocation } from 'react-router';

export function CadastroConcluidoPagina() {
  const { state } = useLocation();
  const email = (state as { email?: string } | null)?.email;
  return (
    <section className="cartao">
      <h1>Verifique seu e-mail</h1>
      <p>
        {email ? (
          <>
            Enviamos um link de ativação para <strong>{email}</strong>.
          </>
        ) : (
          'Enviamos um link de ativação para o e-mail cadastrado.'
        )}
      </p>
      <p>O link é válido por 24 horas.</p>
      <Link to="/login">Ir para o login</Link>
    </section>
  );
}
