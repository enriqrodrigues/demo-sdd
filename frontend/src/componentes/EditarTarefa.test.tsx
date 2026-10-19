import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import App from '../App'
import type { Tarefa } from '../api'
import { SCORE_VAZIO, umaTarefa } from '../test/api-simulada'
import { servidor } from '../test/servidor'

/** API em memória para edição: PUT aplica os dados e incrementa a versão. */
function apiDeEdicao(tarefas: Tarefa[]) {
  const edicoes: { id: string; corpo: Record<string, unknown> }[] = []
  servidor.use(
    http.get('/api/tarefas', () => HttpResponse.json(tarefas)),
    http.get('/api/produtividade/semana', () => HttpResponse.json(SCORE_VAZIO)),
    http.put('/api/tarefas/:id', async ({ params, request }) => {
      const corpo = (await request.json()) as Record<string, unknown>
      edicoes.push({ id: String(params.id), corpo })
      const indice = tarefas.findIndex((t) => t.id === params.id)
      const { version, ...dados } = corpo
      tarefas[indice] = { ...tarefas[indice], ...dados, version: Number(version) + 1 } as Tarefa
      return HttpResponse.json(tarefas[indice])
    }),
  )
  return edicoes
}

const linhaDa = async (titulo: string) =>
  (await screen.findByRole('heading', { name: titulo })).closest('li') as HTMLElement

describe('Editar tarefa pela interface', () => {
  it('abre o formulário preenchido, envia a version e mostra o novo título', async () => {
    const tarefa = umaTarefa({
      titulo: 'Pagar conta',
      descricao: 'Luz',
      dataVencimento: '2026-10-25',
      prioridade: 'ALTA',
      version: 3,
    })
    const edicoes = apiDeEdicao([tarefa])
    render(<App />)

    await userEvent.click(within(await linhaDa('Pagar conta')).getByRole('button', { name: 'Editar' }))
    const formulario = screen.getByRole('form', { name: 'Editar tarefa' })
    expect(within(formulario).getByLabelText('Título')).toHaveValue('Pagar conta')
    expect(within(formulario).getByLabelText('Descrição')).toHaveValue('Luz')
    expect(within(formulario).getByLabelText('Vencimento')).toHaveValue('2026-10-25')
    expect(within(formulario).getByLabelText('Prioridade')).toHaveValue('ALTA')

    await userEvent.clear(within(formulario).getByLabelText('Título'))
    await userEvent.type(within(formulario).getByLabelText('Título'), 'Pagar conta de luz')
    await userEvent.click(within(formulario).getByRole('button', { name: 'Salvar' }))

    expect(await screen.findByRole('heading', { name: 'Pagar conta de luz' })).toBeInTheDocument()
    expect(screen.queryByRole('form', { name: 'Editar tarefa' })).not.toBeInTheDocument()
    expect(edicoes).toHaveLength(1)
    expect(edicoes[0]).toEqual({
      id: tarefa.id,
      corpo: { titulo: 'Pagar conta de luz', descricao: 'Luz', dataVencimento: '2026-10-25', prioridade: 'ALTA', version: 3 },
    })
  })

  it('bloqueia concluir/reabrir e excluir apenas da tarefa em edição', async () => {
    apiDeEdicao([
      umaTarefa({ titulo: 'Pagar conta' }),
      umaTarefa({ titulo: 'Comprar pão', status: 'CONCLUIDA', situacaoPrazo: 'CONCLUIDA' }),
    ])
    render(<App />)
    const emEdicao = await linhaDa('Pagar conta')
    const outra = await linhaDa('Comprar pão')

    await userEvent.click(within(emEdicao).getByRole('button', { name: 'Editar' }))

    expect(within(emEdicao).getByRole('button', { name: 'Concluir' })).toBeDisabled()
    expect(within(emEdicao).getByRole('button', { name: 'Excluir' })).toBeDisabled()
    expect(within(outra).getByRole('button', { name: 'Reabrir' })).toBeEnabled()
    expect(within(outra).getByRole('button', { name: 'Excluir' })).toBeEnabled()
  })

  it('cancelar não envia a edição e mantém a tarefa como estava', async () => {
    const edicoes = apiDeEdicao([umaTarefa({ titulo: 'Pagar conta' })])
    render(<App />)

    await userEvent.click(within(await linhaDa('Pagar conta')).getByRole('button', { name: 'Editar' }))
    const formulario = screen.getByRole('form', { name: 'Editar tarefa' })
    await userEvent.type(within(formulario).getByLabelText('Título'), ' alterado')
    await userEvent.click(within(formulario).getByRole('button', { name: 'Cancelar' }))

    expect(screen.queryByRole('form', { name: 'Editar tarefa' })).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Pagar conta' })).toBeInTheDocument()
    expect(edicoes).toHaveLength(0)
    expect(within(await linhaDa('Pagar conta')).getByRole('button', { name: 'Concluir' })).toBeEnabled()
  })
})
