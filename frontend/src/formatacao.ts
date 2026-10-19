import type { Prioridade, SituacaoPrazo } from './api'

/** "2026-10-25" → "25/10/2026", sem passar por Date (que aplicaria o fuso do navegador). */
export function formatarData(iso: string): string {
  const [ano, mes, dia] = iso.split('-')
  return `${dia}/${mes}/${ano}`
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
