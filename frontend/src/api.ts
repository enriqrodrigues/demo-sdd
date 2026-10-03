// Cliente da API REST. Erros do backend chegam como ProblemDetail (RFC 9457)
// com as extensões `code` e `errors[]` (ver design D6).

export type ApiProblem = {
  status: number;
  code: string;
  title: string;
  detail: string;
  /** Mensagens por campo; um campo pode ter mais de uma (ex.: critérios de senha). */
  fieldErrors: Record<string, string[]>;
};

export type ApiResult<T> = { ok: true; data: T } | { ok: false; problem: ApiProblem };

const NETWORK_PROBLEM: ApiProblem = {
  status: 0,
  code: 'NETWORK_ERROR',
  title: 'Sem conexão',
  detail: 'Não foi possível falar com o servidor. Verifique sua conexão e tente novamente.',
  fieldErrors: {},
};

type ProblemBody = {
  code?: string;
  title?: string;
  detail?: string;
  errors?: { field: string; message: string }[];
};

async function toProblem(response: Response): Promise<ApiProblem> {
  let body: ProblemBody = {};
  try {
    body = (await response.json()) as ProblemBody;
  } catch {
    // Corpo vazio ou não-JSON: usa os valores padrão abaixo.
  }
  const fieldErrors: Record<string, string[]> = {};
  for (const { field, message } of body.errors ?? []) {
    (fieldErrors[field] ??= []).push(message);
  }
  return {
    status: response.status,
    code: body.code ?? 'UNEXPECTED_ERROR',
    title: body.title ?? 'Erro inesperado',
    detail: body.detail ?? 'Ocorreu um erro inesperado. Tente novamente.',
    fieldErrors,
  };
}

// Proteção CSRF no padrão SPA (design D4): o servidor emite o token no cookie
// XSRF-TOKEN e a interface o devolve no cabeçalho X-XSRF-TOKEN.
const XSRF_COOKIE = 'XSRF-TOKEN';
const XSRF_HEADER = 'X-XSRF-TOKEN';
const CSRF_URL = '/api/auth/csrf';

type Method = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

function readCookie(name: string): string | null {
  for (const entry of document.cookie.split(';')) {
    const [key, ...value] = entry.trim().split('=');
    if (key === name) {
      const decoded = decodeURIComponent(value.join('='));
      return decoded === '' ? null : decoded;
    }
  }
  return null;
}

/** Token anti-CSRF do cookie; se ainda não existe (ou foi apagado no logout), pede um ao servidor. */
async function xsrfToken(): Promise<string | null> {
  const current = readCookie(XSRF_COOKIE);
  if (current) {
    return current;
  }
  await fetch(CSRF_URL, { method: 'GET', credentials: 'same-origin' });
  return readCookie(XSRF_COOKIE);
}

async function request<T>(method: Method, url: string, payload?: unknown): Promise<ApiResult<T>> {
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (payload !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  let response: Response;
  try {
    if (method !== 'GET') {
      const token = await xsrfToken();
      if (token) {
        headers[XSRF_HEADER] = token;
      }
    }
    response = await fetch(url, {
      method,
      headers,
      credentials: 'same-origin',
      body: payload === undefined ? undefined : JSON.stringify(payload),
    });
  } catch {
    return { ok: false, problem: NETWORK_PROBLEM };
  }
  if (!response.ok) {
    return { ok: false, problem: await toProblem(response) };
  }
  if (response.status === 204) {
    return { ok: true, data: undefined as T };
  }
  return { ok: true, data: (await response.json()) as T };
}

export type RegistrationPayload = Record<string, string>;

export function register(payload: RegistrationPayload): Promise<ApiResult<{ email: string }>> {
  return request('POST', '/api/registrations', payload);
}

export function activate(token: string): Promise<ApiResult<{ email: string }>> {
  return request('POST', '/api/activations', { token });
}

export type CurrentUser = { name: string; email: string };

/** 401 INVALID_CREDENTIALS para credenciais inválidas; 403 ACCOUNT_PENDING para conta não ativada. */
export function login(email: string, password: string): Promise<ApiResult<CurrentUser>> {
  return request('POST', '/api/auth/login', { email, password });
}

export function logout(): Promise<ApiResult<void>> {
  return request('POST', '/api/auth/logout');
}

/** Usuário da sessão atual; 401 UNAUTHENTICATED quando não há sessão válida. */
export function me(): Promise<ApiResult<CurrentUser>> {
  return request('GET', '/api/auth/me');
}
