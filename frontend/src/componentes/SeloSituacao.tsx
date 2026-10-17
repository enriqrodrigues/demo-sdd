import { ROTULOS_SITUACAO } from '../formatacao';
import type { Situacao } from '../tipos';

export function SeloSituacao({ situacao }: { situacao: Situacao }) {
  return <span className={`selo selo-${situacao.toLowerCase()}`}>{ROTULOS_SITUACAO[situacao]}</span>;
}
