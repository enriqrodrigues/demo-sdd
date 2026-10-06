export type CampoErro = {
  campo: string
  codigo: string
  mensagem: string
}

const CODIGO_FALLBACK = 'ERRO_INTERNO'
const MENSAGEM_FALLBACK = 'Ocorreu um erro inesperado. Tente novamente mais tarde.'

export class ErroApi extends Error {
  readonly status: number
  readonly codigo: string
  readonly mensagem: string
  readonly campos: CampoErro[]

  constructor(status: number, codigo: string, mensagem: string, campos: CampoErro[]) {
    super(mensagem)
    this.name = 'ErroApi'
    this.status = status
    this.codigo = codigo
    this.mensagem = mensagem
    this.campos = campos
  }
}

export type Metodo = 'GET' | 'POST' | 'PUT'

function lerTokenCsrf(): string | undefined {
  const prefixo = 'XSRF-TOKEN='
  const par = document.cookie.split('; ').find((item) => item.startsWith(prefixo))
  return par === undefined ? undefined : decodeURIComponent(par.slice(prefixo.length))
}

function erroFallback(status: number): ErroApi {
  return new ErroApi(status, CODIGO_FALLBACK, MENSAGEM_FALLBACK, [])
}

async function lerErro(resposta: Response): Promise<ErroApi> {
  let corpo: unknown
  try {
    corpo = await resposta.json()
  } catch {
    return erroFallback(resposta.status)
  }
  if (typeof corpo !== 'object' || corpo === null) {
    return erroFallback(resposta.status)
  }
  const { codigo, mensagem, campos } = corpo as Record<string, unknown>
  if (typeof codigo !== 'string' || typeof mensagem !== 'string') {
    return erroFallback(resposta.status)
  }
  return new ErroApi(resposta.status, codigo, mensagem, Array.isArray(campos) ? (campos as CampoErro[]) : [])
}

export async function requisitar<T>(metodo: Metodo, caminho: string, corpo?: unknown): Promise<T> {
  const headers: Record<string, string> = {}
  const token = lerTokenCsrf()
  if (token !== undefined) {
    headers['X-XSRF-TOKEN'] = token
  }
  const temCorpo = corpo !== undefined
  if (temCorpo) {
    headers['Content-Type'] = 'application/json'
  }

  let resposta: Response
  try {
    resposta = await fetch(caminho, {
      method: metodo,
      headers,
      body: temCorpo ? JSON.stringify(corpo) : undefined,
      credentials: 'same-origin',
    })
  } catch {
    throw erroFallback(0)
  }

  if (!resposta.ok) {
    throw await lerErro(resposta)
  }
  if (resposta.status === 204) {
    return undefined as T
  }
  const texto = await resposta.text()
  return (texto === '' ? undefined : JSON.parse(texto)) as T
}
