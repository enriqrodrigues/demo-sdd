import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import App from '../App'
import { apiEmMemoria, problema, umaTarefa, usarApi } from '../test/api-simulada'
import { servidor } from '../test/servidor'

const MENSAGEM = 'Não foi possível falar com o servidor. Tente novamente.'

const linhaDa = async (titulo: string) =>
  (await screen.findByRole('heading', { name: titulo })).closest('li') as HTMLElement

describe('Falha de comunicação', () => {
  it('servidor indisponível ao concluir: mensagem, tarefa continua pendente e controle reabilitado', async () => {
    apiEmMemoria([umaTarefa({ titulo: 'Pagar conta', dataVencimento: '2026-10-25' })])
    servidor.use(http.post('/api/tarefas/:id/concluir', () => HttpResponse.error()))
    render(<App />)
    const linha = await linhaDa('Pagar conta')

    await userEvent.click(within(linha).getByRole('button', { name: 'Concluir' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(MENSAGEM)
    const depois = await linhaDa('Pagar conta')
    expect(within(depois).getByText('No prazo')).toBeInTheDocument()
    expect(within(depois).getByRole('button', { name: 'Concluir' })).toBeEnabled()
  })

  it('erro 500 ao excluir mantém a tarefa e exibe a mensagem', async () => {
    apiEmMemoria([umaTarefa({ titulo: 'Pagar conta' })])
    servidor.use(http.delete('/api/tarefas/:id', () => problema(500)))
    render(<App />)

    await userEvent.click(within(await linhaDa('Pagar conta')).getByRole('button', { name: 'Excluir' }))
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Excluir' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(MENSAGEM)
    expect(within(await linhaDa('Pagar conta')).getByRole('button', { name: 'Excluir' })).toBeEnabled()
  })

  it('falha ao salvar o formulário mantém os valores e reabilita o botão', async () => {
    apiEmMemoria([])
    servidor.use(http.post('/api/tarefas', () => HttpResponse.error()))
    render(<App />)
    await userEvent.click(await screen.findByRole('button', { name: 'Nova tarefa' }))
    const formulario = screen.getByRole('form', { name: 'Nova tarefa' })
    await userEvent.type(within(formulario).getByLabelText('Título'), 'Pagar conta')

    await userEvent.click(within(formulario).getByRole('button', { name: 'Salvar' }))

    expect(await within(formulario).findByRole('alert')).toHaveTextContent(MENSAGEM)
    expect(within(formulario).getByLabelText('Título')).toHaveValue('Pagar conta')
    expect(within(formulario).getByRole('button', { name: 'Salvar' })).toBeEnabled()
  })

  it('falha ao carregar a lista exibe a mensagem em vez de carregar para sempre', async () => {
    usarApi()
    servidor.use(http.get('/api/tarefas', () => problema(503)))
    render(<App />)

    expect(await screen.findByRole('alert')).toHaveTextContent(MENSAGEM)
    expect(screen.queryByText('Carregando…')).not.toBeInTheDocument()
  })
})
