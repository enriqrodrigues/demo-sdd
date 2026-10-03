import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { servidor } from '../test/servidor';
import { ApiError, requisitar } from './cliente';

describe('requisitar', () => {
  it('envia JSON com o token CSRF do cookie em requisições mutáveis', async () => {
    let cabecalho: string | null = null;
    let corpo: unknown;
    servidor.use(
      http.post('/api/exemplo', async ({ request }) => {
        cabecalho = request.headers.get('X-XSRF-TOKEN');
        corpo = await request.json();
        return HttpResponse.json({ ok: true });
      }),
    );

    const resposta = await requisitar<{ ok: boolean }>('POST', '/api/exemplo', { a: 1 });

    expect(resposta).toEqual({ ok: true });
    expect(cabecalho).toBe('token-teste');
    expect(corpo).toEqual({ a: 1 });
  });

  it('busca o token em /api/csrf quando o cookie não existe', async () => {
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT';
    let csrfChamado = false;
    servidor.use(
      http.get('/api/csrf', () => {
        csrfChamado = true;
        document.cookie = 'XSRF-TOKEN=token-novo';
        return HttpResponse.json({ token: 'token-novo' });
      }),
      http.post('/api/exemplo', ({ request }) =>
        HttpResponse.json({ cabecalho: request.headers.get('X-XSRF-TOKEN') }),
      ),
    );

    const resposta = await requisitar<{ cabecalho: string }>('POST', '/api/exemplo', {});

    expect(csrfChamado).toBe(true);
    expect(resposta.cabecalho).toBe('token-novo');
  });

  it('retorna undefined para 204', async () => {
    servidor.use(http.post('/api/exemplo', () => new HttpResponse(null, { status: 204 })));

    await expect(requisitar('POST', '/api/exemplo')).resolves.toBeUndefined();
  });

  it('converte problem+json em ApiError', async () => {
    servidor.use(
      http.post('/api/exemplo', () =>
        HttpResponse.json(
          {
            detail: 'Dados inválidos',
            codigo: 'VALIDACAO',
            erros: [{ campo: 'cpf', mensagem: 'CPF inválido' }],
          },
          { status: 400, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );

    const erro = await requisitar('POST', '/api/exemplo', {}).catch((e: unknown) => e);

    expect(erro).toBeInstanceOf(ApiError);
    expect(erro).toMatchObject({
      status: 400,
      codigo: 'VALIDACAO',
      message: 'Dados inválidos',
      erros: [{ campo: 'cpf', mensagem: 'CPF inválido' }],
    });
  });

  it('usa mensagem genérica quando a resposta de erro não é JSON', async () => {
    servidor.use(http.get('/api/exemplo', () => new HttpResponse('falhou', { status: 500 })));

    const erro = await requisitar('GET', '/api/exemplo').catch((e: unknown) => e);

    expect(erro).toMatchObject({ status: 500, message: 'Erro inesperado', erros: [] });
  });
});
