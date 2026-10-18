import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from './api'
import { ErroApi, MENSAGEM_FALHA_COMUNICACAO, type Score, type Tarefa } from './api'
import App from './App'

vi.mock('./api', async (importOriginal) => {
  const original = await importOriginal<typeof import('./api')>()
  return {
    ...original,
    listarTarefas: vi.fn(),
    buscarScore: vi.fn(),
    criarTarefa: vi.fn(),
    atualizarTarefa: vi.fn(),
    excluirTarefa: vi.fn(),
  }
})

const listarTarefas = vi.mocked(api.listarTarefas)
const buscarScore = vi.mocked(api.buscarScore)
const criarTarefa = vi.mocked(api.criarTarefa)
const atualizarTarefa = vi.mocked(api.atualizarTarefa)
const excluirTarefa = vi.mocked(api.excluirTarefa)

const pagarConta: Tarefa = {
  id: '1',
  titulo: 'Pagar conta',
  descricao: 'Luz',
  dataVencimento: '2026-10-20',
  prioridade: 'ALTA',
  status: 'PENDENTE',
  dataConclusao: null,
  situacaoPrazo: 'NO_PRAZO',
}
const pagarContaConcluida: Tarefa = {
  ...pagarConta,
  status: 'CONCLUIDA',
  dataConclusao: '2026-10-18',
  situacaoPrazo: 'CONCLUIDA',
}

function score(concluidasNoPrazo: number, totalConcluidas: number, percentual: number | null): Score {
  return { inicioSemana: '2026-10-12', fimSemana: '2026-10-18', concluidasNoPrazo, totalConcluidas, percentual }
}

function cardScore() {
  return screen.getByRole('region', { name: 'Score da semana' })
}

beforeEach(() => {
  vi.resetAllMocks()
})

describe('App', () => {
  it('carrega a lista e o score com dados', async () => {
    listarTarefas.mockResolvedValue([pagarConta])
    buscarScore.mockResolvedValue(score(3, 4, 75))

    render(<App />)

    expect(await screen.findByText('Pagar conta')).toBeInTheDocument()
    expect(cardScore()).toHaveTextContent('75%')
    expect(cardScore()).toHaveTextContent('3 de 4 no prazo')
  })

  it('mostra o score sem dados', async () => {
    listarTarefas.mockResolvedValue([])
    buscarScore.mockResolvedValue(score(0, 0, null))

    render(<App />)

    expect(await screen.findByText('Nenhuma tarefa concluída nesta semana.')).toBeInTheDocument()
    expect(screen.getByText('Nenhuma tarefa cadastrada.')).toBeInTheDocument()
  })

  it('mostra mensagem de erro em vez de lista vazia quando a API está indisponível', async () => {
    listarTarefas.mockRejectedValue(new ErroApi(MENSAGEM_FALHA_COMUNICACAO))
    buscarScore.mockRejectedValue(new ErroApi(MENSAGEM_FALHA_COMUNICACAO))

    render(<App />)

    expect(await screen.findByRole('alert')).toHaveTextContent(MENSAGEM_FALHA_COMUNICACAO)
    expect(screen.queryByText('Nenhuma tarefa cadastrada.')).not.toBeInTheDocument()
    expect(screen.queryByText('Carregando…')).not.toBeInTheDocument()
  })

  it('cria uma tarefa e recarrega a lista e o score', async () => {
    listarTarefas.mockResolvedValueOnce([]).mockResolvedValueOnce([pagarConta])
    buscarScore.mockResolvedValue(score(0, 0, null))
    criarTarefa.mockResolvedValue(pagarConta)
    render(<App />)
    await screen.findByText('Nenhuma tarefa cadastrada.')

    await userEvent.type(screen.getByLabelText('Título'), 'Pagar conta')
    await userEvent.type(screen.getByLabelText('Vencimento'), '2026-10-20')
    await userEvent.selectOptions(screen.getByLabelText('Prioridade'), 'ALTA')
    await userEvent.click(screen.getByRole('button', { name: 'Adicionar tarefa' }))

    expect(criarTarefa).toHaveBeenCalledWith({
      titulo: 'Pagar conta',
      descricao: null,
      dataVencimento: '2026-10-20',
      prioridade: 'ALTA',
    })
    expect(await screen.findByText('Pagar conta')).toBeInTheDocument()
    expect(buscarScore).toHaveBeenCalledTimes(2)
    expect(screen.getByLabelText('Título')).toHaveValue('')
  })

  it('edita uma tarefa mantendo o status e mostra os novos valores', async () => {
    const editada = { ...pagarConta, titulo: 'Pagar conta de luz' }
    listarTarefas.mockResolvedValueOnce([pagarConta]).mockResolvedValueOnce([editada])
    buscarScore.mockResolvedValue(score(0, 0, null))
    atualizarTarefa.mockResolvedValue(editada)
    render(<App />)

    await userEvent.click(await screen.findByRole('button', { name: 'Editar Pagar conta' }))
    const titulo = screen.getByLabelText('Título')
    await userEvent.clear(titulo)
    await userEvent.type(titulo, 'Pagar conta de luz')
    await userEvent.click(screen.getByRole('button', { name: 'Salvar alterações' }))

    expect(atualizarTarefa).toHaveBeenCalledWith('1', {
      titulo: 'Pagar conta de luz',
      descricao: 'Luz',
      dataVencimento: '2026-10-20',
      prioridade: 'ALTA',
      status: 'PENDENTE',
    })
    const lista = screen.getByRole('list', { name: 'Tarefas' })
    expect(await within(lista).findByText('Pagar conta de luz')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Nova tarefa' })).toBeInTheDocument()
  })

  it('concluir uma tarefa mostra o selo de concluída e atualiza o score', async () => {
    listarTarefas.mockResolvedValueOnce([pagarConta]).mockResolvedValueOnce([pagarContaConcluida])
    buscarScore.mockResolvedValueOnce(score(0, 0, null)).mockResolvedValueOnce(score(1, 1, 100))
    atualizarTarefa.mockResolvedValue(pagarContaConcluida)
    render(<App />)

    await userEvent.click(await screen.findByRole('checkbox', { name: 'Concluir Pagar conta' }))

    expect(atualizarTarefa).toHaveBeenCalledWith('1', expect.objectContaining({ status: 'CONCLUIDA' }))
    expect(await screen.findByRole('checkbox', { name: 'Reabrir Pagar conta' })).toBeChecked()
    expect(screen.getByText('Concluída')).toBeInTheDocument()
    expect(cardScore()).toHaveTextContent('100%')
    expect(cardScore()).toHaveTextContent('1 de 1 no prazo')
  })

  it('reabrir uma tarefa volta a mostrar a situação de pendente e atualiza o score', async () => {
    listarTarefas.mockResolvedValueOnce([pagarContaConcluida]).mockResolvedValueOnce([pagarConta])
    buscarScore.mockResolvedValueOnce(score(1, 1, 100)).mockResolvedValueOnce(score(0, 0, null))
    atualizarTarefa.mockResolvedValue(pagarConta)
    render(<App />)

    await userEvent.click(await screen.findByRole('checkbox', { name: 'Reabrir Pagar conta' }))

    expect(atualizarTarefa).toHaveBeenCalledWith('1', expect.objectContaining({ status: 'PENDENTE' }))
    expect(await screen.findByText('No prazo')).toBeInTheDocument()
    expect(cardScore()).toHaveTextContent('Nenhuma tarefa concluída nesta semana.')
  })

  it('excluir com confirmação remove a tarefa e atualiza o score', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    listarTarefas.mockResolvedValueOnce([pagarContaConcluida]).mockResolvedValueOnce([])
    buscarScore.mockResolvedValueOnce(score(1, 1, 100)).mockResolvedValueOnce(score(0, 0, null))
    excluirTarefa.mockResolvedValue(undefined)
    render(<App />)

    await userEvent.click(await screen.findByRole('button', { name: 'Excluir Pagar conta' }))

    expect(excluirTarefa).toHaveBeenCalledWith('1')
    expect(await screen.findByText('Nenhuma tarefa cadastrada.')).toBeInTheDocument()
    expect(cardScore()).toHaveTextContent('Nenhuma tarefa concluída nesta semana.')
  })

  it('mostra mensagem de erro quando uma alteração falha', async () => {
    listarTarefas.mockResolvedValue([pagarConta])
    buscarScore.mockResolvedValue(score(0, 0, null))
    atualizarTarefa.mockRejectedValue(new ErroApi('Não existe tarefa com o id 1.'))
    render(<App />)

    await userEvent.click(await screen.findByRole('checkbox', { name: 'Concluir Pagar conta' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Não existe tarefa com o id 1.')
    expect(screen.getByText('Pagar conta')).toBeInTheDocument()
  })
})
