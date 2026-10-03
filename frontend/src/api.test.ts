import { activate, register } from './api';
import { mockFetchNetworkError, mockFetchResponse, problem } from './test/fetchMock';
import { TEST_XSRF_TOKEN } from './test/setup';

afterEach(() => {
  vi.unstubAllGlobals();
});

function clearXsrfCookie() {
  document.cookie = 'XSRF-TOKEN=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
}

/** fetch que emite o cookie em /api/auth/csrf (como o servidor) e responde 201 ao resto. */
function mockFetchIssuingXsrfCookie(issuedToken: string) {
  const fetchMock = vi.fn(async (url: string, _init?: RequestInit) => {
    if (url === '/api/auth/csrf') {
      document.cookie = `XSRF-TOKEN=${issuedToken}; path=/`;
      return new Response(null, { status: 204 });
    }
    return new Response(JSON.stringify({ email: 'maria@exemplo.com' }), { status: 201 });
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

// --- Token anti-CSRF ---

test('POST envia o token do cookie no cabeçalho X-XSRF-TOKEN', async () => {
  const fetchMock = mockFetchResponse(201, { email: 'maria@exemplo.com' });

  await register({ email: 'maria@exemplo.com' });

  expect(fetchMock).toHaveBeenCalledTimes(1);
  const init = fetchMock.mock.calls[0][1] as RequestInit;
  expect(init.headers).toEqual(expect.objectContaining({ 'X-XSRF-TOKEN': TEST_XSRF_TOKEN }));
  expect(init.credentials).toBe('same-origin');
});

test('sem o cookie, busca o token em /api/auth/csrf antes do POST', async () => {
  clearXsrfCookie();
  const fetchMock = mockFetchIssuingXsrfCookie('token-novo');

  const result = await activate('tok123');

  expect(result.ok).toBe(true);
  expect(fetchMock.mock.calls.map((call) => call[0])).toEqual(['/api/auth/csrf', '/api/activations']);
  const init = fetchMock.mock.calls[1][1] as RequestInit;
  expect(init.headers).toEqual(expect.objectContaining({ 'X-XSRF-TOKEN': 'token-novo' }));
});

test('com o cookie presente, não busca o token de novo', async () => {
  clearXsrfCookie();
  const fetchMock = mockFetchIssuingXsrfCookie('token-novo');

  await register({});
  await register({});

  expect(fetchMock.mock.calls.map((call) => call[0])).toEqual([
    '/api/auth/csrf',
    '/api/registrations',
    '/api/registrations',
  ]);
});

test('falha de rede ao buscar o token vira NETWORK_ERROR', async () => {
  clearXsrfCookie();
  mockFetchNetworkError();

  const result = await register({});

  expect(result.ok).toBe(false);
  if (result.ok) return;
  expect(result.problem.code).toBe('NETWORK_ERROR');
});

// --- Respostas de cadastro e ativação ---

test('201 no cadastro devolve o e-mail', async () => {
  const fetchMock = mockFetchResponse(201, { email: 'maria@exemplo.com' });

  const result = await register({ email: 'maria@exemplo.com' });

  expect(result).toEqual({ ok: true, data: { email: 'maria@exemplo.com' } });
  expect(fetchMock).toHaveBeenCalledWith('/api/registrations', expect.objectContaining({ method: 'POST' }));
});

test('400 mapeia errors[] por campo, acumulando mensagens', async () => {
  mockFetchResponse(
    400,
    problem(400, 'VALIDATION_ERROR', 'Um ou mais campos são inválidos', [
      { field: 'cpf', message: 'CPF inválido' },
      { field: 'password', message: 'A senha deve conter ao menos um dígito' },
      { field: 'password', message: 'A senha deve conter ao menos um caractere especial' },
    ]),
  );

  const result = await register({});

  expect(result.ok).toBe(false);
  if (result.ok) return;
  expect(result.problem.code).toBe('VALIDATION_ERROR');
  expect(result.problem.fieldErrors).toEqual({
    cpf: ['CPF inválido'],
    password: ['A senha deve conter ao menos um dígito', 'A senha deve conter ao menos um caractere especial'],
  });
});

test('409 ALREADY_REGISTERED traz o campo em conflito', async () => {
  mockFetchResponse(
    409,
    problem(409, 'ALREADY_REGISTERED', 'Já existe uma conta com estes dados.', [
      { field: 'email', message: 'E-mail já cadastrado' },
    ]),
  );

  const result = await register({});

  expect(result.ok).toBe(false);
  if (result.ok) return;
  expect(result.problem.status).toBe(409);
  expect(result.problem.fieldErrors.email).toEqual(['E-mail já cadastrado']);
});

test('409 PENDING_ACTIVATION traz a mensagem geral', async () => {
  mockFetchResponse(409, problem(409, 'PENDING_ACTIVATION', 'Verifique seu e-mail para ativar a conta.'));

  const result = await register({});

  expect(result.ok).toBe(false);
  if (result.ok) return;
  expect(result.problem.code).toBe('PENDING_ACTIVATION');
  expect(result.problem.detail).toBe('Verifique seu e-mail para ativar a conta.');
});

test('503 EMAIL_UNAVAILABLE', async () => {
  mockFetchResponse(503, problem(503, 'EMAIL_UNAVAILABLE', 'Tente novamente em alguns minutos.'));

  const result = await register({});

  expect(result.ok).toBe(false);
  if (result.ok) return;
  expect(result.problem.code).toBe('EMAIL_UNAVAILABLE');
});

test('falha de rede vira NETWORK_ERROR', async () => {
  mockFetchNetworkError();

  const result = await activate('abc');

  expect(result.ok).toBe(false);
  if (result.ok) return;
  expect(result.problem.code).toBe('NETWORK_ERROR');
});

test('ativação envia o token no corpo', async () => {
  const fetchMock = mockFetchResponse(200, { email: 'maria@exemplo.com' });

  await activate('tok123');

  expect(fetchMock).toHaveBeenCalledWith(
    '/api/activations',
    expect.objectContaining({ method: 'POST', body: JSON.stringify({ token: 'tok123' }) }),
  );
});
