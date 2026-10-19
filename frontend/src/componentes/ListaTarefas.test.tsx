import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from '../App'
import { umaTarefa, usarApi } from '../test/api-simulada'

describe('Lista de tarefas', () => {
  it('exibe título, prioridade, vencimento em DD/MM/AAAA e a situação de cada tarefa', async () => {
    usarApi({
      tarefas: [
        umaTarefa({ titulo: 'Pagar conta', prioridade: 'ALTA', dataVencimento: '2026-10-20', situacaoPrazo: 'ATRASADA' }),
        umaTarefa({ titulo: 'Ligar para o banco', prioridade: 'MEDIA', dataVencimento: '2026-10-21', situacaoPrazo: 'PARA_HOJE' }),
        umaTarefa({ titulo: 'Revisar PR', prioridade: 'BAIXA', dataVencimento: '2026-11-02', situacaoPrazo: 'NO_PRAZO' }),
        umaTarefa({
          titulo: 'Comprar pão',
          dataVencimento: '2026-10-01',
          status: 'CONCLUIDA',
          situacaoPrazo: 'CONCLUIDA',
          concluidaEm: '2026-10-01T09:00:00-03:00',
        }),
      ],
    })

    render(<App />)

    const itens = await screen.findAllByRole('listitem')
    expect(itens).toHaveLength(4)

    const [atrasada, hoje, noPrazo, concluida] = itens
    expect(within(atrasada).getByText('Pagar conta')).toBeInTheDocument()
    expect(within(atrasada).getByText('Alta')).toBeInTheDocument()
    expect(within(atrasada).getByText('20/10/2026')).toBeInTheDocument()
    expect(within(atrasada).getByText('Atrasada')).toBeInTheDocument()

    expect(within(hoje).getByText('Média')).toBeInTheDocument()
    expect(within(hoje).getByText('21/10/2026')).toBeInTheDocument()
    expect(within(hoje).getByText('Para hoje')).toBeInTheDocument()

    expect(within(noPrazo).getByText('Baixa')).toBeInTheDocument()
    expect(within(noPrazo).getByText('02/11/2026')).toBeInTheDocument()
    expect(within(noPrazo).getByText('No prazo')).toBeInTheDocument()

    expect(within(concluida).getByText('Concluída')).toBeInTheDocument()
  })

  it('mantém a ordem retornada pela API', async () => {
    usarApi({ tarefas: [umaTarefa({ titulo: 'B' }), umaTarefa({ titulo: 'C' }), umaTarefa({ titulo: 'A' })] })

    render(<App />)

    const itens = await screen.findAllByRole('listitem')
    expect(itens.map((item) => within(item).getByRole('heading').textContent)).toEqual(['B', 'C', 'A'])
  })

  it('exibe uma mensagem quando não há tarefas', async () => {
    usarApi({ tarefas: [] })

    render(<App />)

    expect(await screen.findByText('Nenhuma tarefa por aqui.')).toBeInTheDocument()
    expect(screen.queryAllByRole('listitem')).toHaveLength(0)
  })
})
