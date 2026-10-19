import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import App from '../App'
import { apiEmMemoria, umaTarefa } from '../test/api-simulada'
import { servidor } from '../test/servidor'

const linhaDa = async (titulo: string) =>
  (await screen.findByRole('heading', { name: titulo })).closest('li') as HTMLElement

describe('Criar tarefa pela interface › Criação com filtro ativo', () => {
  it('com "Atrasadas" ativo, criar uma tarefa futura volta para "Todas" e a mostra', async () => {
    apiEmMemoria([umaTarefa({ titulo: 'Pagar conta', dataVencimento: '2026-10-20' })])
    render(<App />)
    await userEvent.click(await screen.findByRole('button', { name: 'Atrasadas' }))
    await linhaDa('Pagar conta')

    await userEvent.click(screen.getByRole('button', { name: 'Nova tarefa' }))
    const formulario = screen.getByRole('form', { name: 'Nova tarefa' })
    await userEvent.type(within(formulario).getByLabelText('Título'), 'Revisar PR')
    await userEvent.type(within(formulario).getByLabelText('Vencimento'), '2026-10-30')
    await userEvent.click(within(formulario).getByRole('button', { name: 'Salvar' }))

    expect(await screen.findByRole('heading', { name: 'Revisar PR' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Todas' })).toHaveAttribute('aria-pressed', 'true')
  })

  it('se a nova tarefa atende ao filtro ativo, o filtro é mantido', async () => {
    apiEmMemoria([umaTarefa({ titulo: 'Pagar conta', dataVencimento: '2026-10-20' })])
    render(<App />)
    await userEvent.click(await screen.findByRole('button', { name: 'Atrasadas' }))
    await linhaDa('Pagar conta')

    await userEvent.click(screen.getByRole('button', { name: 'Nova tarefa' }))
    const formulario = screen.getByRole('form', { name: 'Nova tarefa' })
    await userEvent.type(within(formulario).getByLabelText('Título'), 'Renovar CNH')
    await userEvent.type(within(formulario).getByLabelText('Vencimento'), '2026-10-01')
    await userEvent.click(within(formulario).getByRole('button', { name: 'Salvar' }))

    expect(await screen.findByRole('heading', { name: 'Renovar CNH' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Atrasadas' })).toHaveAttribute('aria-pressed', 'true')
  })
})

describe('Falha ao recarregar depois de uma ação bem-sucedida', () => {
  it('mantém a lista exibida (com a ação aplicada) e avisa', async () => {
    apiEmMemoria([umaTarefa({ titulo: 'Pagar conta' })])
    render(<App />)
    const linha = await linhaDa('Pagar conta')
    servidor.use(http.get('/api/tarefas', () => HttpResponse.error()))

    await userEvent.click(within(linha).getByRole('button', { name: 'Concluir' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível falar com o servidor')
    expect(within(await linhaDa('Pagar conta')).getByText('Concluída')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Tentar novamente' })).not.toBeInTheDocument()
  })
})
