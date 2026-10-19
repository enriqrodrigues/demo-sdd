import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from '../App'
import { usarApi } from '../test/api-simulada'

const regiaoDoScore = () => screen.findByRole('region', { name: 'Score da semana' })

describe('Score no cabeçalho', () => {
  it('exibe percentual, contagem no prazo e período quando há dados', async () => {
    usarApi({
      score: { inicioSemana: '2026-10-19', fimSemana: '2026-10-25', concluidas: 3, noPrazo: 2, percentual: 66.67 },
    })

    render(<App />)

    const score = await regiaoDoScore()
    expect(await within(score).findByText('66,67%')).toBeInTheDocument()
    expect(within(score).getByText('2 de 3 no prazo')).toBeInTheDocument()
    expect(within(score).getByText('19/10 a 25/10')).toBeInTheDocument()
  })

  it('exibe percentual inteiro sem casas decimais', async () => {
    usarApi({
      score: { inicioSemana: '2026-10-19', fimSemana: '2026-10-25', concluidas: 4, noPrazo: 4, percentual: 100 },
    })

    render(<App />)

    expect(await within(await regiaoDoScore()).findByText('100%')).toBeInTheDocument()
  })

  it('exibe aviso quando não há tarefas finalizadas na semana', async () => {
    usarApi()

    render(<App />)

    const score = await regiaoDoScore()
    expect(await within(score).findByText('Sem tarefas finalizadas esta semana')).toBeInTheDocument()
    expect(within(score).getByText('19/10 a 25/10')).toBeInTheDocument()
    expect(within(score).queryByText(/%/)).not.toBeInTheDocument()
  })
})
