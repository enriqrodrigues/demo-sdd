import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { Situacao } from '../tipos';
import { SeloSituacao } from './SeloSituacao';

describe('SeloSituacao', () => {
  it.each<[Situacao, string, string]>([
    ['ATRASADA', 'Atrasada', 'selo-atrasada'],
    ['PARA_HOJE', 'Para Hoje', 'selo-para_hoje'],
    ['NO_PRAZO', 'No Prazo', 'selo-no_prazo'],
    ['CONCLUIDA', 'Concluída', 'selo-concluida'],
  ])('mostra %s como "%s" com a classe %s', (situacao, texto, classe) => {
    render(<SeloSituacao situacao={situacao} />);

    const selo = screen.getByText(texto);
    expect(selo).toHaveClass('selo', classe);
  });
});
