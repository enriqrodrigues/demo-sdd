import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { calledRoutes, mockApi, problem } from '../test/fetchMock';
import { renderApp } from '../test/renderWithRouter';

const USER = { name: 'Maria da Silva', email: 'maria@exemplo.com' };
const NO_SESSION = { status: 401, body: problem(401, 'UNAUTHENTICATED', 'Faça login para continuar.') };

afterEach(() => {
  vi.unstubAllGlobals();
});

async function fillAndSubmit(user: ReturnType<typeof userEvent.setup>, email: string, password: string) {
  if (email) await user.type(screen.getByLabelText('E-mail'), email);
  if (password) await user.type(screen.getByLabelText('Senha'), password);
  await user.click(screen.getByRole('button', { name: 'Entrar' }));
}

test('visitante vê e-mail, senha e o link para o cadastro', async () => {
  mockApi({ 'GET /api/auth/me': NO_SESSION });

  renderApp('/login');

  expect(screen.getByRole('heading', { name: 'Entrar' })).toBeInTheDocument();
  expect(screen.getByLabelText('E-mail')).toBeInTheDocument();
  expect(screen.getByLabelText('Senha')).toHaveAttribute('type', 'password');
  expect(screen.getByRole('link', { name: 'Cadastre-se' })).toHaveAttribute('href', '/cadastro');
});

test('e-mail e senha são obrigatórios e não chamam o login', async () => {
  const fetchMock = mockApi({ 'GET /api/auth/me': NO_SESSION });
  const user = userEvent.setup();
  renderApp('/login');

  await fillAndSubmit(user, '', '');

  expect(screen.getAllByText('Campo obrigatório')).toHaveLength(2);
  expect(screen.getByLabelText('E-mail')).toHaveAttribute('aria-invalid', 'true');
  expect(screen.getByLabelText('E-mail')).toHaveFocus();
  expect(calledRoutes(fetchMock)).not.toContain('POST /api/auth/login');
});

test('login bem-sucedido leva à área interna com o nome do usuário', async () => {
  const fetchMock = mockApi({
    'GET /api/auth/me': [NO_SESSION, { status: 200, body: USER }],
    'POST /api/auth/login': { status: 200, body: USER },
  });
  const user = userEvent.setup();
  renderApp('/login');

  await fillAndSubmit(user, 'maria@exemplo.com', 'Segura@123');

  expect(await screen.findByRole('heading', { name: 'Olá, Maria da Silva!' })).toBeInTheDocument();
  const loginCall = fetchMock.mock.calls.find(([url]) => url === '/api/auth/login')!;
  expect(JSON.parse(loginCall[1]!.body as string)).toEqual({ email: 'maria@exemplo.com', password: 'Segura@123' });
});

test('credenciais inválidas mostram a mensagem genérica e limpam a senha', async () => {
  mockApi({
    'GET /api/auth/me': NO_SESSION,
    'POST /api/auth/login': { status: 401, body: problem(401, 'INVALID_CREDENTIALS', 'E-mail ou senha inválidos') },
  });
  const user = userEvent.setup();
  renderApp('/login');

  await fillAndSubmit(user, 'maria@exemplo.com', 'Errada@123');

  expect(await screen.findByRole('alert')).toHaveTextContent('E-mail ou senha inválidos');
  expect(screen.getByLabelText('Senha')).toHaveValue('');
  expect(screen.getByLabelText('E-mail')).toHaveValue('maria@exemplo.com');
});

test('conta pendente recebe a orientação de ativação pelo e-mail', async () => {
  const detail = 'Sua conta ainda não foi ativada. Use o link enviado para o seu e-mail para ativá-la.';
  mockApi({
    'GET /api/auth/me': NO_SESSION,
    'POST /api/auth/login': { status: 403, body: problem(403, 'ACCOUNT_PENDING', detail) },
  });
  const user = userEvent.setup();
  renderApp('/login');

  await fillAndSubmit(user, 'maria@exemplo.com', 'Segura@123');

  const alert = await screen.findByRole('alert');
  expect(alert).toHaveTextContent(detail);
  expect(alert).toHaveClass('alert--info');
  expect(screen.getByRole('heading', { name: 'Entrar' })).toBeInTheDocument();
});

test('mostra o carregamento e bloqueia novo envio enquanto entra', async () => {
  let finishLogin: (response: Response) => void = () => {};
  vi.stubGlobal(
    'fetch',
    vi.fn((url: string) =>
      url === '/api/auth/login'
        ? new Promise<Response>((resolve) => (finishLogin = resolve))
        : Promise.resolve(new Response(JSON.stringify(NO_SESSION.body), { status: 401 })),
    ),
  );
  const user = userEvent.setup();
  renderApp('/login');

  await fillAndSubmit(user, 'maria@exemplo.com', 'Segura@123');

  expect(await screen.findByRole('button', { name: 'Entrando...' })).toBeDisabled();
  finishLogin(new Response(JSON.stringify(problem(401, 'INVALID_CREDENTIALS', 'E-mail ou senha inválidos')), { status: 401 }));
  expect(await screen.findByRole('button', { name: 'Entrar' })).toBeEnabled();
});

test('usuário já autenticado que abre o login vai para a área interna', async () => {
  mockApi({ 'GET /api/auth/me': { status: 200, body: USER } });

  renderApp('/login');

  expect(await screen.findByRole('heading', { name: 'Olá, Maria da Silva!' })).toBeInTheDocument();
});

test('depois do logout mostra o aviso "Você saiu da sua conta."', async () => {
  mockApi({
    'GET /api/auth/me': [{ status: 200, body: USER }, NO_SESSION],
    'POST /api/auth/logout': { status: 204 },
  });
  const user = userEvent.setup();
  renderApp('/inicio');

  await user.click(await screen.findByRole('button', { name: 'Sair' }));

  expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument();
  expect(screen.getByRole('status')).toHaveTextContent('Você saiu da sua conta.');
});

test('o aviso de logout não aparece num acesso comum ao login', async () => {
  mockApi({ 'GET /api/auth/me': NO_SESSION });

  renderApp('/login');

  expect(screen.queryByText('Você saiu da sua conta.')).not.toBeInTheDocument();
});
