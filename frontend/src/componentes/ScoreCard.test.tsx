import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { ScoreSemanal } from '../tipos';
import { ScoreCard } from './ScoreCard';

const semana = { inicioSemana: '2026-10-12', fimSemana: '2026-10-18' };

describe('ScoreCard', () => {
  it('mostra percentual formatado, contagem e semana', () => {
    const score: ScoreSemanal = { ...semana, totalConcluidas: 5, concluidasNoPrazo: 4, percentualNoPrazo: 80 };

    render(<ScoreCard score={score} />);

    expect(screen.getByText('80,00% no prazo (4 de 5)')).toBeInTheDocument();
    expect(screen.getByText('Semana de 12/10/2026 a 18/10/2026')).toBeInTheDocument();
  });

  it('mostra mensagem quando não houve conclusões na semana', () => {
    const score: ScoreSemanal = { ...semana, totalConcluidas: 0, concluidasNoPrazo: 0, percentualNoPrazo: null };

    render(<ScoreCard score={score} />);

    expect(screen.getByText('Nenhuma tarefa concluída nesta semana')).toBeInTheDocument();
  });
});
