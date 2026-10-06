import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ErroApi, requisitar } from './cliente.ts'

const MENSAGEM_FALLBACK = 'Ocorreu um erro inesperado. Tente novamente mais tarde.'

function definirCookie(nome: string, valor: string) {
  document.cookie = `${nome}=${valor}; path=/`
}

function apagarCookie(nome: string) {
  document.cookie = `${nome}=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT`
}

function respostaJson(status: number, corpo: unknown): Response {
  return new Response(JSON.stringify(corpo), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function headersDaChamada(fetchMock: ReturnType<typeof vi.fn>, indice = 0): Record<string, string> {
  const init = fetchMock.mock.calls[indice][1] as RequestInit
  return init.headers as Record<string, string>
}

async function capturarErro(promessa: Promise<unknown>): Promise<ErroApi> {
  try {
    await promessa
  } catch (erro) {
    expect(erro).toBeInstanceOf(ErroApi)
    return erro as ErroApi
  }
  throw new Error('a requisição deveria ter falhado')
}

describe('AD-7, AD-11: cliente HTTP do frontend', () => {
  let fetchMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    apagarCookie('XSRF-TOKEN')
    apagarCookie('outro')
    apagarCookie('mais')
  })

  it('envia o X-XSRF-TOKEN lido do cookie e o corpo em JSON', async () => {
    definirCookie('outro', '1')
    definirCookie('XSRF-TOKEN', 'abc')
    definirCookie('mais', '2')
    fetchMock.mockResolvedValue(respostaJson(201, { email: 'ana@exemplo.com' }))

    await requisitar('POST', '/api/cadastro', { email: 'ana@exemplo.com' })

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [caminho, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(caminho).toBe('/api/cadastro')
    expect(init.method).toBe('POST')
    expect(init.credentials).toBe('same-origin')
    expect(init.body).toBe(JSON.stringify({ email: 'ana@exemplo.com' }))
    expect(init.headers).toEqual({ 'X-XSRF-TOKEN': 'abc', 'Content-Type': 'application/json' })
  })

  it('decodifica o valor do cookie', async () => {
    definirCookie('XSRF-TOKEN', encodeURIComponent('a=b/c'))
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))

    await requisitar('POST', '/api/auth/logout')

    expect(headersDaChamada(fetchMock)['X-XSRF-TOKEN']).toBe('a=b/c')
  })

  it('lê o token atual a cada requisição', async () => {
    fetchMock.mockImplementation(() => Promise.resolve(new Response(null, { status: 204 })))

    definirCookie('XSRF-TOKEN', 'primeiro')
    await requisitar('POST', '/api/auth/logout')
    definirCookie('XSRF-TOKEN', 'segundo')
    await requisitar('POST', '/api/auth/logout')

    expect(headersDaChamada(fetchMock, 0)['X-XSRF-TOKEN']).toBe('primeiro')
    expect(headersDaChamada(fetchMock, 1)['X-XSRF-TOKEN']).toBe('segundo')
  })

  it('sem cookie, a requisição sai sem o header e sem Content-Type quando não há corpo', async () => {
    fetchMock.mockResolvedValue(respostaJson(200, { autenticado: false }))

    await requisitar('GET', '/api/auth/sessao')

    expect(headersDaChamada(fetchMock)).toEqual({})
  })

  it('devolve o JSON em 200', async () => {
    fetchMock.mockResolvedValue(respostaJson(200, { autenticado: false }))

    await expect(requisitar('GET', '/api/auth/sessao')).resolves.toEqual({ autenticado: false })
  })

  it('devolve undefined em 204 sem corpo', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))

    await expect(requisitar('POST', '/api/auth/logout')).resolves.toBeUndefined()
  })

  it('devolve undefined em 200 com corpo vazio', async () => {
    fetchMock.mockResolvedValue(new Response('', { status: 200 }))

    await expect(requisitar('GET', '/api/auth/sessao')).resolves.toBeUndefined()
  })

  it('lança ErroApi com status e os campos do envelope', async () => {
    const campos = [{ campo: 'email', codigo: 'EMAIL_DUPLICADO', mensagem: 'E-mail já cadastrado' }]
    fetchMock.mockResolvedValue(
      respostaJson(409, { codigo: 'EMAIL_DUPLICADO', mensagem: 'E-mail já cadastrado', campos }),
    )

    const erro = await capturarErro(requisitar('POST', '/api/cadastro', {}))

    expect(erro.status).toBe(409)
    expect(erro.codigo).toBe('EMAIL_DUPLICADO')
    expect(erro.mensagem).toBe('E-mail já cadastrado')
    expect(erro.campos).toEqual(campos)
  })

  it('usa campos vazio quando o envelope não traz um array', async () => {
    fetchMock.mockResolvedValue(
      respostaJson(403, { codigo: 'CSRF_INVALIDO', mensagem: 'Sua sessão de navegação expirou. Recarregue a página.' }),
    )

    const erro = await capturarErro(requisitar('POST', '/api/cadastro', {}))

    expect(erro.status).toBe(403)
    expect(erro.codigo).toBe('CSRF_INVALIDO')
    expect(erro.campos).toEqual([])
  })

  it.each([
    ['HTML', () => new Response('<html>Bad Gateway</html>', { status: 502 })],
    ['JSON sem codigo', () => respostaJson(502, { erro: 'x' })],
  ])('sem envelope (%s), lança ERRO_INTERNO com a mensagem do catálogo', async (_descricao, resposta) => {
    fetchMock.mockResolvedValue(resposta())

    const erro = await capturarErro(requisitar('GET', '/api/auth/sessao'))

    expect(erro.status).toBe(502)
    expect(erro.codigo).toBe('ERRO_INTERNO')
    expect(erro.mensagem).toBe(MENSAGEM_FALLBACK)
    expect(erro.campos).toEqual([])
  })

  it('em falha de rede, lança ERRO_INTERNO com status 0', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))

    const erro = await capturarErro(requisitar('GET', '/api/auth/sessao'))

    expect(erro.status).toBe(0)
    expect(erro.codigo).toBe('ERRO_INTERNO')
    expect(erro.mensagem).toBe(MENSAGEM_FALLBACK)
    expect(erro.campos).toEqual([])
  })
})
