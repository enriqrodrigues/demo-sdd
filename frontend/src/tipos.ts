export type Prioridade = 'BAIXA' | 'MEDIA' | 'ALTA';
export type StatusTarefa = 'PENDENTE' | 'CONCLUIDA';
export type Situacao = 'ATRASADA' | 'PARA_HOJE' | 'NO_PRAZO' | 'CONCLUIDA';

export interface Tarefa {
  id: string;
  titulo: string;
  descricao: string | null;
  dataVencimento: string; // YYYY-MM-DD
  prioridade: Prioridade;
  status: StatusTarefa;
  dataConclusao: string | null;
  situacao: Situacao;
}

export interface TarefaInput {
  titulo: string;
  descricao: string | null;
  dataVencimento: string;
  prioridade: Prioridade;
  status: StatusTarefa;
}

export interface ScoreSemanal {
  inicioSemana: string;
  fimSemana: string;
  totalConcluidas: number;
  concluidasNoPrazo: number;
  percentualNoPrazo: number | null;
}

export interface ErroCampo {
  campo: string;
  mensagem: string;
}
