import type { Prioridade, Situacao } from './tipos';

export const ROTULOS_PRIORIDADE: Record<Prioridade, string> = {
  BAIXA: 'Baixa',
  MEDIA: 'Média',
  ALTA: 'Alta',
};

export const ROTULOS_SITUACAO: Record<Situacao, string> = {
  ATRASADA: 'Atrasada',
  PARA_HOJE: 'Para Hoje',
  NO_PRAZO: 'No Prazo',
  CONCLUIDA: 'Concluída',
};

/** 'YYYY-MM-DD' -> 'dd/mm/aaaa'. Não usa Date: new Date('2026-10-16') seria meia-noite UTC e viraria dia 15 no Brasil. */
export function formatarData(iso: string): string {
  const [ano, mes, dia] = iso.split('-');
  return `${dia}/${mes}/${ano}`;
}

export function formatarPercentual(valor: number): string {
  return valor.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
