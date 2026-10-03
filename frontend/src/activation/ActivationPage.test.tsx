import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderApp } from '../test/renderWithRouter';
import { mockFetchResponse, problem } from '../test/fetchMock';

afterEach(() => {
  vi.unstubAllGlobals();
});

test('não chama a API antes do clique no botão', () => {
  const fetchMock = mockFetchResponse(200, { email: 'maria@exemplo.com' });

  renderApp('/ativar?token=abc123');

  expect(screen.getByRole('button', { name: 'Ativar minha conta' })).toBeInTheDocument();
  expect(fetchMock).not.toHaveBeenCalled();
});

test('ativação bem-sucedida', async () => {
  const fetchMock = mockFetchResponse(200, { email: 'maria@exemplo.com' });
  const user = userEvent.setup();
  renderApp('/ativar?token=abc123');

  await user.click(screen.getByRole('button', { name: 'Ativar minha conta' }));

  expect(await screen.findByRole('status')).toHaveTextContent('Conta ativada com sucesso!');
  expect(screen.getByText('maria@exemplo.com')).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Entrar' })).toHaveAttribute('href', '/login');
  expect(fetchMock).toHaveBeenCalledTimes(1);
  expect(JSON.parse(fetchMock.mock.calls[0][1].body as string)).toEqual({ token: 'abc123' });
});

test('link expirado orienta novo cadastro', async () => {
  mockFetchResponse(410, problem(410, 'TOKEN_EXPIRED', 'Este link de ativação expirou.'));
  const user = userEvent.setup();
  renderApp('/ativar?token=abc123');

  await user.click(screen.getByRole('button', { name: 'Ativar minha conta' }));

  expect(await screen.findByText('Este link de ativação expirou.')).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'novo cadastro' })).toHaveAttribute('href', '/cadastro');
});

test('link já utilizado', async () => {
  mockFetchResponse(409, problem(409, 'TOKEN_ALREADY_USED', 'Este link de ativação já foi utilizado.'));
  const user = userEvent.setup();
  renderApp('/ativar?token=abc123');

  await user.click(screen.getByRole('button', { name: 'Ativar minha conta' }));

  expect(await screen.findByRole('alert')).toHaveTextContent('Este link de ativação já foi utilizado.');
});

test('link inválido', async () => {
  mockFetchResponse(400, problem(400, 'INVALID_TOKEN', 'Este link de ativação é inválido.'));
  const user = userEvent.setup();
  renderApp('/ativar?token=adulterado');

  await user.click(screen.getByRole('button', { name: 'Ativar minha conta' }));

  expect(await screen.findByRole('alert')).toHaveTextContent('Este link de ativação é inválido.');
});

test('falha inesperada mostra a mensagem e permite tentar de novo', async () => {
  mockFetchResponse(500, {});
  const user = userEvent.setup();
  renderApp('/ativar?token=abc123');

  await user.click(screen.getByRole('button', { name: 'Ativar minha conta' }));

  expect(await screen.findByRole('alert')).toHaveTextContent('Ocorreu um erro inesperado');
  expect(screen.getByRole('button', { name: 'Ativar minha conta' })).toBeEnabled();
});

test('sem token na URL, o link é tratado como inválido', () => {
  const fetchMock = mockFetchResponse(200, {});

  renderApp('/ativar');

  expect(screen.getByRole('alert')).toHaveTextContent('Este link de ativação é inválido.');
  expect(screen.queryByRole('button', { name: 'Ativar minha conta' })).not.toBeInTheDocument();
  expect(fetchMock).not.toHaveBeenCalled();
});
