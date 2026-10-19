import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { delay, http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import App from '../App'
import type { Tarefa } from '../api'
import { problema, SCORE_VAZIO, umaTarefa } from '../test/api-simulada'
import { servidor } from '../test/servidor'

/** API em memória: POST acrescenta a tarefa e as listagens seguintes a incluem. */
function apiEmMemoria({ atrasoNaCriacao = 0 } = {}) {
  const tarefas: Tarefa[] = []
  const estado = { criacoes: 0, consultasScore: 0 }
  servidor.use(
    http.get('/api/tarefas', () => HttpResponse.json(tarefas)),
    http.get('/api/produtividade/semana', () => {
      estado.consultasScore += 1
      return HttpResponse.json(SCORE_VAZIO)
    }),
    http.post('/api/tarefas', async ({ request }) => {
      estado.criacoes += 1
      await delay(atrasoNaCriacao)
      const dados = (await request.json()) as Partial<Tarefa>
      const criada = umaTarefa({ ...dados, descricao: dados.descricao || null })
      tarefas.push(criada)
      return HttpResponse.json(criada, { status: 201 })
    }),
  )
  return estado
}

async function abrirFormulario() {
  await userEvent.click(await screen.findByRole('button', { name: 'Nova tarefa' }))
  return screen.getByRole('form', { name: 'Nova tarefa' })
}

async function preencher(formulario: HTMLElement, titulo: string) {
  await userEvent.type(within(formulario).getByLabelText('Título'), titulo)
  await userEvent.type(within(formulario).getByLabelText('Vencimento'), '2026-10-25')
  await userEvent.selectOptions(within(formulario).getByLabelText('Prioridade'), 'ALTA')
}

describe('Criar tarefa pela interface', () => {
  it('criação com sucesso fecha o formulário, mostra a nova tarefa e recarrega o score', async () => {
    const estado = apiEmMemoria()
    render(<App />)
    await screen.findByText('Nenhuma tarefa por aqui.')
    const formulario = await abrirFormulario()

    await preencher(formulario, 'Pagar conta')
    await userEvent.click(within(formulario).getByRole('button', { name: 'Salvar' }))

    expect(await screen.findByRole('heading', { name: 'Pagar conta' })).toBeInTheDocument()
    expect(screen.queryByRole('form', { name: 'Nova tarefa' })).not.toBeInTheDocument()
    expect(estado.consultasScore).toBe(2)
  })

  it('erros de validação da API aparecem junto aos campos e mantêm os valores', async () => {
    apiEmMemoria()
    servidor.use(
      http.post('/api/tarefas', () =>
        problema(400, {
          erros: [
            { campo: 'titulo', mensagem: 'Informe o título.' },
            { campo: 'dataVencimento', mensagem: 'Informe uma data válida no formato AAAA-MM-DD.' },
          ],
        }),
      ),
    )
    render(<App />)
    const formulario = await abrirFormulario()
    await userEvent.type(within(formulario).getByLabelText('Descrição'), 'Detalhes')

    await userEvent.click(within(formulario).getByRole('button', { name: 'Salvar' }))

    const titulo = within(formulario).getByLabelText('Título')
    expect(await within(formulario).findByText('Informe o título.')).toBeInTheDocument()
    expect(titulo).toHaveAccessibleDescription('Informe o título.')
    expect(titulo).toHaveAttribute('aria-invalid', 'true')
    expect(within(formulario).getByLabelText('Vencimento')).toHaveAccessibleDescription(
      'Informe uma data válida no formato AAAA-MM-DD.',
    )
    expect(screen.getByRole('form', { name: 'Nova tarefa' })).toBeInTheDocument()
    expect(within(formulario).getByLabelText('Descrição')).toHaveValue('Detalhes')
  })

  it('duplo clique em salvar cria apenas uma tarefa', async () => {
    const estado = apiEmMemoria({ atrasoNaCriacao: 200 })
    render(<App />)
    const formulario = await abrirFormulario()
    await preencher(formulario, 'Pagar conta')
    const salvar = within(formulario).getByRole('button', { name: 'Salvar' })

    await userEvent.dblClick(salvar)

    expect(salvar).toBeDisabled()
    expect(await screen.findByRole('heading', { name: 'Pagar conta' })).toBeInTheDocument()
    expect(estado.criacoes).toBe(1)
    expect(screen.getAllByRole('listitem')).toHaveLength(1)
  })

  it('impede digitar além de 100 caracteres no título e 2000 na descrição', async () => {
    apiEmMemoria()
    render(<App />)
    const formulario = await abrirFormulario()

    expect(within(formulario).getByLabelText('Título')).toHaveAttribute('maxLength', '100')
    expect(within(formulario).getByLabelText('Descrição')).toHaveAttribute('maxLength', '2000')
    await userEvent.type(within(formulario).getByLabelText('Título'), 'a'.repeat(105))
    expect(within(formulario).getByLabelText('Título')).toHaveValue('a'.repeat(100))
  })
})
