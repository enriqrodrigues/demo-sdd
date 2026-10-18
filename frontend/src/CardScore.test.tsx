import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { CardScore } from './CardScore'

const semana = { inicioSemana: '2026-10-12', fimSemana: '2026-10-18' }

describe('CardScore', () => {
  it('mostra o percentual e as contagens', () => {
    render(<CardScore score={{ ...semana, percentual: 75, concluidasNoPrazo: 3, totalConcluidas: 4 }} />)

    const card = screen.getByRole('region', { name: 'Score da semana' })
    expect(card).toHaveTextContent('75%')
    expect(card).toHaveTextContent('3 de 4 no prazo')
    expect(card).toHaveTextContent('12/10/2026 a 18/10/2026')
  })

  it('mostra o percentual com casas decimais no formato brasileiro', () => {
    render(<CardScore score={{ ...semana, percentual: 66.67, concluidasNoPrazo: 2, totalConcluidas: 3 }} />)

    expect(screen.getByText('66,67%')).toBeInTheDocument()
  })

  it('informa quando não há tarefas concluídas na semana', () => {
    render(<CardScore score={{ ...semana, percentual: null, concluidasNoPrazo: 0, totalConcluidas: 0 }} />)

    expect(screen.getByText('Nenhuma tarefa concluída nesta semana.')).toBeInTheDocument()
    expect(screen.queryByText(/%/)).not.toBeInTheDocument()
  })
})
