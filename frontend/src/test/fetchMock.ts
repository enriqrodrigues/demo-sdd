import { vi } from 'vitest';

/** Substitui o fetch global por um mock que responde com o status e o corpo JSON informados. */
export function mockFetchResponse(status: number, body: unknown) {
  const fetchMock = vi.fn().mockResolvedValue(
    new Response(JSON.stringify(body), {
      status,
      headers: { 'Content-Type': status >= 400 ? 'application/problem+json' : 'application/json' },
    }),
  );
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

export function mockFetchNetworkError() {
  const fetchMock = vi.fn().mockRejectedValue(new TypeError('Failed to fetch'));
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

export function problem(status: number, code: string, detail: string, errors?: { field: string; message: string }[]) {
  return { type: 'about:blank', title: 'Erro', status, code, detail, ...(errors ? { errors } : {}) };
}
