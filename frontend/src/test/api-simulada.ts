import { delay, http, HttpResponse } from 'msw'
import type { Score, Tarefa } from '../api'
import { servidor } from './servidor'

let sequencia = 0

/** Tarefa pendente válida; sobrescreva só o que importa para o teste. */
export function umaTarefa(campos: Partial<Tarefa> = {}): Tarefa {
  sequencia += 1
  return {
    id: `00000000-0000-4000-8000-${String(sequencia).padStart(12, '0')}`,
    titulo: `Tarefa ${sequencia}`,
    descricao: null,
    dataVencimento: '2026-10-25',
    prioridade: 'MEDIA',
    status: 'PENDENTE',
    concluidaEm: null,
    situacaoPrazo: 'NO_PRAZO',
    version: 0,
    ...campos,
  }
}

export const SCORE_VAZIO: Score = {
  inicioSemana: '2026-10-19',
  fimSemana: '2026-10-25',
  concluidas: 0,
  noPrazo: 0,
  percentual: null,
}

/** Registra respostas padrão para a listagem e o score, que a tela consulta ao abrir. */
export function usarApi({ tarefas = [] as Tarefa[], score = SCORE_VAZIO } = {}) {
  servidor.use(
    http.get('/api/tarefas', () => HttpResponse.json(tarefas)),
    http.get('/api/produtividade/semana', () => HttpResponse.json(score)),
  )
}

const HOJE = '2026-10-21'

function situacao(tarefa: Tarefa): Tarefa['situacaoPrazo'] {
  if (tarefa.status === 'CONCLUIDA') return 'CONCLUIDA'
  if (tarefa.dataVencimento < HOJE) return 'ATRASADA'
  return tarefa.dataVencimento === HOJE ? 'PARA_HOJE' : 'NO_PRAZO'
}

/**
 * API em memória (data corrente 2026-10-21) que aplica filtros, conclui, reabre e exclui, conta
 * as requisições recebidas e calcula um score simples. {@code atraso} atrasa as ações sobre tarefas.
 */
export function apiEmMemoria(iniciais: Tarefa[], { atraso = 0 } = {}) {
  const tarefas = iniciais.map((t) => ({ ...t, situacaoPrazo: situacao(t) }))
  const contagem = { concluir: 0, reabrir: 0, excluir: 0, score: 0 }
  const buscar = (id: unknown) => tarefas.find((t) => t.id === id)
  const transicao = (acao: 'concluir' | 'reabrir', status: Tarefa['status']) =>
    http.post(`/api/tarefas/:id/${acao}`, async ({ params }) => {
      contagem[acao] += 1
      await delay(atraso)
      const tarefa = buscar(params.id)
      if (!tarefa) return problema(404)
      if (tarefa.status !== status) {
        tarefa.status = status
        tarefa.concluidaEm = status === 'CONCLUIDA' ? '2026-10-21T09:00:00-03:00' : null
        tarefa.version += 1
        tarefa.situacaoPrazo = situacao(tarefa)
      }
      return HttpResponse.json(tarefa)
    })

  servidor.use(
    http.get('/api/tarefas', ({ request }) => {
      const parametros = new URL(request.url).searchParams
      return HttpResponse.json(
        tarefas.filter(
          (t) =>
            (!parametros.has('status') || t.status === parametros.get('status')) &&
            (!parametros.has('situacaoPrazo') || t.situacaoPrazo === parametros.get('situacaoPrazo')),
        ),
      )
    }),
    http.get('/api/produtividade/semana', () => {
      contagem.score += 1
      const concluidas = tarefas.filter((t) => t.status === 'CONCLUIDA').length
      return HttpResponse.json({
        ...SCORE_VAZIO,
        concluidas,
        noPrazo: concluidas,
        percentual: concluidas === 0 ? null : 100,
      })
    }),
    http.put('/api/tarefas/:id', async ({ params, request }) => {
      await delay(atraso)
      const tarefa = buscar(params.id)
      if (!tarefa) return problema(404)
      const { version, ...dados } = (await request.json()) as Record<string, unknown>
      if (version !== tarefa.version) return problema(409)
      Object.assign(tarefa, dados, { version: tarefa.version + 1 })
      tarefa.situacaoPrazo = situacao(tarefa)
      return HttpResponse.json(tarefa)
    }),
    transicao('concluir', 'CONCLUIDA'),
    transicao('reabrir', 'PENDENTE'),
    http.delete('/api/tarefas/:id', async ({ params }) => {
      contagem.excluir += 1
      await delay(atraso)
      const indice = tarefas.findIndex((t) => t.id === params.id)
      if (indice < 0) return problema(404)
      tarefas.splice(indice, 1)
      return new HttpResponse(null, { status: 204 })
    }),
  )
  /** Simula outra aba excluindo a tarefa depois que esta já a exibiu. */
  const excluirEmOutraAba = (titulo: string) => {
    tarefas.splice(
      tarefas.findIndex((t) => t.titulo === titulo),
      1,
    )
  }
  return { tarefas, contagem, excluirEmOutraAba }
}

export function problema(status: number, extra: object = {}) {
  return HttpResponse.json(
    { type: 'about:blank', title: 'Erro', status, detail: 'detalhe', ...extra },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  )
}
