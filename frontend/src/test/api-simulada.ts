import { http, HttpResponse } from 'msw'
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

export function problema(status: number, extra: object = {}) {
  return HttpResponse.json(
    { type: 'about:blank', title: 'Erro', status, detail: 'detalhe', ...extra },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  )
}
