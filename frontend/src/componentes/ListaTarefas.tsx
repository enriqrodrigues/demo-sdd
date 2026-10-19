import type { Tarefa } from '../api'
import { formatarData, ROTULO_PRIORIDADE, ROTULO_SITUACAO } from '../formatacao'

interface Props {
  tarefas: Tarefa[]
  /** Tarefa com o formulário de edição aberto: suas ações de concluir/reabrir e excluir ficam bloqueadas. */
  idEmEdicao?: string
  /** Tarefas com requisição em andamento: suas ações ficam bloqueadas até a resposta. */
  emOperacao?: ReadonlySet<string>
  aoEditar: (tarefa: Tarefa) => void
  aoAlternarConclusao?: (tarefa: Tarefa) => void
  aoExcluir?: (tarefa: Tarefa) => void
}

export function ListaTarefas({ tarefas, idEmEdicao, emOperacao, aoEditar, aoAlternarConclusao, aoExcluir }: Props) {
  if (tarefas.length === 0) {
    return <p className="lista-vazia">Nenhuma tarefa por aqui.</p>
  }
  return (
    <ul className="lista-tarefas">
      {tarefas.map((tarefa) => {
        const operando = emOperacao?.has(tarefa.id) ?? false
        const bloqueada = tarefa.id === idEmEdicao || operando
        const concluida = tarefa.status === 'CONCLUIDA'
        return (
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
            <div className="tarefa-acoes">
              <button type="button" disabled={bloqueada} onClick={() => aoAlternarConclusao?.(tarefa)}>
                {concluida ? 'Reabrir' : 'Concluir'}
              </button>
              {/* Editar durante a requisição abriria o formulário com uma versão prestes a mudar. */}
              <button type="button" disabled={operando} onClick={() => aoEditar(tarefa)}>
                Editar
              </button>
              <button type="button" disabled={bloqueada} onClick={() => aoExcluir?.(tarefa)}>
                Excluir
              </button>
            </div>
          </li>
        )
      })}
    </ul>
  )
}
