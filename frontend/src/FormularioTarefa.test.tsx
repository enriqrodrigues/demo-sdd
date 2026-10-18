import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ErroApi, ErroValidacao, type Tarefa } from './api'
import { FormularioTarefa } from './FormularioTarefa'

const tarefa: Tarefa = {
  id: '1',
  titulo: 'Pagar conta',
  descricao: 'Luz',
  dataVencimento: '2026-10-20',
  prioridade: 'ALTA',
  status: 'PENDENTE',
  dataConclusao: null,
  situacaoPrazo: 'NO_PRAZO',
}

describe('FormularioTarefa', () => {
  it('cria uma tarefa com os dados digitados e limpa o formulário', async () => {
    const onSalvar = vi.fn().mockResolvedValue(undefined)
    render(<FormularioTarefa onSalvar={onSalvar} />)

    await userEvent.type(screen.getByLabelText('Título'), 'Pagar conta')
    await userEvent.type(screen.getByLabelText('Descrição'), 'Luz')
    await userEvent.type(screen.getByLabelText('Vencimento'), '2026-10-20')
    await userEvent.selectOptions(screen.getByLabelText('Prioridade'), 'ALTA')
    await userEvent.click(screen.getByRole('button', { name: 'Adicionar tarefa' }))

    expect(onSalvar).toHaveBeenCalledWith({
      titulo: 'Pagar conta',
      descricao: 'Luz',
      dataVencimento: '2026-10-20',
      prioridade: 'ALTA',
    })
    expect(screen.getByLabelText('Título')).toHaveValue('')
    expect(screen.getByLabelText('Descrição')).toHaveValue('')
    expect(screen.getByLabelText('Vencimento')).toHaveValue('')
    expect(screen.getByLabelText('Prioridade')).toHaveValue('')
  })

  it('envia campos vazios como null para a API validar', async () => {
    const onSalvar = vi.fn().mockResolvedValue(undefined)
    render(<FormularioTarefa onSalvar={onSalvar} />)

    await userEvent.click(screen.getByRole('button', { name: 'Adicionar tarefa' }))

    expect(onSalvar).toHaveBeenCalledWith({ titulo: '', descricao: null, dataVencimento: null, prioridade: null })
  })

  it('edita uma tarefa existente partindo dos valores atuais', async () => {
    const onSalvar = vi.fn().mockResolvedValue(undefined)
    render(<FormularioTarefa tarefa={tarefa} onSalvar={onSalvar} onCancelar={vi.fn()} />)

    expect(screen.getByRole('heading', { name: 'Editar tarefa' })).toBeInTheDocument()
    expect(screen.getByLabelText('Título')).toHaveValue('Pagar conta')
    expect(screen.getByLabelText('Descrição')).toHaveValue('Luz')
    expect(screen.getByLabelText('Vencimento')).toHaveValue('2026-10-20')
    expect(screen.getByLabelText('Prioridade')).toHaveValue('ALTA')

    await userEvent.clear(screen.getByLabelText('Título'))
    await userEvent.type(screen.getByLabelText('Título'), 'Pagar conta de luz')
    await userEvent.selectOptions(screen.getByLabelText('Prioridade'), 'BAIXA')
    await userEvent.click(screen.getByRole('button', { name: 'Salvar alterações' }))

    expect(onSalvar).toHaveBeenCalledWith({
      titulo: 'Pagar conta de luz',
      descricao: 'Luz',
      dataVencimento: '2026-10-20',
      prioridade: 'BAIXA',
    })
  })

  it('cancela a edição', async () => {
    const onCancelar = vi.fn()
    render(<FormularioTarefa tarefa={tarefa} onSalvar={vi.fn()} onCancelar={onCancelar} />)

    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }))

    expect(onCancelar).toHaveBeenCalled()
  })

  it('mostra o erro da API junto ao campo e mantém os dados digitados', async () => {
    const onSalvar = vi
      .fn()
      .mockRejectedValue(new ErroValidacao('Um ou mais campos são inválidos.', { titulo: 'O título é obrigatório.' }))
    render(<FormularioTarefa onSalvar={onSalvar} />)

    await userEvent.type(screen.getByLabelText('Descrição'), 'Luz')
    await userEvent.type(screen.getByLabelText('Vencimento'), '2026-10-20')
    await userEvent.selectOptions(screen.getByLabelText('Prioridade'), 'MEDIA')
    await userEvent.click(screen.getByRole('button', { name: 'Adicionar tarefa' }))

    const titulo = screen.getByLabelText('Título')
    expect(titulo).toHaveAccessibleDescription('O título é obrigatório.')
    expect(titulo).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByLabelText('Descrição')).toHaveValue('Luz')
    expect(screen.getByLabelText('Vencimento')).toHaveValue('2026-10-20')
    expect(screen.getByLabelText('Prioridade')).toHaveValue('MEDIA')
  })

  it('mostra erros de vários campos ao mesmo tempo', async () => {
    const onSalvar = vi.fn().mockRejectedValue(
      new ErroValidacao('Um ou mais campos são inválidos.', {
        titulo: 'O título é obrigatório.',
        prioridade: 'A prioridade é obrigatória.',
      }),
    )
    render(<FormularioTarefa onSalvar={onSalvar} />)

    await userEvent.click(screen.getByRole('button', { name: 'Adicionar tarefa' }))

    expect(screen.getByLabelText('Título')).toHaveAccessibleDescription('O título é obrigatório.')
    expect(screen.getByLabelText('Prioridade')).toHaveAccessibleDescription('A prioridade é obrigatória.')
  })

  it('mostra um erro geral quando a falha não é de validação', async () => {
    const onSalvar = vi.fn().mockRejectedValue(new ErroApi('Não foi possível se comunicar com o servidor.'))
    render(<FormularioTarefa onSalvar={onSalvar} />)

    await userEvent.type(screen.getByLabelText('Título'), 'Pagar conta')
    await userEvent.click(screen.getByRole('button', { name: 'Adicionar tarefa' }))

    expect(screen.getByRole('alert')).toHaveTextContent('Não foi possível se comunicar com o servidor.')
    expect(screen.getByLabelText('Título')).toHaveValue('Pagar conta')
  })
})
