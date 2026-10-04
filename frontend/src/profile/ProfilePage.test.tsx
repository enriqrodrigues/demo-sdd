import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { calledRoutes, mockApi, problem, type MockReply } from '../test/fetchMock';
import { renderApp } from '../test/renderWithRouter';

const USER = { name: 'Maria da Silva', email: 'maria@exemplo.com' };
const NO_SESSION = { status: 401, body: problem(401, 'UNAUTHENTICATED', 'Faça login para continuar.') };

const PROFILE = {
  name: 'Maria da Silva',
  cpf: '52998224725',
  email: 'maria@exemplo.com',
  birthDate: '1990-05-20',
  phone: '11987654321',
  cep: '01310100',
  street: 'Avenida Paulista',
  number: '1000',
  complement: null,
  district: 'Bela Vista',
  city: 'São Paulo',
  state: 'SP',
};

afterEach(() => {
  vi.unstubAllGlobals();
});

/**
 * Abre /perfil com sessão e o perfil acima. `put` define as respostas do PUT e
 * `me`, as da sessão (por padrão, a sessão acaba depois de abrir o perfil).
 */
async function openProfile(put?: MockReply | MockReply[], me: MockReply[] = [{ status: 200, body: USER }, NO_SESSION]) {
  const fetchMock = mockApi({
    'GET /api/auth/me': me,
    'GET /api/profile': { status: 200, body: PROFILE },
    ...(put ? { 'PUT /api/profile': put } : {}),
  });
  renderApp('/perfil');
  await screen.findByRole('heading', { name: 'Meu perfil' });
  return { fetchMock, user: userEvent.setup() };
}

function putBody(fetchMock: ReturnType<typeof mockApi>) {
  const call = fetchMock.mock.calls.find(([url, init]) => url === '/api/profile' && init?.method === 'PUT');
  return call ? JSON.parse(call[1]!.body as string) : undefined;
}

// --- Visualização ---

test('dados pessoais aparecem formatados e não são campos editáveis', async () => {
  await openProfile();

  const personal = screen.getByRole('region', { name: 'Dados pessoais' });
  expect(within(personal).getByText('Maria da Silva')).toBeInTheDocument();
  expect(within(personal).getByText('529.982.247-25')).toBeInTheDocument();
  expect(within(personal).getByText('maria@exemplo.com')).toBeInTheDocument();
  expect(within(personal).getByText('20/05/1990')).toBeInTheDocument();
  expect(within(personal).queryByRole('textbox')).not.toBeInTheDocument();

  for (const label of [/nome/i, /cpf/i, /e-mail/i, /nascimento/i]) {
    expect(screen.queryByLabelText(label)).not.toBeInTheDocument();
  }
});

test('o formulário de contato vem preenchido e formatado', async () => {
  await openProfile();

  expect(screen.getByLabelText('Telefone (com DDD)')).toHaveValue('(11) 98765-4321');
  expect(screen.getByLabelText('CEP')).toHaveValue('01310-100');
  expect(screen.getByLabelText('Logradouro')).toHaveValue('Avenida Paulista');
  expect(screen.getByLabelText('Número')).toHaveValue('1000');
  expect(screen.getByLabelText(/Complemento/)).toHaveValue('');
  expect(screen.getByLabelText('Bairro')).toHaveValue('Bela Vista');
  expect(screen.getByLabelText('Cidade')).toHaveValue('São Paulo');
  expect(screen.getByLabelText('UF')).toHaveValue('SP');
});

test('CEP com 7 dígitos mostra o erro ao sair do campo, sem enviar', async () => {
  const { fetchMock, user } = await openProfile();
  const cep = screen.getByLabelText('CEP');

  await user.clear(cep);
  await user.type(cep, '2004002');
  await user.tab();

  expect(screen.getByText('O CEP deve ter 8 dígitos')).toBeInTheDocument();
  expect(cep).toHaveAttribute('aria-invalid', 'true');
  expect(calledRoutes(fetchMock)).not.toContain('PUT /api/profile');
});

test('formulário inválido não é enviado', async () => {
  const { fetchMock, user } = await openProfile();

  await user.clear(screen.getByLabelText('Cidade'));
  await user.click(screen.getByRole('button', { name: 'Salvar alterações' }));

  expect(screen.getByText('Campo obrigatório')).toBeInTheDocument();
  expect(screen.getByLabelText('Cidade')).toHaveFocus();
  expect(calledRoutes(fetchMock)).not.toContain('PUT /api/profile');
});

test('falha ao carregar o perfil mostra o erro', async () => {
  mockApi({ 'GET /api/auth/me': { status: 200, body: USER }, 'GET /api/profile': 'network-error' });

  renderApp('/perfil');

  expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível falar com o servidor');
});

// --- Salvar e descartar ---

test('salvar envia o contato, confirma e mostra os valores devolvidos', async () => {
  const updated = {
    ...PROFILE,
    phone: '2134567890',
    cep: '20040020',
    street: 'Rua da Assembleia',
    number: '10',
    complement: 'Sala 501',
    district: 'Centro',
    city: 'Rio de Janeiro',
    state: 'RJ',
  };
  const { fetchMock, user } = await openProfile({ status: 200, body: updated });

  const phone = screen.getByLabelText('Telefone (com DDD)');
  await user.clear(phone);
  await user.type(phone, '2134567890');
  await user.clear(screen.getByLabelText('CEP'));
  await user.type(screen.getByLabelText('CEP'), '20040020');
  await user.clear(screen.getByLabelText('Logradouro'));
  await user.type(screen.getByLabelText('Logradouro'), 'Rua da Assembleia');
  await user.clear(screen.getByLabelText('Número'));
  await user.type(screen.getByLabelText('Número'), '10');
  await user.type(screen.getByLabelText(/Complemento/), 'Sala 501');
  await user.clear(screen.getByLabelText('Bairro'));
  await user.type(screen.getByLabelText('Bairro'), 'Centro');
  await user.clear(screen.getByLabelText('Cidade'));
  await user.type(screen.getByLabelText('Cidade'), 'Rio de Janeiro');
  await user.selectOptions(screen.getByLabelText('UF'), 'RJ');
  await user.click(screen.getByRole('button', { name: 'Salvar alterações' }));

  expect(await screen.findByRole('status')).toHaveTextContent('Dados atualizados com sucesso.');
  expect(putBody(fetchMock)).toEqual({
    phone: '(21) 3456-7890',
    cep: '20040-020',
    street: 'Rua da Assembleia',
    number: '10',
    complement: 'Sala 501',
    district: 'Centro',
    city: 'Rio de Janeiro',
    state: 'RJ',
  });
  expect(phone).toHaveValue('(21) 3456-7890');
  expect(screen.getByLabelText('CEP')).toHaveValue('20040-020');
  expect(screen.getByLabelText(/Complemento/)).toHaveValue('Sala 501');
});

test('o envio nunca inclui nome, CPF, e-mail ou data de nascimento', async () => {
  const { fetchMock, user } = await openProfile({ status: 200, body: PROFILE });

  await user.click(screen.getByRole('button', { name: 'Salvar alterações' }));

  await screen.findByRole('status');
  expect(Object.keys(putBody(fetchMock)).sort()).toEqual(
    ['cep', 'city', 'complement', 'district', 'number', 'phone', 'state', 'street'],
  );
});

test('erros do servidor aparecem nos campos', async () => {
  const { user } = await openProfile({
    status: 400,
    body: problem(400, 'VALIDATION_ERROR', 'Um ou mais campos são inválidos', [
      { field: 'city', message: 'Máximo de 100 caracteres' },
    ]),
  });

  await user.click(screen.getByRole('button', { name: 'Salvar alterações' }));

  expect(await screen.findByText('Máximo de 100 caracteres')).toBeInTheDocument();
  expect(screen.getByLabelText('Cidade')).toHaveAttribute('aria-invalid', 'true');
  expect(screen.getByRole('alert')).toHaveTextContent('Corrija os campos destacados e tente novamente.');
  expect(screen.queryByText('Dados atualizados com sucesso.')).not.toBeInTheDocument();
});

test('outros erros do servidor aparecem como mensagem geral', async () => {
  const { user } = await openProfile('network-error');

  await user.click(screen.getByRole('button', { name: 'Salvar alterações' }));

  expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível falar com o servidor');
});

test('descartar restaura os valores carregados e limpa os erros', async () => {
  const { fetchMock, user } = await openProfile();

  await user.clear(screen.getByLabelText('CEP'));
  await user.type(screen.getByLabelText('CEP'), '2004002');
  await user.tab();
  await user.clear(screen.getByLabelText('Cidade'));
  await user.type(screen.getByLabelText('Cidade'), 'Niterói');
  await user.click(screen.getByRole('button', { name: 'Descartar alterações' }));

  expect(screen.getByLabelText('CEP')).toHaveValue('01310-100');
  expect(screen.getByLabelText('Cidade')).toHaveValue('São Paulo');
  expect(screen.queryByText('O CEP deve ter 8 dígitos')).not.toBeInTheDocument();
  expect(calledRoutes(fetchMock)).not.toContain('PUT /api/profile');
});

test('sessão expirada ao salvar leva ao login', async () => {
  const { user } = await openProfile(NO_SESSION);

  await user.click(screen.getByRole('button', { name: 'Salvar alterações' }));

  expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument();
});

test('sem sessão, /perfil leva ao login', async () => {
  const fetchMock = mockApi({ 'GET /api/auth/me': NO_SESSION });

  renderApp('/perfil');

  expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument();
  expect(calledRoutes(fetchMock)).not.toContain('GET /api/profile');
});

// --- Navegação ---

test('o perfil oferece o link de volta ao início', async () => {
  const { user } = await openProfile(undefined, [{ status: 200, body: USER }]);

  const back = screen.getByRole('link', { name: 'Voltar ao início' });
  expect(back).toHaveAttribute('href', '/inicio');

  await user.click(back);

  expect(await screen.findByRole('heading', { name: 'Olá, Maria da Silva!' })).toBeInTheDocument();
});
