import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router';
import { ApiError, requisitar } from '../api/cliente';

const MENSAGENS: Record<string, string> = {
  TOKEN_EXPIRADO: 'Este link de ativação expirou.',
  TOKEN_JA_UTILIZADO: 'Este link já foi utilizado. Se você já ativou sua conta, faça login.',
  TOKEN_INVALIDO: 'Link de ativação inválido.',
};

type Estado = { tipo: 'carregando' } | { tipo: 'sucesso' } | { tipo: 'erro'; mensagem: string };

export function AtivacaoPagina() {
  const [parametros] = useSearchParams();
  const token = parametros.get('token');
  const [estado, setEstado] = useState<Estado>(
    token ? { tipo: 'carregando' } : { tipo: 'erro', mensagem: MENSAGENS.TOKEN_INVALIDO },
  );
  // O token é de uso único: o StrictMode (e remontagens) não podem disparar uma segunda chamada.
  const enviado = useRef(false);

  useEffect(() => {
    if (!token || enviado.current) {
      return;
    }
    enviado.current = true;
    requisitar<void>('POST', '/api/ativacao', { token })
      .then(() => setEstado({ tipo: 'sucesso' }))
      .catch((erro: unknown) => {
        const mensagem =
          (erro instanceof ApiError && erro.codigo && MENSAGENS[erro.codigo]) ||
          'Não foi possível ativar a conta. Tente novamente mais tarde.';
        setEstado({ tipo: 'erro', mensagem });
      });
  }, [token]);

  return (
    <section className="cartao">
      <h1>Ativação de conta</h1>
      {estado.tipo === 'carregando' && <p>Ativando sua conta…</p>}
      {estado.tipo === 'sucesso' && <div className="alerta alerta-sucesso">Conta ativada com sucesso!</div>}
      {estado.tipo === 'erro' && (
        <div className="alerta alerta-erro" role="alert">
          {estado.mensagem}
        </div>
      )}
      {estado.tipo !== 'carregando' && <Link to="/login">Ir para o login</Link>}
    </section>
  );
}
