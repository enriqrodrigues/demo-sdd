import type { ErroCampo, ScoreSemanal, Tarefa, TarefaInput } from '../tipos';

/** Erro da API já convertido do problem+json. status 0 = falha de rede. */
export class ErroApi extends Error {
  readonly status: number;
  readonly detail: string;
  readonly erros: ErroCampo[];

  constructor(status: number, detail: string, erros: ErroCampo[] = []) {
    super(detail);
    this.name = 'ErroApi';
    this.status = status;
    this.detail = detail;
    this.erros = erros;
  }
}

const BASE = '/api';

async function converterErro(resposta: Response): Promise<ErroApi> {
  const generico = `Erro ${resposta.status}`;
  try {
    const corpo = await resposta.json();
    return new ErroApi(
      resposta.status,
      typeof corpo.detail === 'string' ? corpo.detail : generico,
      Array.isArray(corpo.erros) ? corpo.erros : [],
    );
  } catch {
    return new ErroApi(resposta.status, generico);
  }
}

async function requisitar<T>(caminho: string, init: RequestInit = {}): Promise<T> {
  const opcoes: RequestInit = init.body
    ? { ...init, headers: { 'Content-Type': 'application/json' } }
    : init;
  let resposta: Response;
  try {
    resposta = await fetch(BASE + caminho, opcoes);
  } catch {
    throw new ErroApi(0, 'Não foi possível conectar ao servidor.');
  }
  if (!resposta.ok) {
    throw await converterErro(resposta);
  }
  if (resposta.status === 204) {
    return undefined as T;
  }
  return (await resposta.json()) as T;
}

export const tarefasApi = {
  listar: () => requisitar<Tarefa[]>('/tarefas'),
  criar: (dados: TarefaInput) =>
    requisitar<Tarefa>('/tarefas', { method: 'POST', body: JSON.stringify(dados) }),
  atualizar: (id: string, dados: TarefaInput) =>
    requisitar<Tarefa>(`/tarefas/${id}`, { method: 'PUT', body: JSON.stringify(dados) }),
  excluir: (id: string) => requisitar<void>(`/tarefas/${id}`, { method: 'DELETE' }),
  scoreSemanal: () => requisitar<ScoreSemanal>('/produtividade/score-semanal'),
};
