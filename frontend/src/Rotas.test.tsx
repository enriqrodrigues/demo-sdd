import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import Rotas from './Rotas.tsx'

describe('NFR-4: rotas provisórias do frontend', () => {
  it.each([
    ['/cadastro', 'Cadastro'],
    ['/ativacao', 'Ativação da conta'],
    ['/login', 'Login'],
    ['/', 'Página inicial'],
    ['/perfil', 'Meu perfil'],
  ])('a rota %s renderiza a página "%s"', (caminho, titulo) => {
    render(
      <MemoryRouter initialEntries={[caminho]}>
        <Rotas />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(titulo)
  })
})
