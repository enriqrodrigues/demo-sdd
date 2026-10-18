import type { Tarefa } from './api'
import { ROTULO_PRIORIDADE, ROTULO_SITUACAO, formatarData } from './formatacao'

interface Props {
  tarefas: Tarefa[]
  onAlternarStatus: (tarefa: Tarefa) => void
  onEditar: (tarefa: Tarefa) => void
  onExcluir: (tarefa: Tarefa) => void
}

export function ListaTarefas({ tarefas, onAlternarStatus, onEditar, onExcluir }: Props) {
  if (tarefas.length === 0) {
    return <p className="vazio">Nenhuma tarefa cadastrada.</p>
  }

  function confirmarExclusao(tarefa: Tarefa) {
    if (window.confirm(`Excluir a tarefa "${tarefa.titulo}"?`)) {
      onExcluir(tarefa)
    }
  }

  return (
    <ul className="lista-tarefas" aria-label="Tarefas">
      {tarefas.map((tarefa) => {
        const concluida = tarefa.status === 'CONCLUIDA'
        return (
          <li key={tarefa.id} className={concluida ? 'tarefa concluida' : 'tarefa'}>
            <input
              type="checkbox"
              checked={concluida}
              onChange={() => onAlternarStatus(tarefa)}
              aria-label={concluida ? `Reabrir ${tarefa.titulo}` : `Concluir ${tarefa.titulo}`}
            />
            <div className="tarefa-dados">
              <span className="tarefa-titulo">{tarefa.titulo}</span>
              <span className="tarefa-detalhes">
                <span className={`prioridade prioridade-${tarefa.prioridade.toLowerCase()}`}>
                  Prioridade {ROTULO_PRIORIDADE[tarefa.prioridade]}
                </span>
                <span>Vence em {formatarData(tarefa.dataVencimento)}</span>
              </span>
            </div>
            <span className={`selo selo-${tarefa.situacaoPrazo.toLowerCase()}`}>
              {ROTULO_SITUACAO[tarefa.situacaoPrazo]}
            </span>
            <div className="tarefa-acoes">
              <button type="button" onClick={() => onEditar(tarefa)} aria-label={`Editar ${tarefa.titulo}`}>
                Editar
              </button>
              <button
                type="button"
                className="perigo"
                onClick={() => confirmarExclusao(tarefa)}
                aria-label={`Excluir ${tarefa.titulo}`}
              >
                Excluir
              </button>
            </div>
          </li>
        )
      })}
    </ul>
  )
}
