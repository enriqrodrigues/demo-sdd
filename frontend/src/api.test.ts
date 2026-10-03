import { activate, register } from './api';
import { mockFetchNetworkError, mockFetchResponse, problem } from './test/fetchMock';

afterEach(() => {
  vi.unstubAllGlobals();
});

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
