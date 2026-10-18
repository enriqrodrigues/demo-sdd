import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { Tarefa } from './api'
import { ListaTarefas } from './ListaTarefas'

function tarefa(parcial: Partial<Tarefa>): Tarefa {
  return {
    id: 'id',
    titulo: 'Tarefa',
    descricao: null,
    dataVencimento: '2026-10-20',
    prioridade: 'MEDIA',
    status: 'PENDENTE',
    dataConclusao: null,
    situacaoPrazo: 'NO_PRAZO',
    ...parcial,
  }
}

const pagarConta = tarefa({ id: '1', titulo: 'Pagar conta', dataVencimento: '2026-10-17', prioridade: 'ALTA', situacaoPrazo: 'ATRASADA' })
const ligarBanco = tarefa({ id: '2', titulo: 'Ligar para o banco', dataVencimento: '2026-10-18', prioridade: 'BAIXA', situacaoPrazo: 'PARA_HOJE' })
const comprarPao = tarefa({ id: '3', titulo: 'Comprar pão', status: 'CONCLUIDA', dataConclusao: '2026-10-18', situacaoPrazo: 'CONCLUIDA' })

function renderizar(tarefas: Tarefa[]) {
  const callbacks = { onAlternarStatus: vi.fn(), onEditar: vi.fn(), onExcluir: vi.fn() }
  const resultado = render(<ListaTarefas tarefas={tarefas} {...callbacks} />)
  return { ...callbacks, ...resultado }
}

describe('ListaTarefas', () => {
  it('exibe cada tarefa com título, prioridade, vencimento e selo, na ordem recebida', () => {
    renderizar([pagarConta, ligarBanco, comprarPao])

    const itens = within(screen.getByRole('list', { name: 'Tarefas' })).getAllByRole('listitem')
    expect(itens).toHaveLength(3)

    expect(itens[0]).toHaveTextContent('Pagar conta')
    expect(itens[0]).toHaveTextContent('Prioridade Alta')
    expect(itens[0]).toHaveTextContent('Vence em 17/10/2026')
    expect(itens[0]).toHaveTextContent('Atrasada')

    expect(itens[1]).toHaveTextContent('Ligar para o banco')
    expect(itens[1]).toHaveTextContent('Prioridade Baixa')
    expect(itens[1]).toHaveTextContent('Para hoje')

    expect(itens[2]).toHaveTextContent('Comprar pão')
    expect(itens[2]).toHaveTextContent('Prioridade Média')
    expect(itens[2]).toHaveTextContent('Concluída')
  })

  it('informa quando não há tarefas cadastradas', () => {
    renderizar([])

    expect(screen.getByText('Nenhuma tarefa cadastrada.')).toBeInTheDocument()
    expect(screen.queryByRole('list')).not.toBeInTheDocument()
  })

  it('marcar uma tarefa pendente pede a conclusão', async () => {
    const { onAlternarStatus, rerender } = renderizar([pagarConta])

    const controle = screen.getByRole('checkbox', { name: 'Concluir Pagar conta' })
    expect(controle).not.toBeChecked()
    await userEvent.click(controle)

    expect(onAlternarStatus).toHaveBeenCalledWith(pagarConta)

    // depois da recarga, a tarefa vem concluída da API
    rerender(
      <ListaTarefas
        tarefas={[{ ...pagarConta, status: 'CONCLUIDA', dataConclusao: '2026-10-18', situacaoPrazo: 'CONCLUIDA' }]}
        onAlternarStatus={onAlternarStatus}
        onEditar={vi.fn()}
        onExcluir={vi.fn()}
      />,
    )
    expect(screen.getByRole('checkbox', { name: 'Reabrir Pagar conta' })).toBeChecked()
    expect(screen.getByText('Concluída')).toBeInTheDocument()
  })

  it('desmarcar uma tarefa concluída pede a reabertura', async () => {
    const { onAlternarStatus } = renderizar([comprarPao])

    const controle = screen.getByRole('checkbox', { name: 'Reabrir Comprar pão' })
    expect(controle).toBeChecked()
    await userEvent.click(controle)

    expect(onAlternarStatus).toHaveBeenCalledWith(comprarPao)
  })

  it('exclui após o usuário confirmar', async () => {
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true)
    const { onExcluir } = renderizar([pagarConta])

    await userEvent.click(screen.getByRole('button', { name: 'Excluir Pagar conta' }))

    expect(confirmar).toHaveBeenCalledWith('Excluir a tarefa "Pagar conta"?')
    expect(onExcluir).toHaveBeenCalledWith(pagarConta)
  })

  it('não exclui quando o usuário cancela a confirmação', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    const { onExcluir } = renderizar([pagarConta])

    await userEvent.click(screen.getByRole('button', { name: 'Excluir Pagar conta' }))

    expect(onExcluir).not.toHaveBeenCalled()
    expect(screen.getByText('Pagar conta')).toBeInTheDocument()
  })

  it('pede a edição da tarefa escolhida', async () => {
    const { onEditar } = renderizar([pagarConta, ligarBanco])

    await userEvent.click(screen.getByRole('button', { name: 'Editar Ligar para o banco' }))

    expect(onEditar).toHaveBeenCalledWith(ligarBanco)
  })
})
