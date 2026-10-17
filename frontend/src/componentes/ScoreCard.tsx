import { formatarData, formatarPercentual } from '../formatacao';
import type { ScoreSemanal } from '../tipos';

export function ScoreCard({ score }: { score: ScoreSemanal }) {
  return (
    <section className="score-card" aria-label="Score semanal">
      <h2>Produtividade da semana</h2>
      {score.percentualNoPrazo === null ? (
        <p>Nenhuma tarefa concluída nesta semana</p>
      ) : (
        <p>{`${formatarPercentual(score.percentualNoPrazo)}% no prazo (${score.concluidasNoPrazo} de ${score.totalConcluidas})`}</p>
      )}
      <p className="score-semana">
        {`Semana de ${formatarData(score.inicioSemana)} a ${formatarData(score.fimSemana)}`}
      </p>
    </section>
  );
}
