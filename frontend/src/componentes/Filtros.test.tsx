import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import App from '../App'
import { umaTarefa, usarApi } from '../test/api-simulada'
import { servidor } from '../test/servidor'

const atrasada = umaTarefa({ titulo: 'Pagar conta', situacaoPrazo: 'ATRASADA', dataVencimento: '2026-10-20' })
const paraHoje = umaTarefa({ titulo: 'Ligar para o banco', situacaoPrazo: 'PARA_HOJE', dataVencimento: '2026-10-21' })
const concluida = umaTarefa({ titulo: 'Comprar pão', status: 'CONCLUIDA', situacaoPrazo: 'CONCLUIDA' })

/** Responde como a API real filtraria e guarda os parâmetros de cada listagem. */
function apiQueFiltra() {
  const consultas: URLSearchParams[] = []
  usarApi()
  servidor.use(
    http.get('/api/tarefas', ({ request }) => {
      const parametros = new URL(request.url).searchParams
      consultas.push(parametros)
      const todas = [atrasada, paraHoje, concluida]
      return HttpResponse.json(
        todas.filter(
          (t) =>
            (!parametros.has('status') || t.status === parametros.get('status')) &&
            (!parametros.has('situacaoPrazo') || t.situacaoPrazo === parametros.get('situacaoPrazo')),
        ),
      )
    }),
  )
  return consultas
}

const titulosExibidos = () =>
  screen.getAllByRole('listitem').map((item) => within(item).getByRole('heading').textContent)

describe('Filtros da lista', () => {
  it('filtrar atrasadas exibe apenas tarefas com situação ATRASADA', async () => {
    const consultas = apiQueFiltra()
    render(<App />)
    await screen.findByText('Comprar pão')

    await userEvent.click(screen.getByRole('button', { name: 'Atrasadas' }))

    await screen.findByText('Pagar conta')
    expect(screen.queryByText('Comprar pão')).not.toBeInTheDocument()
    expect(titulosExibidos()).toEqual(['Pagar conta'])
    const ultima = consultas.at(-1)!
    expect(ultima.get('situacaoPrazo')).toBe('ATRASADA')
    expect(ultima.has('status')).toBe(false)
    expect(screen.getByRole('button', { name: 'Atrasadas' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: 'Todas' })).toHaveAttribute('aria-pressed', 'false')
  })

  it('mapeia cada filtro para os parâmetros da API', async () => {
    const consultas = apiQueFiltra()
    render(<App />)
    await screen.findByText('Comprar pão')
    expect([...consultas[0].keys()]).toEqual([])

    await userEvent.click(screen.getByRole('button', { name: 'Pendentes' }))
    await screen.findByText('Ligar para o banco')
    expect(screen.queryByText('Comprar pão')).not.toBeInTheDocument()
    expect(consultas.at(-1)!.get('status')).toBe('PENDENTE')

    await userEvent.click(screen.getByRole('button', { name: 'Para hoje' }))
    await screen.findByText('Ligar para o banco')
    expect(screen.queryByText('Pagar conta')).not.toBeInTheDocument()
    expect(consultas.at(-1)!.get('situacaoPrazo')).toBe('PARA_HOJE')

    await userEvent.click(screen.getByRole('button', { name: 'Todas' }))
    await screen.findByText('Comprar pão')
    expect([...consultas.at(-1)!.keys()]).toEqual([])
  })
})
