import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import App from '../App'
import { apiEmMemoria, umaTarefa } from '../test/api-simulada'

async function pedirExclusao(titulo: string) {
  const linha = (await screen.findByRole('heading', { name: titulo })).closest('li') as HTMLElement
  await userEvent.click(within(linha).getByRole('button', { name: 'Excluir' }))
  return screen.getByRole('dialog', { name: 'Excluir tarefa?' })
}

describe('Excluir pela interface', () => {
  it('confirmar exclui a tarefa e ela some da lista', async () => {
    const { contagem } = apiEmMemoria([umaTarefa({ titulo: 'Pagar conta' }), umaTarefa({ titulo: 'Comprar pão' })])
    render(<App />)

    const dialogo = await pedirExclusao('Pagar conta')
    expect(dialogo).toHaveTextContent('Pagar conta')
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Excluir' }))

    await expect.poll(() => screen.queryByRole('heading', { name: 'Pagar conta' })).toBeNull()
    expect(screen.getByRole('heading', { name: 'Comprar pão' })).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(contagem.excluir).toBe(1)
  })

  it('cancelar não envia requisição e a tarefa continua na lista', async () => {
    const { contagem } = apiEmMemoria([umaTarefa({ titulo: 'Pagar conta' })])
    render(<App />)

    const dialogo = await pedirExclusao('Pagar conta')
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Cancelar' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Pagar conta' })).toBeInTheDocument()
    expect(contagem.excluir).toBe(0)
  })

  it('o diálogo é acessível: modal, foco em Cancelar e Esc cancela', async () => {
    const { contagem } = apiEmMemoria([umaTarefa({ titulo: 'Pagar conta' })])
    render(<App />)

    const dialogo = await pedirExclusao('Pagar conta')
    expect(dialogo).toHaveAttribute('aria-modal', 'true')
    expect(within(dialogo).getByRole('button', { name: 'Cancelar' })).toHaveFocus()

    await userEvent.keyboard('{Escape}')

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(contagem.excluir).toBe(0)
  })
})
