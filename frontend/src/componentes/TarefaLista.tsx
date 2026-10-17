import { formatarData, ROTULOS_PRIORIDADE } from '../formatacao';
import type { Tarefa } from '../tipos';
import { SeloSituacao } from './SeloSituacao';

interface Props {
  tarefas: Tarefa[];
  onEditar: (tarefa: Tarefa) => void;
  onAlternarStatus: (tarefa: Tarefa) => void;
  onExcluir: (tarefa: Tarefa) => void;
}

export function TarefaLista({ tarefas, onEditar, onAlternarStatus, onExcluir }: Props) {
  if (tarefas.length === 0) {
    return <p className="lista-vazia">Nenhuma tarefa cadastrada</p>;
  }

  function confirmarExclusao(tarefa: Tarefa) {
    if (window.confirm(`Excluir a tarefa "${tarefa.titulo}"?`)) {
      onExcluir(tarefa);
    }
  }

  return (
    <table>
      <thead>
        <tr>
          <th>Título</th>
          <th>Prioridade</th>
          <th>Vencimento</th>
          <th>Situação</th>
          <th>Ações</th>
        </tr>
      </thead>
      <tbody>
        {tarefas.map((tarefa) => (
          <tr key={tarefa.id}>
            <td>{tarefa.titulo}</td>
            <td>{ROTULOS_PRIORIDADE[tarefa.prioridade]}</td>
            <td>{formatarData(tarefa.dataVencimento)}</td>
            <td>
              <SeloSituacao situacao={tarefa.situacao} />
            </td>
            <td className="acoes">
              <button type="button" onClick={() => onEditar(tarefa)}>
                Editar
              </button>
              <button type="button" onClick={() => onAlternarStatus(tarefa)}>
                {tarefa.status === 'PENDENTE' ? 'Concluir' : 'Reabrir'}
              </button>
              <button type="button" onClick={() => confirmarExclusao(tarefa)}>
                Excluir
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
