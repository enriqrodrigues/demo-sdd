import { act } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { inicializar } from './inicializacao.ts'

vi.mock('./inicializacao.ts', () => ({ inicializar: vi.fn() }))

describe('AD-7: a SPA só renderiza depois da inicialização', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('mantém #root vazio enquanto inicializar() está pendente e renderiza depois', async () => {
    let concluir!: () => void
    vi.mocked(inicializar).mockReturnValue(
      new Promise<void>((resolve) => {
        concluir = resolve
      }),
    )
    const raiz = document.createElement('div')
    raiz.id = 'root'
    document.body.appendChild(raiz)

    await import('./main.tsx')
    await act(async () => {
      await Promise.resolve()
    })

    expect(inicializar).toHaveBeenCalledTimes(1)
    expect(raiz).toBeEmptyDOMElement()

    await act(async () => {
      concluir()
      await Promise.resolve()
    })

    expect(raiz).not.toBeEmptyDOMElement()
  })
})
