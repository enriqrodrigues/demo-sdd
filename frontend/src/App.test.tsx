import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { App } from './App';
import { ErroApi, tarefasApi } from './api/tarefasApi';
import type { ScoreSemanal, Tarefa } from './tipos';

vi.mock('./api/tarefasApi', async (importOriginal) => {
  const original = await importOriginal<typeof import('./api/tarefasApi')>();
  return {
    ...original,
    tarefasApi: {
      listar: vi.fn(),
      criar: vi.fn(),
      atualizar: vi.fn(),
      excluir: vi.fn(),
      scoreSemanal: vi.fn(),
    },
  };
});

const api = vi.mocked(tarefasApi);

const pendente: Tarefa = {
  id: '1',
  titulo: 'Pagar boleto',
  descricao: null,
  dataVencimento: '2026-10-16',
  prioridade: 'ALTA',
  status: 'PENDENTE',
  dataConclusao: null,
  situacao: 'PARA_HOJE',
};

const scoreVazio: ScoreSemanal = {
  inicioSemana: '2026-10-12',
  fimSemana: '2026-10-18',
  totalConcluidas: 0,
  concluidasNoPrazo: 0,
  percentualNoPrazo: null,
};

describe('App', () => {
  beforeEach(() => {
    api.listar.mockResolvedValue([pendente]);
    api.scoreSemanal.mockResolvedValue(scoreVazio);
    api.criar.mockResolvedValue(pendente);
    api.atualizar.mockResolvedValue({ ...pendente, status: 'CONCLUIDA' });
    api.excluir.mockResolvedValue(undefined);
  });

  afterEach(() => {
    vi.clearAllMocks();
    vi.restoreAllMocks();
  });

  it('mostra carregando e depois tarefas e score', async () => {
    render(<App />);

    expect(screen.getByText('Carregando…')).toBeInTheDocument();
    expect(await screen.findByText('Pagar boleto')).toBeInTheDocument();
    expect(screen.getByText('Nenhuma tarefa concluída nesta semana')).toBeInTheDocument();
  });

  it('conclui tarefa e recarrega lista e score', async () => {
    render(<App />);
    await screen.findByText('Pagar boleto');

    await userEvent.click(screen.getByRole('button', { name: 'Concluir' }));

    expect(api.atualizar).toHaveBeenCalledWith('1', {
      titulo: 'Pagar boleto',
      descricao: null,
      dataVencimento: '2026-10-16',
      prioridade: 'ALTA',
      status: 'CONCLUIDA',
    });
    await waitFor(() => expect(api.listar).toHaveBeenCalledTimes(2));
    expect(api.scoreSemanal).toHaveBeenCalledTimes(2);
  });

  it('exclui tarefa após confirmar e recarrega', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    render(<App />);
    await screen.findByText('Pagar boleto');

    await userEvent.click(screen.getByRole('button', { name: 'Excluir' }));

    expect(api.excluir).toHaveBeenCalledWith('1');
    await waitFor(() => expect(api.listar).toHaveBeenCalledTimes(2));
  });

  it('cria tarefa pelo formulário e recarrega', async () => {
    render(<App />);
    await screen.findByText('Pagar boleto');

    await userEvent.type(screen.getByLabelText('Título'), 'Estudar');
    fireEvent.change(screen.getByLabelText('Vencimento'), { target: { value: '2026-10-20' } });
    await userEvent.click(screen.getByRole('button', { name: 'Adicionar tarefa' }));

    expect(api.criar).toHaveBeenCalledWith({
      titulo: 'Estudar',
      descricao: null,
      dataVencimento: '2026-10-20',
      prioridade: 'MEDIA',
      status: 'PENDENTE',
    });
    await waitFor(() => expect(api.listar).toHaveBeenCalledTimes(2));
  });

  it('edita tarefa existente', async () => {
    api.atualizar.mockResolvedValue(pendente);
    render(<App />);
    await screen.findByText('Pagar boleto');

    await userEvent.click(screen.getByRole('button', { name: 'Editar' }));
    expect(screen.getByLabelText('Título')).toHaveValue('Pagar boleto');
    await userEvent.click(screen.getByRole('button', { name: 'Salvar alterações' }));

    expect(api.atualizar).toHaveBeenCalledWith('1', expect.objectContaining({ status: 'PENDENTE' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Adicionar tarefa' })).toBeInTheDocument());
  });

  it('mantém o formulário sincronizado quando a tarefa em edição é concluída', async () => {
    render(<App />);
    await screen.findByText('Pagar boleto');

    await userEvent.click(screen.getByRole('button', { name: 'Editar' }));
    await userEvent.click(screen.getByRole('button', { name: 'Concluir' }));
    await waitFor(() => expect(api.listar).toHaveBeenCalledTimes(2));
    await userEvent.click(screen.getByRole('button', { name: 'Salvar alterações' }));

    expect(api.atualizar).toHaveBeenLastCalledWith('1', expect.objectContaining({ status: 'CONCLUIDA' }));
  });

  it('mostra banner quando o servidor não responde', async () => {
    api.listar.mockRejectedValue(new ErroApi(0, 'Não foi possível conectar ao servidor.'));
    render(<App />);

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível conectar ao servidor.');
    expect(screen.queryByText('Carregando…')).not.toBeInTheDocument();
  });
});
