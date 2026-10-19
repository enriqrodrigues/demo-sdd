import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { delay, http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import App from '../App'
import { apiEmMemoria, problema, umaTarefa } from '../test/api-simulada'
import { servidor } from '../test/servidor'

const linhaDa = async (titulo: string) =>
  (await screen.findByRole('heading', { name: titulo })).closest('li') as HTMLElement

describe('Diálogo de exclusão acessível', () => {
  it('mantém o foco dentro do diálogo com Tab e Shift+Tab', async () => {
    apiEmMemoria([umaTarefa({ titulo: 'Pagar conta' })])
    render(<App />)
    await userEvent.click(within(await linhaDa('Pagar conta')).getByRole('button', { name: 'Excluir' }))
    const dialogo = screen.getByRole('dialog')
    const cancelar = within(dialogo).getByRole('button', { name: 'Cancelar' })
    const confirmar = within(dialogo).getByRole('button', { name: 'Excluir' })

    await userEvent.tab()
    expect(confirmar).toHaveFocus()
    await userEvent.tab()
    expect(cancelar).toHaveFocus()
    await userEvent.tab({ shift: true })
    expect(confirmar).toHaveFocus()
  })

  it('devolve o foco ao botão que abriu o diálogo ao cancelar', async () => {
    apiEmMemoria([umaTarefa({ titulo: 'Pagar conta' })])
    render(<App />)
    const excluir = within(await linhaDa('Pagar conta')).getByRole('button', { name: 'Excluir' })
    await userEvent.click(excluir)

    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancelar' }))

    expect(excluir).toHaveFocus()
  })

  it('Esc cancela mesmo com o foco fora do diálogo', async () => {
    const { contagem } = apiEmMemoria([umaTarefa({ titulo: 'Pagar conta' })])
    render(<App />)
    await userEvent.click(within(await linhaDa('Pagar conta')).getByRole('button', { name: 'Excluir' }))
    ;(document.activeElement as HTMLElement).blur()

    await userEvent.keyboard('{Escape}')

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(contagem.excluir).toBe(0)
  })
})

describe('Falha ao trocar de filtro', () => {
  it('não mostra a lista do filtro anterior sob o novo filtro e permite tentar de novo', async () => {
    apiEmMemoria([
      umaTarefa({ titulo: 'Pagar conta', dataVencimento: '2026-10-20' }),
      umaTarefa({ titulo: 'Revisar PR', dataVencimento: '2026-11-02' }),
    ])
    render(<App />)
    await screen.findByRole('heading', { name: 'Revisar PR' })
    servidor.use(http.get('/api/tarefas', () => HttpResponse.error(), { once: true }))

    await userEvent.click(screen.getByRole('button', { name: 'Atrasadas' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível falar com o servidor')
    expect(screen.queryByRole('heading', { name: 'Revisar PR' })).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(await screen.findByRole('heading', { name: 'Pagar conta' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Revisar PR' })).not.toBeInTheDocument()
  })

  it('falha ao carregar o score mostra o score como indisponível', async () => {
    apiEmMemoria([])
    servidor.use(http.get('/api/produtividade/semana', () => problema(500)))
    render(<App />)

    const score = await screen.findByRole('region', { name: 'Score da semana' })
    expect(await within(score).findByText('Score indisponível')).toBeInTheDocument()
  })
})

describe('Conflito seguido de falha ao recarregar', () => {
  it('tarefa excluída entre o 409 e a recarga: avisa, remove e fecha o formulário', async () => {
    const api = apiEmMemoria([umaTarefa({ titulo: 'Pagar conta', version: 0 }), umaTarefa({ titulo: 'Comprar pão' })])
    render(<App />)
    await userEvent.click(within(await linhaDa('Pagar conta')).getByRole('button', { name: 'Editar' }))
    servidor.use(
      http.put('/api/tarefas/:id', () => {
        api.excluirEmOutraAba('Pagar conta')
        return problema(409)
      }),
    )

    const formulario = screen.getByRole('form', { name: 'Editar tarefa' })
    await userEvent.click(within(formulario).getByRole('button', { name: 'Salvar' }))

    expect(await screen.findByText('Esta tarefa não existe mais. Ela foi removida da lista.')).toBeInTheDocument()
    expect(screen.queryByRole('form', { name: 'Editar tarefa' })).not.toBeInTheDocument()
    await expect.poll(() => screen.queryByRole('heading', { name: 'Pagar conta' })).toBeNull()
  })
})

describe('Resultado tardio de um formulário', () => {
  it('a criação que termina depois não fecha o formulário de edição aberto em seguida', async () => {
    apiEmMemoria([umaTarefa({ titulo: 'Comprar pão' })])
    servidor.use(
      http.post('/api/tarefas', async ({ request }) => {
        await delay(300)
        return HttpResponse.json(umaTarefa((await request.json()) as object), { status: 201 })
      }),
    )
    render(<App />)
    await userEvent.click(await screen.findByRole('button', { name: 'Nova tarefa' }))
    const nova = screen.getByRole('form', { name: 'Nova tarefa' })
    await userEvent.type(within(nova).getByLabelText('Título'), 'Pagar conta')
    await userEvent.click(within(nova).getByRole('button', { name: 'Salvar' }))

    await userEvent.click(within(await linhaDa('Comprar pão')).getByRole('button', { name: 'Editar' }))
    const edicao = screen.getByRole('form', { name: 'Editar tarefa' })
    await userEvent.type(within(edicao).getByLabelText('Descrição'), 'integral')
    await delay(500)

    expect(screen.getByRole('form', { name: 'Editar tarefa' })).toBeInTheDocument()
    expect(within(edicao).getByLabelText('Descrição')).toHaveValue('integral')
  })
})

describe('Ações e avisos', () => {
  it('Editar fica desabilitado enquanto a tarefa está sendo concluída', async () => {
    apiEmMemoria([umaTarefa({ titulo: 'Pagar conta' })], { atraso: 300 })
    render(<App />)
    const linha = await linhaDa('Pagar conta')

    await userEvent.click(within(linha).getByRole('button', { name: 'Concluir' }))

    expect(within(linha).getByRole('button', { name: 'Editar' })).toBeDisabled()
    expect(await within(await linhaDa('Pagar conta')).findByText('Concluída')).toBeInTheDocument()
    expect(within(await linhaDa('Pagar conta')).getByRole('button', { name: 'Editar' })).toBeEnabled()
  })

  it('409 ao concluir (corrida com outra aba) avisa e recarrega a lista', async () => {
    apiEmMemoria([umaTarefa({ titulo: 'Pagar conta' })])
    servidor.use(http.post('/api/tarefas/:id/concluir', () => problema(409)))
    render(<App />)

    await userEvent.click(within(await linhaDa('Pagar conta')).getByRole('button', { name: 'Concluir' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('alterada em outro lugar')
    expect(within(await linhaDa('Pagar conta')).getByRole('button', { name: 'Concluir' })).toBeEnabled()
  })

  it('o aviso some quando uma ação seguinte dá certo', async () => {
    apiEmMemoria([umaTarefa({ titulo: 'Pagar conta' })])
    servidor.use(http.post('/api/tarefas/:id/concluir', () => HttpResponse.error(), { once: true }))
    render(<App />)
    const concluir = () =>
      linhaDa('Pagar conta').then((linha) =>
        userEvent.click(within(linha).getByRole('button', { name: 'Concluir' })),
      )

    await concluir()
    expect(await screen.findByRole('alert')).toBeInTheDocument()
    await concluir()

    expect(await within(await linhaDa('Pagar conta')).findByText('Concluída')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('erro 400 só em campo que o formulário não exibe mostra mensagem geral', async () => {
    apiEmMemoria([umaTarefa({ titulo: 'Pagar conta' })])
    servidor.use(
      http.put('/api/tarefas/:id', () =>
        problema(400, { erros: [{ campo: 'version', mensagem: 'Informe a versão da tarefa que está sendo editada.' }] }),
      ),
    )
    render(<App />)
    await userEvent.click(within(await linhaDa('Pagar conta')).getByRole('button', { name: 'Editar' }))
    const formulario = screen.getByRole('form', { name: 'Editar tarefa' })

    await userEvent.click(within(formulario).getByRole('button', { name: 'Salvar' }))

    expect(await within(formulario).findByRole('alert')).toHaveTextContent('Não foi possível salvar')
  })
})
