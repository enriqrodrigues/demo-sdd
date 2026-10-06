import { beforeEach, describe, expect, it, vi } from 'vitest'
import { obterSessao } from './api/sessao.ts'
import { inicializar } from './inicializacao.ts'

vi.mock('./api/sessao.ts', () => ({ obterSessao: vi.fn() }))

const obterSessaoMock = vi.mocked(obterSessao)

describe('AD-7: sessão na inicialização da SPA', () => {
  beforeEach(() => {
    obterSessaoMock.mockReset()
  })

  it('chama obterSessao exatamente uma vez', async () => {
    obterSessaoMock.mockResolvedValue({ autenticado: false })

    await inicializar()

    expect(obterSessaoMock).toHaveBeenCalledTimes(1)
  })

  it('resolve mesmo quando obterSessao rejeita', async () => {
    obterSessaoMock.mockRejectedValue(new Error('backend fora'))

    await expect(inicializar()).resolves.toBeUndefined()
    expect(obterSessaoMock).toHaveBeenCalledTimes(1)
  })
})
