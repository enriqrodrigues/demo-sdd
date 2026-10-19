import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import {
  concluirTarefa,
  consultarScore,
  consultarTarefa,
  criarTarefa,
  editarTarefa,
  ErroComunicacao,
  ErroConflito,
  ErroNaoEncontrado,
  ErroValidacao,
  excluirTarefa,
  listarTarefas,
  reabrirTarefa,
  type Tarefa,
} from './api'
import { servidor } from './test/servidor'

const tarefa: Tarefa = {
  id: '11111111-1111-4111-8111-111111111111',
  titulo: 'Pagar conta',
  descricao: null,
  dataVencimento: '2026-10-25',
  prioridade: 'ALTA',
  status: 'PENDENTE',
  concluidaEm: null,
  situacaoPrazo: 'NO_PRAZO',
  version: 0,
}

const problema = (status: number, extra: object = {}) =>
  HttpResponse.json(
    { type: 'about:blank', title: 'Erro', status, detail: 'detalhe', ...extra },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  )

describe('api: chamadas aos endpoints', () => {
  it('lista tarefas enviando os filtros como parâmetros', async () => {
    let url: URL | undefined
    servidor.use(
      http.get('/api/tarefas', ({ request }) => {
        url = new URL(request.url)
        return HttpResponse.json([tarefa])
      }),
    )

    const tarefas = await listarTarefas({ situacaoPrazo: 'ATRASADA' })

    expect(tarefas).toEqual([tarefa])
    expect(url?.searchParams.get('situacaoPrazo')).toBe('ATRASADA')
    expect(url?.searchParams.has('status')).toBe(false)
  })

  it('consulta uma tarefa', async () => {
    servidor.use(http.get(`/api/tarefas/${tarefa.id}`, () => HttpResponse.json(tarefa)))

    expect(await consultarTarefa(tarefa.id)).toEqual(tarefa)
  })

  it('cria uma tarefa enviando os dados em JSON', async () => {
    let corpo: unknown
    servidor.use(
      http.post('/api/tarefas', async ({ request }) => {
        corpo = await request.json()
        return HttpResponse.json(tarefa, { status: 201 })
      }),
    )

    const criada = await criarTarefa({
      titulo: 'Pagar conta',
      descricao: '',
      dataVencimento: '2026-10-25',
      prioridade: 'ALTA',
    })

    expect(criada).toEqual(tarefa)
    expect(corpo).toEqual({ titulo: 'Pagar conta', descricao: '', dataVencimento: '2026-10-25', prioridade: 'ALTA' })
  })

  it('edita uma tarefa enviando a version', async () => {
    let corpo: unknown
    servidor.use(
      http.put(`/api/tarefas/${tarefa.id}`, async ({ request }) => {
        corpo = await request.json()
        return HttpResponse.json({ ...tarefa, version: 4 })
      }),
    )

    const editada = await editarTarefa(
      tarefa.id,
      { titulo: 'Pagar conta', descricao: '', dataVencimento: '2026-10-25', prioridade: 'ALTA' },
      3,
    )

    expect(editada.version).toBe(4)
    expect(corpo).toMatchObject({ titulo: 'Pagar conta', version: 3 })
  })

  it('conclui e reabre uma tarefa', async () => {
    servidor.use(
      http.post(`/api/tarefas/${tarefa.id}/concluir`, () =>
        HttpResponse.json({ ...tarefa, status: 'CONCLUIDA', situacaoPrazo: 'CONCLUIDA' }),
      ),
      http.post(`/api/tarefas/${tarefa.id}/reabrir`, () => HttpResponse.json(tarefa)),
    )

    expect((await concluirTarefa(tarefa.id)).status).toBe('CONCLUIDA')
    expect((await reabrirTarefa(tarefa.id)).status).toBe('PENDENTE')
  })

  it('exclui uma tarefa', async () => {
    let excluiu = false
    servidor.use(
      http.delete(`/api/tarefas/${tarefa.id}`, () => {
        excluiu = true
        return new HttpResponse(null, { status: 204 })
      }),
    )

    await excluirTarefa(tarefa.id)

    expect(excluiu).toBe(true)
  })

  it('consulta o score da semana', async () => {
    const score = { inicioSemana: '2026-10-19', fimSemana: '2026-10-25', concluidas: 3, noPrazo: 2, percentual: 66.67 }
    servidor.use(http.get('/api/produtividade/semana', () => HttpResponse.json(score)))

    expect(await consultarScore()).toEqual(score)
  })
})

describe('api: conversão de erros', () => {
  it('400 com erros vira ErroValidacao com as mensagens por campo', async () => {
    servidor.use(
      http.post('/api/tarefas', () =>
        problema(400, {
          erros: [
            { campo: 'titulo', mensagem: 'Informe o título.' },
            { campo: 'prioridade', mensagem: 'A prioridade deve ser BAIXA, MEDIA ou ALTA.' },
          ],
        }),
      ),
    )

    const erro = await criarTarefa({ titulo: '', descricao: '', dataVencimento: '', prioridade: 'ALTA' }).catch(
      (e: unknown) => e,
    )

    expect(erro).toBeInstanceOf(ErroValidacao)
    expect((erro as ErroValidacao).erros).toEqual({
      titulo: 'Informe o título.',
      prioridade: 'A prioridade deve ser BAIXA, MEDIA ou ALTA.',
    })
  })

  it('400 sem erros (corpo ilegível) vira ErroValidacao sem campos', async () => {
    servidor.use(http.post('/api/tarefas', () => problema(400)))

    const erro = await criarTarefa({ titulo: 'x', descricao: '', dataVencimento: '', prioridade: 'ALTA' }).catch(
      (e: unknown) => e,
    )

    expect(erro).toBeInstanceOf(ErroValidacao)
    expect((erro as ErroValidacao).erros).toEqual({})
  })

  it('409 vira ErroConflito', async () => {
    servidor.use(http.put(`/api/tarefas/${tarefa.id}`, () => problema(409)))

    await expect(
      editarTarefa(tarefa.id, { titulo: 'x', descricao: '', dataVencimento: '2026-10-25', prioridade: 'ALTA' }, 0),
    ).rejects.toBeInstanceOf(ErroConflito)
  })

  it('404 vira ErroNaoEncontrado', async () => {
    servidor.use(http.post(`/api/tarefas/${tarefa.id}/concluir`, () => problema(404)))

    await expect(concluirTarefa(tarefa.id)).rejects.toBeInstanceOf(ErroNaoEncontrado)
  })

  it('5xx vira ErroComunicacao', async () => {
    servidor.use(http.get('/api/produtividade/semana', () => problema(500)))

    await expect(consultarScore()).rejects.toBeInstanceOf(ErroComunicacao)
  })

  it('falha de rede vira ErroComunicacao', async () => {
    servidor.use(http.post(`/api/tarefas/${tarefa.id}/concluir`, () => HttpResponse.error()))

    await expect(concluirTarefa(tarefa.id)).rejects.toBeInstanceOf(ErroComunicacao)
  })
})
