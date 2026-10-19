import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from './App'
import { usarApi } from './test/api-simulada'

describe('App', () => {
  it('exibe o título da aplicação', async () => {
    usarApi()

    render(<App />)

    expect(screen.getByRole('heading', { level: 1, name: 'To-Do Inteligente' })).toBeInTheDocument()
    expect(await screen.findByText('Nenhuma tarefa por aqui.')).toBeInTheDocument()
  })
})
