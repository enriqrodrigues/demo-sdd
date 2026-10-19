import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import App from '../App'
import type { Tarefa } from '../api'
import { problema, SCORE_VAZIO, umaTarefa } from '../test/api-simulada'
import { servidor } from '../test/servidor'

/**
 * A tarefa X é lida pendente na versão 0; outra aba a conclui (versão 1) antes do primeiro
 * salvar, que recebe 409. Edições com a versão atual são aplicadas.
 */
function apiComConclusaoEmOutraAba() {
  const pendente = umaTarefa({ titulo: 'Pagar conta', version: 0 })
  let atual: Tarefa = pendente
  const versoesEnviadas: number[] = []
  let primeiraLeitura = true
  servidor.use(
    http.get('/api/tarefas', () => {
      const resposta = HttpResponse.json([atual])
      if (primeiraLeitura) {
        primeiraLeitura = false
        // A outra aba conclui a tarefa logo depois de esta aba carregar a lista.
        atual = { ...pendente, status: 'CONCLUIDA', situacaoPrazo: 'CONCLUIDA', concluidaEm: '2026-10-21T09:00:00-03:00', version: 1 }
      }
      return resposta
    }),
    http.get('/api/produtividade/semana', () => HttpResponse.json(SCORE_VAZIO)),
    http.get(`/api/tarefas/${pendente.id}`, () => HttpResponse.json(atual)),
    http.put(`/api/tarefas/${pendente.id}`, async ({ request }) => {
      const corpo = (await request.json()) as Record<string, unknown>
      versoesEnviadas.push(Number(corpo.version))
      if (corpo.version !== atual.version) {
        return problema(409)
      }
      atual = { ...atual, titulo: String(corpo.titulo), version: atual.version + 1 }
      return HttpResponse.json(atual)
    }),
  )
  return versoesEnviadas
}

async function editarTituloPara(novoTitulo: string) {
  const linha = (await screen.findByRole('heading', { name: 'Pagar conta' })).closest('li') as HTMLElement
  await userEvent.click(within(linha).getByRole('button', { name: 'Editar' }))
  const formulario = screen.getByRole('form', { name: 'Editar tarefa' })
  await userEvent.clear(within(formulario).getByLabelText('Título'))
  await userEvent.type(within(formulario).getByLabelText('Título'), novoTitulo)
  await userEvent.click(within(formulario).getByRole('button', { name: 'Salvar' }))
  return formulario
}

describe('Conflito de edição', () => {
  it('avisa do conflito, mostra a tarefa concluída e mantém o que foi digitado', async () => {
    apiComConclusaoEmOutraAba()
    render(<App />)

    const formulario = await editarTituloPara('Pagar conta de luz')

    expect(await within(formulario).findByRole('alert')).toHaveTextContent('alterada em outro lugar')
    expect(screen.getByRole('form', { name: 'Editar tarefa' })).toBeInTheDocument()
    expect(within(formulario).getByLabelText('Título')).toHaveValue('Pagar conta de luz')
    const linha = screen.getByRole('listitem')
    expect(await within(linha).findByText('Concluída')).toBeInTheDocument()
  })

  it('salvar de novo após o conflito envia a version recarregada e aplica a edição', async () => {
    const versoesEnviadas = apiComConclusaoEmOutraAba()
    render(<App />)
    const formulario = await editarTituloPara('Pagar conta de luz')
    await within(formulario).findByRole('alert')

    await userEvent.click(within(formulario).getByRole('button', { name: 'Salvar' }))

    expect(await screen.findByRole('heading', { name: 'Pagar conta de luz' })).toBeInTheDocument()
    expect(screen.queryByRole('form', { name: 'Editar tarefa' })).not.toBeInTheDocument()
    expect(versoesEnviadas).toEqual([0, 1])
  })
})
