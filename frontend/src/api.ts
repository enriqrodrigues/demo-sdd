export type Prioridade = 'BAIXA' | 'MEDIA' | 'ALTA'
export type StatusTarefa = 'PENDENTE' | 'CONCLUIDA'
export type SituacaoPrazo = 'ATRASADA' | 'PARA_HOJE' | 'NO_PRAZO' | 'CONCLUIDA'

export interface Tarefa {
  id: string
  titulo: string
  descricao: string | null
  dataVencimento: string
  prioridade: Prioridade
  status: StatusTarefa
  dataConclusao: string | null
  situacaoPrazo: SituacaoPrazo
}

/** Campos editáveis pelo formulário; vazios seguem como `null` para a API validar. */
export interface DadosTarefa {
  titulo: string
  descricao: string | null
  dataVencimento: string | null
  prioridade: Prioridade | null
}

export interface AtualizacaoTarefa extends DadosTarefa {
  status: StatusTarefa
}

export interface Score {
  inicioSemana: string
  fimSemana: string
  concluidasNoPrazo: number
  totalConcluidas: number
  percentual: number | null
}

/** Erro 400 com `errors`: mensagens da API indexadas pelo nome do campo. */
export class ErroValidacao extends Error {
  readonly erros: Record<string, string>

  constructor(mensagem: string, erros: Record<string, string>) {
    super(mensagem)
    this.name = 'ErroValidacao'
    this.erros = erros
  }
}

/** Qualquer outra falha: API fora do ar, resposta inesperada ou erro sem campos. */
export class ErroApi extends Error {
  constructor(mensagem: string) {
    super(mensagem)
    this.name = 'ErroApi'
  }
}

export const MENSAGEM_FALHA_COMUNICACAO =
  'Não foi possível se comunicar com o servidor. Verifique se a API está no ar e tente novamente.'

interface ProblemDetail {
  title?: string
  detail?: string
  errors?: { campo: string; mensagem: string }[]
}

const BASE = '/api'

async function requisitar<T>(caminho: string, init?: RequestInit): Promise<T> {
  let resposta: Response
  try {
    resposta = await fetch(BASE + caminho, {
      ...init,
      headers: init?.body ? { 'Content-Type': 'application/json' } : undefined,
    })
  } catch {
    throw new ErroApi(MENSAGEM_FALHA_COMUNICACAO)
  }

  if (!resposta.ok) {
    throw await converterErro(resposta)
  }
  if (resposta.status === 204) {
    return undefined as T
  }
  return (await resposta.json()) as T
}

async function converterErro(resposta: Response): Promise<Error> {
  const problema = await lerProblemDetail(resposta)
  if (!problema) {
    return new ErroApi(MENSAGEM_FALHA_COMUNICACAO)
  }
  if (problema.errors && problema.errors.length > 0) {
    const erros: Record<string, string> = {}
    for (const { campo, mensagem } of problema.errors) {
      erros[campo] ??= mensagem
    }
    return new ErroValidacao(problema.detail ?? 'Dados inválidos.', erros)
  }
  return new ErroApi(problema.detail ?? problema.title ?? MENSAGEM_FALHA_COMUNICACAO)
}

async function lerProblemDetail(resposta: Response): Promise<ProblemDetail | null> {
  const tipo = resposta.headers.get('Content-Type') ?? ''
  if (!tipo.includes('json')) {
    return null
  }
  try {
    return (await resposta.json()) as ProblemDetail
  } catch {
    return null
  }
}

export function listarTarefas(): Promise<Tarefa[]> {
  return requisitar('/tarefas')
}

export function criarTarefa(dados: DadosTarefa): Promise<Tarefa> {
  return requisitar('/tarefas', { method: 'POST', body: JSON.stringify(dados) })
}

export function atualizarTarefa(id: string, dados: AtualizacaoTarefa): Promise<Tarefa> {
  return requisitar(`/tarefas/${id}`, { method: 'PUT', body: JSON.stringify(dados) })
}

export function excluirTarefa(id: string): Promise<void> {
  return requisitar(`/tarefas/${id}`, { method: 'DELETE' })
}

export function buscarScore(): Promise<Score> {
  return requisitar('/produtividade/score')
}
