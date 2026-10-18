import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  ErroApi,
  ErroValidacao,
  MENSAGEM_FALHA_COMUNICACAO,
  atualizarTarefa,
  buscarScore,
  criarTarefa,
  excluirTarefa,
  listarTarefas,
  type Tarefa,
} from './api'

const tarefa: Tarefa = {
  id: 'a1',
  titulo: 'Pagar conta',
  descricao: 'Luz',
  dataVencimento: '2026-10-20',
  prioridade: 'ALTA',
  status: 'PENDENTE',
  dataConclusao: null,
  situacaoPrazo: 'NO_PRAZO',
}

function json(corpo: unknown, status = 200, tipo = 'application/json'): Response {
  return new Response(JSON.stringify(corpo), { status, headers: { 'Content-Type': tipo } })
}

const fetchMock = vi.fn<typeof fetch>()

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock)
  fetchMock.mockReset()
})

describe('api', () => {
  it('lista as tarefas', async () => {
    fetchMock.mockResolvedValue(json([tarefa]))

    await expect(listarTarefas()).resolves.toEqual([tarefa])
    expect(fetchMock).toHaveBeenCalledWith('/api/tarefas', expect.objectContaining({}))
  })

  it('cria uma tarefa enviando JSON por POST', async () => {
    fetchMock.mockResolvedValue(json(tarefa, 201))
    const dados = { titulo: 'Pagar conta', descricao: 'Luz', dataVencimento: '2026-10-20', prioridade: 'ALTA' as const }

    await expect(criarTarefa(dados)).resolves.toEqual(tarefa)

    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/tarefas')
    expect(init?.method).toBe('POST')
    expect(init?.headers).toEqual({ 'Content-Type': 'application/json' })
    expect(JSON.parse(init?.body as string)).toEqual(dados)
  })

  it('atualiza uma tarefa por PUT com o status', async () => {
    fetchMock.mockResolvedValue(json({ ...tarefa, status: 'CONCLUIDA' }))

    await atualizarTarefa('a1', {
      titulo: 'Pagar conta',
      descricao: 'Luz',
      dataVencimento: '2026-10-20',
      prioridade: 'ALTA',
      status: 'CONCLUIDA',
    })

    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/tarefas/a1')
    expect(init?.method).toBe('PUT')
    expect(JSON.parse(init?.body as string)).toMatchObject({ status: 'CONCLUIDA' })
  })

  it('exclui uma tarefa e aceita a resposta 204 sem corpo', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))

    await expect(excluirTarefa('a1')).resolves.toBeUndefined()

    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/tarefas/a1')
    expect(init?.method).toBe('DELETE')
  })

  it('busca o score', async () => {
    const score = {
      inicioSemana: '2026-10-12',
      fimSemana: '2026-10-18',
      concluidasNoPrazo: 3,
      totalConcluidas: 4,
      percentual: 75,
    }
    fetchMock.mockResolvedValue(json(score))

    await expect(buscarScore()).resolves.toEqual(score)
    expect(fetchMock.mock.calls[0][0]).toBe('/api/produtividade/score')
  })

  it('converte ProblemDetail com errors em erros por campo', async () => {
    fetchMock.mockResolvedValue(
      json(
        {
          type: 'about:blank',
          title: 'Dados inválidos',
          status: 400,
          detail: 'Um ou mais campos são inválidos.',
          errors: [
            { campo: 'titulo', mensagem: 'O título é obrigatório.' },
            { campo: 'prioridade', mensagem: 'A prioridade é obrigatória.' },
          ],
        },
        400,
        'application/problem+json',
      ),
    )

    const erro = await criarTarefa({ titulo: '', descricao: null, dataVencimento: null, prioridade: null }).catch(
      (e: unknown) => e,
    )

    expect(erro).toBeInstanceOf(ErroValidacao)
    expect((erro as ErroValidacao).erros).toEqual({
      titulo: 'O título é obrigatório.',
      prioridade: 'A prioridade é obrigatória.',
    })
  })

  it('converte ProblemDetail sem errors em erro genérico com o detail', async () => {
    fetchMock.mockResolvedValue(
      json(
        { type: 'about:blank', title: 'Tarefa não encontrada', status: 404, detail: 'Não existe tarefa com o id a1.' },
        404,
        'application/problem+json',
      ),
    )

    const erro = await excluirTarefa('a1').catch((e: unknown) => e)

    expect(erro).toBeInstanceOf(ErroApi)
    expect((erro as Error).message).toBe('Não existe tarefa com o id a1.')
  })

  it('trata falha de rede como erro de comunicação', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))

    const erro = await listarTarefas().catch((e: unknown) => e)

    expect(erro).toBeInstanceOf(ErroApi)
    expect((erro as Error).message).toBe(MENSAGEM_FALHA_COMUNICACAO)
  })

  it('trata resposta de erro sem JSON (proxy sem backend) como erro de comunicação', async () => {
    fetchMock.mockResolvedValue(new Response('Internal Server Error', { status: 500 }))

    const erro = await listarTarefas().catch((e: unknown) => e)

    expect(erro).toBeInstanceOf(ErroApi)
    expect((erro as Error).message).toBe(MENSAGEM_FALHA_COMUNICACAO)
  })
})
