import type { Score } from '../api'
import { formatarDiaMes, formatarPercentual } from '../formatacao'

interface Props {
  score: Score | null
  /** A consulta do score falhou e não há valor anterior para mostrar. */
  indisponivel?: boolean
}

export function CabecalhoScore({ score, indisponivel = false }: Props) {
  return (
    <section className="score" aria-label="Score da semana">
      {score === null ? (
        <span className="score-carregando">{indisponivel ? 'Score indisponível' : 'Carregando score…'}</span>
      ) : (
        <>
          {score.percentual === null ? (
            <strong className="score-vazio">Sem tarefas finalizadas esta semana</strong>
          ) : (
            <>
              <strong className="score-percentual">{formatarPercentual(score.percentual)}</strong>
              <span>
                {score.noPrazo} de {score.concluidas} no prazo
              </span>
            </>
          )}
          <span className="score-periodo">
            {formatarDiaMes(score.inicioSemana)} a {formatarDiaMes(score.fimSemana)}
          </span>
        </>
      )}
    </section>
  )
}
