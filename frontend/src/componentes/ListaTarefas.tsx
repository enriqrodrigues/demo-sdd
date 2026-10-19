import type { Tarefa } from '../api'
import { formatarData, ROTULO_PRIORIDADE, ROTULO_SITUACAO } from '../formatacao'

interface Props {
  tarefas: Tarefa[]
}

export function ListaTarefas({ tarefas }: Props) {
  if (tarefas.length === 0) {
    return <p className="lista-vazia">Nenhuma tarefa por aqui.</p>
  }
  return (
    <ul className="lista-tarefas">
      {tarefas.map((tarefa) => (
        <li key={tarefa.id} className={`tarefa situacao-${tarefa.situacaoPrazo.toLowerCase()}`}>
          <h3>{tarefa.titulo}</h3>
          <div className="tarefa-detalhes">
            <span className={`selo situacao-${tarefa.situacaoPrazo.toLowerCase()}`}>
              {ROTULO_SITUACAO[tarefa.situacaoPrazo]}
            </span>
            <span className={`prioridade prioridade-${tarefa.prioridade.toLowerCase()}`}>
              {ROTULO_PRIORIDADE[tarefa.prioridade]}
            </span>
            <time dateTime={tarefa.dataVencimento}>{formatarData(tarefa.dataVencimento)}</time>
          </div>
        </li>
      ))}
    </ul>
  )
}
