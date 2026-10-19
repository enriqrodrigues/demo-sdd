/** Cliente HTTP fino da API, que transforma Problem Details em erros tipados. */

export type Prioridade = 'BAIXA' | 'MEDIA' | 'ALTA'
export type StatusTarefa = 'PENDENTE' | 'CONCLUIDA'
export type SituacaoPrazo = 'CONCLUIDA' | 'ATRASADA' | 'PARA_HOJE' | 'NO_PRAZO'

export interface Tarefa {
  id: string
  titulo: string
  descricao: string | null
  dataVencimento: string
  prioridade: Prioridade
  status: StatusTarefa
  concluidaEm: string | null
  situacaoPrazo: SituacaoPrazo
  version: number
}

export interface DadosTarefa {
  titulo: string
  descricao: string
  dataVencimento: string
  prioridade: Prioridade
}

export interface FiltroTarefas {
  status?: StatusTarefa
  situacaoPrazo?: SituacaoPrazo
}

export interface Score {
  inicioSemana: string
  fimSemana: string
  concluidas: number
  noPrazo: number
  percentual: number | null
}

/** 400: {@link erros} traz a mensagem de cada campo inválido (vazio se o corpo era ilegível). */
export class ErroValidacao extends Error {
  readonly erros: Record<string, string>

  constructor(mensagem: string, erros: Record<string, string>) {
    super(mensagem)
    this.erros = erros
  }
}

/** 409: a tarefa foi alterada por outra ação. */
export class ErroConflito extends Error {}

/** 404: a tarefa não existe mais. */
export class ErroNaoEncontrado extends Error {}

/** Falha de rede, 5xx ou resposta inesperada. */
export class ErroComunicacao extends Error {}

interface ProblemDetails {
  detail?: string
  erros?: { campo: string; mensagem: string }[]
}

function url(caminho: string, parametros?: Record<string, string | undefined>): URL {
  const endereco = new URL(caminho, window.location.href)
  Object.entries(parametros ?? {}).forEach(([nome, valor]) => {
    if (valor !== undefined) {
      endereco.searchParams.set(nome, valor)
    }
  })
  return endereco
}

async function requisitar(endereco: URL, init?: RequestInit): Promise<Response> {
  let resposta: Response
  try {
    resposta = await fetch(endereco, init)
  } catch {
    throw new ErroComunicacao('Não foi possível falar com o servidor.')
  }
  if (resposta.ok) {
    return resposta
  }
  const problema: ProblemDetails = await resposta.json().catch(() => ({}))
  const detalhe = problema.detail ?? resposta.statusText
  switch (resposta.status) {
    case 400:
      throw new ErroValidacao(
        detalhe,
        Object.fromEntries((problema.erros ?? []).map((erro) => [erro.campo, erro.mensagem])),
      )
    case 404:
      throw new ErroNaoEncontrado(detalhe)
    case 409:
      throw new ErroConflito(detalhe)
    default:
      throw new ErroComunicacao(detalhe)
  }
}

async function json<T>(endereco: URL, init?: RequestInit): Promise<T> {
  return (await requisitar(endereco, init)).json() as Promise<T>
}

function comCorpo(metodo: string, corpo: unknown): RequestInit {
  return { method: metodo, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(corpo) }
}

export function listarTarefas(filtro: FiltroTarefas = {}): Promise<Tarefa[]> {
  return json(url('/api/tarefas', { status: filtro.status, situacaoPrazo: filtro.situacaoPrazo }))
}

export function consultarTarefa(id: string): Promise<Tarefa> {
  return json(url(`/api/tarefas/${id}`))
}

export function criarTarefa(dados: DadosTarefa): Promise<Tarefa> {
  return json(url('/api/tarefas'), comCorpo('POST', dados))
}

export function editarTarefa(id: string, dados: DadosTarefa, version: number): Promise<Tarefa> {
  return json(url(`/api/tarefas/${id}`), comCorpo('PUT', { ...dados, version }))
}

export function concluirTarefa(id: string): Promise<Tarefa> {
  return json(url(`/api/tarefas/${id}/concluir`), { method: 'POST' })
}

export function reabrirTarefa(id: string): Promise<Tarefa> {
  return json(url(`/api/tarefas/${id}/reabrir`), { method: 'POST' })
}

export async function excluirTarefa(id: string): Promise<void> {
  await requisitar(url(`/api/tarefas/${id}`), { method: 'DELETE' })
}

export function consultarScore(): Promise<Score> {
  return json(url('/api/produtividade/semana'))
}
