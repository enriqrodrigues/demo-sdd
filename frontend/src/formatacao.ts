import type { Prioridade, SituacaoPrazo } from './api'

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

/** `2026-10-20` → `20/10/2026`, sem passar por `Date` para não depender do fuso do navegador. */
export function formatarData(iso: string): string {
  const [ano, mes, dia] = iso.split('-')
  return `${dia}/${mes}/${ano}`
}
