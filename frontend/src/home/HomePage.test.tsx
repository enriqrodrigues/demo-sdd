import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { calledRoutes, mockApi, problem } from '../test/fetchMock';
import { renderApp } from '../test/renderWithRouter';

const USER = { name: 'Maria da Silva', email: 'maria@exemplo.com' };
const NO_SESSION = { status: 401, body: problem(401, 'UNAUTHENTICATED', 'Faça login para continuar.') };

afterEach(() => {
  vi.unstubAllGlobals();
});

test('sem sessão, a área interna leva ao login', async () => {
  mockApi({ 'GET /api/auth/me': NO_SESSION });

  renderApp('/inicio');

  expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument();
});

test('mostra "Carregando..." enquanto confere a sessão', () => {
  vi.stubGlobal('fetch', vi.fn(() => new Promise<Response>(() => {})));

  renderApp('/inicio');

  expect(screen.getByRole('status')).toHaveTextContent('Carregando...');
});

test('com sessão, saúda o usuário pelo nome e oferece sair', async () => {
  mockApi({ 'GET /api/auth/me': { status: 200, body: USER } });

  renderApp('/inicio');

  expect(await screen.findByRole('heading', { name: 'Olá, Maria da Silva!' })).toBeInTheDocument();
  expect(screen.getByText('maria@exemplo.com')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Sair' })).toBeInTheDocument();
});

test('sair chama o logout e leva ao login', async () => {
  const fetchMock = mockApi({
    'GET /api/auth/me': [{ status: 200, body: USER }, NO_SESSION],
    'POST /api/auth/logout': { status: 204 },
  });
  const user = userEvent.setup();
  renderApp('/inicio');

  await user.click(await screen.findByRole('button', { name: 'Sair' }));

  expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument();
  expect(calledRoutes(fetchMock)).toContain('POST /api/auth/logout');
});

test('falha no servidor ao conferir a sessão mostra o erro em vez de redirecionar', async () => {
  mockApi({ 'GET /api/auth/me': 'network-error' });

  renderApp('/inicio');

  expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível falar com o servidor');
});

test('a rota raiz leva à área interna', async () => {
  mockApi({ 'GET /api/auth/me': { status: 200, body: USER } });

  renderApp('/');

  expect(await screen.findByRole('heading', { name: 'Olá, Maria da Silva!' })).toBeInTheDocument();
});
