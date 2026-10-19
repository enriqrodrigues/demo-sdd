import type { Prioridade, SituacaoPrazo } from './api'

/** "2026-10-25" → "25/10/2026", sem passar por Date (que aplicaria o fuso do navegador). */
export function formatarData(iso: string): string {
  const [ano, mes, dia] = iso.split('-')
  return `${dia}/${mes}/${ano}`
}

/** "2026-10-19" → "19/10". */
export function formatarDiaMes(iso: string): string {
  const [, mes, dia] = iso.split('-')
  return `${dia}/${mes}`
}

const PERCENTUAL = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 })

/** 66.67 → "66,67%"; 100 → "100%". */
export function formatarPercentual(valor: number): string {
  return `${PERCENTUAL.format(valor)}%`
}

export const ROTULO_PRIORIDADE: Record<Prioridade, string> = {
  ALTA: 'Alta',
  MEDIA: 'Média',
  BAIXA: 'Baixa',
}

export const ROTULO_SITUACAO: Record<SituacaoPrazo, string> = {
  ATRASADA: 'Atrasada',
  PARA_HOJE: 'Para hoje',
  NO_PRAZO: 'No prazo',
  CONCLUIDA: 'Concluída',
}
