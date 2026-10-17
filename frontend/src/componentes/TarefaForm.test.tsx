import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ErroApi } from '../api/tarefasApi';
import type { Tarefa } from '../tipos';
import { TarefaForm } from './TarefaForm';

const tarefaConcluida: Tarefa = {
  id: 't1',
  titulo: 'Pagar boleto',
  descricao: 'Luz',
  dataVencimento: '2026-10-20',
  prioridade: 'ALTA',
  status: 'CONCLUIDA',
  dataConclusao: '2026-10-14',
  situacao: 'CONCLUIDA',
};

function preencherVencimento(valor: string) {
  fireEvent.change(screen.getByLabelText('Vencimento'), { target: { value: valor } });
}

describe('TarefaForm', () => {
  it('mostra erros e não envia quando os campos obrigatórios estão vazios', async () => {
    const onSalvar = vi.fn();
    render(<TarefaForm tarefaEmEdicao={null} onSalvar={onSalvar} onCancelar={vi.fn()} />);

    await userEvent.click(screen.getByRole('button', { name: 'Adicionar tarefa' }));

    expect(screen.getByText('Título é obrigatório')).toBeInTheDocument();
    expect(screen.getByText('Data de vencimento é obrigatória')).toBeInTheDocument();
    expect(onSalvar).not.toHaveBeenCalled();
  });

  it('rejeita título com mais de 100 caracteres', async () => {
    const onSalvar = vi.fn();
    render(<TarefaForm tarefaEmEdicao={null} onSalvar={onSalvar} onCancelar={vi.fn()} />);
    fireEvent.change(screen.getByLabelText('Título'), { target: { value: 'a'.repeat(101) } });
    preencherVencimento('2026-10-20');

    await userEvent.click(screen.getByRole('button', { name: 'Adicionar tarefa' }));

    expect(screen.getByText('Título deve ter no máximo 100 caracteres')).toBeInTheDocument();
    expect(onSalvar).not.toHaveBeenCalled();
  });

  it('envia nova tarefa normalizada e limpa o formulário', async () => {
    const onSalvar = vi.fn().mockResolvedValue(undefined);
    render(<TarefaForm tarefaEmEdicao={null} onSalvar={onSalvar} onCancelar={vi.fn()} />);
    await userEvent.type(screen.getByLabelText('Título'), '  Estudar  ');
    await userEvent.type(screen.getByLabelText('Descrição'), '   ');
    preencherVencimento('2026-10-20');
    await userEvent.selectOptions(screen.getByLabelText('Prioridade'), 'ALTA');

    await userEvent.click(screen.getByRole('button', { name: 'Adicionar tarefa' }));

    expect(onSalvar).toHaveBeenCalledWith({
      titulo: 'Estudar',
      descricao: null,
      dataVencimento: '2026-10-20',
      prioridade: 'ALTA',
      status: 'PENDENTE',
    });
    await waitFor(() => expect(screen.getByLabelText('Título')).toHaveValue(''));
    expect(screen.getByLabelText('Prioridade')).toHaveValue('MEDIA');
  });

  it('preenche os campos ao editar e preserva o status', async () => {
    const onSalvar = vi.fn().mockResolvedValue(undefined);
    const onCancelar = vi.fn();
    render(<TarefaForm tarefaEmEdicao={tarefaConcluida} onSalvar={onSalvar} onCancelar={onCancelar} />);

    expect(screen.getByLabelText('Título')).toHaveValue('Pagar boleto');
    expect(screen.getByLabelText('Descrição')).toHaveValue('Luz');
    expect(screen.getByLabelText('Vencimento')).toHaveValue('2026-10-20');
    expect(screen.getByLabelText('Prioridade')).toHaveValue('ALTA');

    await userEvent.click(screen.getByRole('button', { name: 'Salvar alterações' }));
    expect(onSalvar).toHaveBeenCalledWith({
      titulo: 'Pagar boleto',
      descricao: 'Luz',
      dataVencimento: '2026-10-20',
      prioridade: 'ALTA',
      status: 'CONCLUIDA',
    });

    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }));
    expect(onCancelar).toHaveBeenCalled();
  });

  it('não mostra Cancelar ao criar', () => {
    render(<TarefaForm tarefaEmEdicao={null} onSalvar={vi.fn()} onCancelar={vi.fn()} />);

    expect(screen.queryByRole('button', { name: 'Cancelar' })).not.toBeInTheDocument();
  });

  it('mostra erros de campo retornados pelo backend', async () => {
    const onSalvar = vi.fn().mockRejectedValue(
      new ErroApi(400, 'Um ou mais campos são inválidos.', [
        { campo: 'titulo', mensagem: 'deve ter no máximo 100 caracteres' },
      ]),
    );
    render(<TarefaForm tarefaEmEdicao={null} onSalvar={onSalvar} onCancelar={vi.fn()} />);
    await userEvent.type(screen.getByLabelText('Título'), 'Estudar');
    preencherVencimento('2026-10-20');

    await userEvent.click(screen.getByRole('button', { name: 'Adicionar tarefa' }));

    expect(await screen.findByText('deve ter no máximo 100 caracteres')).toBeInTheDocument();
    expect(screen.getByLabelText('Título')).toHaveValue('Estudar');
  });

  it('mostra erro geral quando o backend falha sem erros de campo', async () => {
    const onSalvar = vi.fn().mockRejectedValue(new ErroApi(500, 'Ocorreu um erro inesperado.'));
    render(<TarefaForm tarefaEmEdicao={null} onSalvar={onSalvar} onCancelar={vi.fn()} />);
    await userEvent.type(screen.getByLabelText('Título'), 'Estudar');
    preencherVencimento('2026-10-20');

    await userEvent.click(screen.getByRole('button', { name: 'Adicionar tarefa' }));

    expect(await screen.findByText('Ocorreu um erro inesperado.')).toBeInTheDocument();
  });
});
