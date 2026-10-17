import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { Tarefa } from '../tipos';
import { TarefaLista } from './TarefaLista';

const pendente: Tarefa = {
  id: '1',
  titulo: 'Pagar boleto',
  descricao: null,
  dataVencimento: '2026-10-16',
  prioridade: 'MEDIA',
  status: 'PENDENTE',
  dataConclusao: null,
  situacao: 'PARA_HOJE',
};

const concluida: Tarefa = { ...pendente, id: '2', status: 'CONCLUIDA', dataConclusao: '2026-10-15', situacao: 'CONCLUIDA' };

function renderizar(tarefas: Tarefa[]) {
  const callbacks = { onEditar: vi.fn(), onAlternarStatus: vi.fn(), onExcluir: vi.fn() };
  render(<TarefaLista tarefas={tarefas} {...callbacks} />);
  return callbacks;
}

describe('TarefaLista', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('mostra mensagem quando não há tarefas', () => {
    renderizar([]);

    expect(screen.getByText('Nenhuma tarefa cadastrada')).toBeInTheDocument();
  });

  it('mostra dados formatados e selo de situação', () => {
    renderizar([pendente]);

    expect(screen.getByText('Pagar boleto')).toBeInTheDocument();
    expect(screen.getByText('Média')).toBeInTheDocument();
    expect(screen.getByText('16/10/2026')).toBeInTheDocument();
    expect(screen.getByText('Para Hoje')).toHaveClass('selo-para_hoje');
  });

  it('oferece Concluir para pendente', async () => {
    const { onAlternarStatus } = renderizar([pendente]);

    await userEvent.click(screen.getByRole('button', { name: 'Concluir' }));

    expect(onAlternarStatus).toHaveBeenCalledWith(pendente);
  });

  it('oferece Reabrir para concluída', async () => {
    const { onAlternarStatus } = renderizar([concluida]);

    await userEvent.click(screen.getByRole('button', { name: 'Reabrir' }));

    expect(onAlternarStatus).toHaveBeenCalledWith(concluida);
  });

  it('chama onEditar com a tarefa', async () => {
    const { onEditar } = renderizar([pendente]);

    await userEvent.click(screen.getByRole('button', { name: 'Editar' }));

    expect(onEditar).toHaveBeenCalledWith(pendente);
  });

  it('exclui somente após confirmação', async () => {
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValueOnce(true);
    const { onExcluir } = renderizar([pendente]);

    await userEvent.click(screen.getByRole('button', { name: 'Excluir' }));
    expect(onExcluir).not.toHaveBeenCalled();

    await userEvent.click(screen.getByRole('button', { name: 'Excluir' }));
    expect(onExcluir).toHaveBeenCalledWith(pendente);
    expect(confirmar).toHaveBeenCalledWith('Excluir a tarefa "Pagar boleto"?');
  });
});
