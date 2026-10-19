import type { FiltroTarefas } from './api'

export type Filtro = 'TODAS' | 'PENDENTES' | 'ATRASADAS' | 'PARA_HOJE'

export const OPCOES_DE_FILTRO: { valor: Filtro; rotulo: string }[] = [
  { valor: 'TODAS', rotulo: 'Todas' },
  { valor: 'PENDENTES', rotulo: 'Pendentes' },
  { valor: 'ATRASADAS', rotulo: 'Atrasadas' },
  { valor: 'PARA_HOJE', rotulo: 'Para hoje' },
]

/** Tradução de cada filtro da tela para os parâmetros da listagem na API. */
export const PARAMETROS_DO_FILTRO: Record<Filtro, FiltroTarefas> = {
  TODAS: {},
  PENDENTES: { status: 'PENDENTE' },
  ATRASADAS: { situacaoPrazo: 'ATRASADA' },
  PARA_HOJE: { situacaoPrazo: 'PARA_HOJE' },
}
