import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import App from '../App'
import { apiEmMemoria, umaTarefa } from '../test/api-simulada'

const linhaDa = async (titulo: string) =>
  (await screen.findByRole('heading', { name: titulo })).closest('li') as HTMLElement

describe('Concluir e reabrir pela interface', () => {
  it('concluir uma tarefa pendente mostra a situação "Concluída"', async () => {
    apiEmMemoria([umaTarefa({ titulo: 'Pagar conta', dataVencimento: '2026-10-25' })])
    render(<App />)
    const linha = await linhaDa('Pagar conta')

    await userEvent.click(within(linha).getByRole('button', { name: 'Concluir' }))

    expect(await within(await linhaDa('Pagar conta')).findByText('Concluída')).toBeInTheDocument()
    expect(within(await linhaDa('Pagar conta')).getByRole('button', { name: 'Reabrir' })).toBeEnabled()
  })

  it('reabrir uma tarefa concluída com vencimento passado mostra "Atrasada"', async () => {
    apiEmMemoria([umaTarefa({ titulo: 'Pagar conta', dataVencimento: '2026-10-20', status: 'CONCLUIDA' })])
    render(<App />)
    const linha = await linhaDa('Pagar conta')

    await userEvent.click(within(linha).getByRole('button', { name: 'Reabrir' }))

    expect(await within(await linhaDa('Pagar conta')).findByText('Atrasada')).toBeInTheDocument()
  })

  it('duplo clique em concluir envia uma só requisição e a tarefa termina concluída', async () => {
    const { contagem } = apiEmMemoria([umaTarefa({ titulo: 'Pagar conta' })], { atraso: 200 })
    render(<App />)
    const concluir = within(await linhaDa('Pagar conta')).getByRole('button', { name: 'Concluir' })

    await userEvent.dblClick(concluir)

    expect(concluir).toBeDisabled()
    expect(await within(await linhaDa('Pagar conta')).findByText('Concluída')).toBeInTheDocument()
    expect(contagem.concluir).toBe(1)
    expect(contagem.reabrir).toBe(0)
  })

  it('o score é consultado de novo após concluir', async () => {
    const { contagem } = apiEmMemoria([umaTarefa({ titulo: 'Pagar conta' })])
    render(<App />)
    const score = await screen.findByRole('region', { name: 'Score da semana' })
    await within(score).findByText('Sem tarefas finalizadas esta semana')

    await userEvent.click(within(await linhaDa('Pagar conta')).getByRole('button', { name: 'Concluir' }))

    expect(await within(score).findByText('1 de 1 no prazo')).toBeInTheDocument()
    expect(within(score).getByText('100%')).toBeInTheDocument()
    expect(contagem.score).toBe(2)
  })

  it('com o filtro "Atrasadas" ativo, a tarefa concluída sai da lista', async () => {
    apiEmMemoria([
      umaTarefa({ titulo: 'Pagar conta', dataVencimento: '2026-10-20' }),
      umaTarefa({ titulo: 'Renovar CNH', dataVencimento: '2026-10-10' }),
    ])
    render(<App />)
    await userEvent.click(await screen.findByRole('button', { name: 'Atrasadas' }))
    const linha = await linhaDa('Pagar conta')

    await userEvent.click(within(linha).getByRole('button', { name: 'Concluir' }))

    await screen.findByRole('heading', { name: 'Renovar CNH' })
    await expect.poll(() => screen.queryByRole('heading', { name: 'Pagar conta' })).toBeNull()
    expect(screen.getAllByRole('listitem')).toHaveLength(1)
  })
})
