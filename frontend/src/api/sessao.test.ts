import { afterEach, describe, expect, it, vi } from 'vitest'
import { obterSessao } from './sessao.ts'

describe('AD-7: obterSessao', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('faz GET em /api/auth/sessao e devolve o JSON', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ autenticado: false }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await expect(obterSessao()).resolves.toEqual({ autenticado: false })

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [caminho, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(caminho).toBe('/api/auth/sessao')
    expect(init.method).toBe('GET')
  })
})
