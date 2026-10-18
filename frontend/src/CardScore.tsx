import type { Score } from './api'
import { formatarData } from './formatacao'

const formatoPercentual = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 })

export function CardScore({ score }: { score: Score }) {
  return (
    <section className="card-score" aria-label="Score da semana">
      <h2>Score da semana</h2>
      <p className="periodo">
        {formatarData(score.inicioSemana)} a {formatarData(score.fimSemana)}
      </p>
      {score.percentual === null ? (
        <p className="sem-dados">Nenhuma tarefa concluída nesta semana.</p>
      ) : (
        <p className="resultado">
          <strong className="percentual">{formatoPercentual.format(score.percentual)}%</strong>
          <span className="contagem">
            {score.concluidasNoPrazo} de {score.totalConcluidas} no prazo
          </span>
        </p>
      )}
    </section>
  )
}
