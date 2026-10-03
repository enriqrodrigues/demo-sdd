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

async function postJson<T>(url: string, payload: unknown): Promise<ApiResult<T>> {
  let response: Response;
  try {
    response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify(payload),
    });
  } catch {
    return { ok: false, problem: NETWORK_PROBLEM };
  }
  if (!response.ok) {
    return { ok: false, problem: await toProblem(response) };
  }
  return { ok: true, data: (await response.json()) as T };
}

export type RegistrationPayload = Record<string, string>;

export function register(payload: RegistrationPayload): Promise<ApiResult<{ email: string }>> {
  return postJson('/api/registrations', payload);
}

export function activate(token: string): Promise<ApiResult<{ email: string }>> {
  return postJson('/api/activations', { token });
}
