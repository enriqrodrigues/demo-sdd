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

export type MockReply = { status: number; body?: unknown } | 'network-error';

/**
 * fetch que responde por rota (`'MÉTODO /url'`). Uma lista de respostas é usada
 * em ordem, repetindo a última. Rotas não previstas viram falha de rede e ficam
 * registradas nas chamadas do mock.
 */
export function mockApi(routes: Record<string, MockReply | MockReply[]>) {
  const queues = new Map(Object.entries(routes).map(([route, reply]) => [route, [reply].flat()]));
  const fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
    const queue = queues.get(`${init?.method ?? 'GET'} ${url}`);
    const reply = queue && (queue.length > 1 ? queue.shift() : queue[0]);
    if (!reply || reply === 'network-error') {
      throw new TypeError('Failed to fetch');
    }
    return new Response(reply.body === undefined ? null : JSON.stringify(reply.body), {
      status: reply.status,
      headers: { 'Content-Type': reply.status >= 400 ? 'application/problem+json' : 'application/json' },
    });
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

/** Rotas chamadas pelo mock, no formato `'MÉTODO /url'`. */
export function calledRoutes(fetchMock: ReturnType<typeof mockApi>) {
  return fetchMock.mock.calls.map(([url, init]) => `${init?.method ?? 'GET'} ${url}`);
}
