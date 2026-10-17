import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { TarefaInput } from '../tipos';
import { ErroApi, tarefasApi } from './tarefasApi';

const fetchMock = vi.fn();

function respostaJson(status: number, corpo: unknown): Response {
  return new Response(JSON.stringify(corpo), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

const dados: TarefaInput = {
  titulo: 'Pagar boleto',
  descricao: null,
  dataVencimento: '2026-10-16',
  prioridade: 'ALTA',
  status: 'PENDENTE',
};

describe('tarefasApi', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock);
  });

  afterEach(() => {
    fetchMock.mockReset();
    vi.unstubAllGlobals();
  });

  it('lista tarefas com GET em /api/tarefas', async () => {
    fetchMock.mockResolvedValue(respostaJson(200, [{ id: '1' }]));

    const tarefas = await tarefasApi.listar();

    expect(fetchMock.mock.calls[0][0]).toBe('/api/tarefas');
    expect(tarefas).toEqual([{ id: '1' }]);
  });

  it('cria tarefa com POST e corpo JSON', async () => {
    fetchMock.mockResolvedValue(respostaJson(201, { id: '1', ...dados }));

    await tarefasApi.criar(dados);

    expect(fetchMock).toHaveBeenCalledWith('/api/tarefas', {
      method: 'POST',
      body: JSON.stringify(dados),
      headers: { 'Content-Type': 'application/json' },
    });
  });

  it('atualiza tarefa com PUT no id', async () => {
    fetchMock.mockResolvedValue(respostaJson(200, { id: 'abc', ...dados }));

    await tarefasApi.atualizar('abc', dados);

    expect(fetchMock.mock.calls[0][0]).toBe('/api/tarefas/abc');
    expect(fetchMock.mock.calls[0][1].method).toBe('PUT');
  });

  it('exclui tarefa e aceita resposta 204 sem corpo', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));

    await expect(tarefasApi.excluir('abc')).resolves.toBeUndefined();
    expect(fetchMock.mock.calls[0][1].method).toBe('DELETE');
  });

  it('busca o score semanal', async () => {
    fetchMock.mockResolvedValue(respostaJson(200, { totalConcluidas: 0, percentualNoPrazo: null }));

    const score = await tarefasApi.scoreSemanal();

    expect(fetchMock.mock.calls[0][0]).toBe('/api/produtividade/score-semanal');
    expect(score.percentualNoPrazo).toBeNull();
  });

  it('converte problem+json em ErroApi com erros de campo', async () => {
    fetchMock.mockResolvedValue(
      respostaJson(400, {
        title: 'Dados inválidos',
        detail: 'Um ou mais campos são inválidos.',
        erros: [{ campo: 'titulo', mensagem: 'é obrigatório' }],
      }),
    );

    const erro = await tarefasApi.criar(dados).catch((e: unknown) => e);

    expect(erro).toBeInstanceOf(ErroApi);
    expect(erro).toMatchObject({
      status: 400,
      detail: 'Um ou mais campos são inválidos.',
      erros: [{ campo: 'titulo', mensagem: 'é obrigatório' }],
    });
  });

  it('converte falha de rede em ErroApi com status 0', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'));

    const erro = await tarefasApi.listar().catch((e: unknown) => e);

    expect(erro).toMatchObject({ status: 0, detail: 'Não foi possível conectar ao servidor.', erros: [] });
  });

  it('usa mensagem genérica quando o erro não tem corpo JSON', async () => {
    fetchMock.mockResolvedValue(new Response('Bad Gateway', { status: 502 }));

    const erro = await tarefasApi.listar().catch((e: unknown) => e);

    expect(erro).toMatchObject({ status: 502, detail: 'Erro 502', erros: [] });
  });
});
