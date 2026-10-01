import type { ErroCampo } from './tipos';

export class ApiError extends Error {
  readonly status: number;
  readonly codigo?: string;
  readonly erros: ErroCampo[];

  constructor(status: number, mensagem: string, codigo?: string, erros: ErroCampo[] = []) {
    super(mensagem);
    this.name = 'ApiError';
    this.status = status;
    this.codigo = codigo;
    this.erros = erros;
  }
}

type Metodo = 'GET' | 'POST' | 'PUT';

function urlAbsoluta(caminho: string): string {
  return new URL(caminho, window.location.origin).toString();
}

function lerCookie(nome: string): string | undefined {
  const par = document.cookie.split('; ').find((c) => c.startsWith(`${nome}=`));
  const valor = par?.substring(nome.length + 1);
  return valor ? decodeURIComponent(valor) : undefined;
}

async function obterTokenCsrf(): Promise<string> {
  let token = lerCookie('XSRF-TOKEN');
  if (!token) {
    await fetch(urlAbsoluta('/api/csrf'), { credentials: 'same-origin' });
    token = lerCookie('XSRF-TOKEN');
  }
  return token ?? '';
}

export async function requisitar<T>(metodo: Metodo, caminho: string, corpo?: unknown): Promise<T> {
  const cabecalhos: Record<string, string> = { Accept: 'application/json' };
  if (corpo !== undefined) {
    cabecalhos['Content-Type'] = 'application/json';
  }
  if (metodo !== 'GET') {
    cabecalhos['X-XSRF-TOKEN'] = await obterTokenCsrf();
  }

  const resposta = await fetch(urlAbsoluta(caminho), {
    method: metodo,
    headers: cabecalhos,
    body: corpo === undefined ? undefined : JSON.stringify(corpo),
    credentials: 'same-origin',
  });

  if (!resposta.ok) {
    let problema: { detail?: string; codigo?: string; erros?: ErroCampo[] } = {};
    try {
      problema = await resposta.json();
    } catch {
      // resposta sem corpo JSON
    }
    throw new ApiError(resposta.status, problema.detail ?? 'Erro inesperado', problema.codigo, problema.erros ?? []);
  }

  if (resposta.status === 204) {
    return undefined as T;
  }
  return (await resposta.json()) as T;
}
