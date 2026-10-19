import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import App from '../App'
import { apiEmMemoria, umaTarefa } from '../test/api-simulada'

const linhaDa = async (titulo: string) =>
  (await screen.findByRole('heading', { name: titulo })).closest('li') as HTMLElement

async function esperarAvisoDeRemocao() {
  expect(await screen.findByText('Esta tarefa não existe mais. Ela foi removida da lista.')).toBeInTheDocument()
  expect(screen.getByRole('alert')).toHaveTextContent('não existe mais')
}

describe('Tarefa removida durante uma ação', () => {
  it('editar tarefa excluída em outra aba avisa, fecha o formulário e remove a tarefa', async () => {
    const api = apiEmMemoria([umaTarefa({ titulo: 'Pagar conta' }), umaTarefa({ titulo: 'Comprar pão' })])
    render(<App />)
    await userEvent.click(within(await linhaDa('Pagar conta')).getByRole('button', { name: 'Editar' }))
    api.excluirEmOutraAba('Pagar conta')

    const formulario = screen.getByRole('form', { name: 'Editar tarefa' })
    await userEvent.type(within(formulario).getByLabelText('Título'), ' de luz')
    await userEvent.click(within(formulario).getByRole('button', { name: 'Salvar' }))

    await esperarAvisoDeRemocao()
    expect(screen.queryByRole('form', { name: 'Editar tarefa' })).not.toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Pagar conta' })).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Comprar pão' })).toBeInTheDocument()
  })

  it('concluir tarefa excluída em outra aba avisa e remove a tarefa', async () => {
    const api = apiEmMemoria([umaTarefa({ titulo: 'Pagar conta' }), umaTarefa({ titulo: 'Comprar pão' })])
    render(<App />)
    const linha = await linhaDa('Pagar conta')
    api.excluirEmOutraAba('Pagar conta')

    await userEvent.click(within(linha).getByRole('button', { name: 'Concluir' }))

    await esperarAvisoDeRemocao()
    expect(screen.queryByRole('heading', { name: 'Pagar conta' })).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Comprar pão' })).toBeInTheDocument()
  })

  it('reabrir tarefa excluída em outra aba avisa e remove a tarefa', async () => {
    const api = apiEmMemoria([umaTarefa({ titulo: 'Pagar conta', status: 'CONCLUIDA' })])
    render(<App />)
    const linha = await linhaDa('Pagar conta')
    api.excluirEmOutraAba('Pagar conta')

    await userEvent.click(within(linha).getByRole('button', { name: 'Reabrir' }))

    await esperarAvisoDeRemocao()
    expect(screen.queryByRole('heading', { name: 'Pagar conta' })).not.toBeInTheDocument()
  })

  it('excluir tarefa já excluída em outra aba avisa e remove a tarefa', async () => {
    const api = apiEmMemoria([umaTarefa({ titulo: 'Pagar conta' })])
    render(<App />)
    const linha = await linhaDa('Pagar conta')
    api.excluirEmOutraAba('Pagar conta')

    await userEvent.click(within(linha).getByRole('button', { name: 'Excluir' }))
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Excluir' }))

    await esperarAvisoDeRemocao()
    expect(screen.queryByRole('heading', { name: 'Pagar conta' })).not.toBeInTheDocument()
  })
})
