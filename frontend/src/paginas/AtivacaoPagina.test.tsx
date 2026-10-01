import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { renderizar } from '../test/renderizar';
import { servidor } from '../test/servidor';
import { AtivacaoPagina } from './AtivacaoPagina';

function erroToken(codigo: string) {
  return HttpResponse.json(
    { detail: 'erro', codigo },
    { status: 400, headers: { 'Content-Type': 'application/problem+json' } },
  );
}

describe('AtivacaoPagina', () => {
  it('ativa a conta chamando a API uma única vez, mesmo em StrictMode', async () => {
    let chamadas = 0;
    let tokenRecebido: unknown;
    servidor.use(
      http.post('/api/ativacao', async ({ request }) => {
        chamadas += 1;
        tokenRecebido = ((await request.json()) as { token: string }).token;
        return new HttpResponse(null, { status: 204 });
      }),
    );

    renderizar('/ativar?token=abc123', [{ caminho: '/ativar', elemento: <AtivacaoPagina /> }], { strict: true });

    expect(await screen.findByText('Conta ativada com sucesso!')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ir para o login' })).toHaveAttribute('href', '/login');
    expect(chamadas).toBe(1);
    expect(tokenRecebido).toBe('abc123');
  });

  it.each([
    ['TOKEN_EXPIRADO', 'Este link de ativação expirou.'],
    ['TOKEN_JA_UTILIZADO', 'Este link já foi utilizado. Se você já ativou sua conta, faça login.'],
    ['TOKEN_INVALIDO', 'Link de ativação inválido.'],
  ])('mostra a mensagem específica para %s', async (codigo, mensagem) => {
    servidor.use(http.post('/api/ativacao', () => erroToken(codigo)));

    renderizar('/ativar?token=abc', [{ caminho: '/ativar', elemento: <AtivacaoPagina /> }]);

    expect(await screen.findByText(mensagem)).toBeInTheDocument();
  });

  it('sem token na URL mostra link inválido sem chamar a API', async () => {
    renderizar('/ativar', [{ caminho: '/ativar', elemento: <AtivacaoPagina /> }]);

    expect(await screen.findByText('Link de ativação inválido.')).toBeInTheDocument();
  });
});
