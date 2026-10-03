import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderApp } from '../test/renderWithRouter';
import { mockFetchNetworkError, mockFetchResponse, problem } from '../test/fetchMock';

afterEach(() => {
  vi.unstubAllGlobals();
});

async function fillValidForm(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('Nome completo'), 'Maria da Silva');
  await user.type(screen.getByLabelText('CPF'), '52998224725');
  await user.type(screen.getByLabelText('E-mail'), 'maria@exemplo.com');
  await user.type(screen.getByLabelText('Data de nascimento'), '1990-05-20');
  await user.type(screen.getByLabelText('Senha'), 'Segura@123');
  await user.type(screen.getByLabelText('Telefone (com DDD)'), '11987654321');
  await user.type(screen.getByLabelText('CEP'), '01310100');
  await user.type(screen.getByLabelText('Logradouro'), 'Avenida Paulista');
  await user.type(screen.getByLabelText('Número'), '1000');
  await user.type(screen.getByLabelText('Bairro'), 'Bela Vista');
  await user.type(screen.getByLabelText('Cidade'), 'São Paulo');
  await user.selectOptions(screen.getByLabelText('UF'), 'SP');
}

describe('formulário de cadastro', () => {
  test('exibe todos os campos, com complemento opcional', () => {
    renderApp('/cadastro');

    for (const label of [
      'Nome completo', 'CPF', 'E-mail', 'Data de nascimento', 'Senha', 'Telefone (com DDD)',
      'CEP', 'Logradouro', 'Número', 'Bairro', 'Cidade', 'UF',
    ]) {
      expect(screen.getByLabelText(label)).toBeInTheDocument();
    }
    expect(screen.getByLabelText(/Complemento/)).toBeInTheDocument();
    expect(screen.getByText('(opcional)')).toBeInTheDocument();
  });

  test('a rota raiz redireciona para o cadastro', () => {
    renderApp('/');
    expect(screen.getByRole('heading', { name: 'Crie sua conta' })).toBeInTheDocument();
  });

  test('aplica máscaras de CPF, telefone (fixo e celular) e CEP', async () => {
    const user = userEvent.setup();
    renderApp('/cadastro');

    await user.type(screen.getByLabelText('CPF'), '52998224725');
    await user.type(screen.getByLabelText('Telefone (com DDD)'), '1134567890');
    await user.type(screen.getByLabelText('CEP'), '01310100');

    expect(screen.getByLabelText('CPF')).toHaveValue('529.982.247-25');
    expect(screen.getByLabelText('Telefone (com DDD)')).toHaveValue('(11) 3456-7890');
    expect(screen.getByLabelText('CEP')).toHaveValue('01310-100');

    await user.type(screen.getByLabelText('Telefone (com DDD)'), '1');
    expect(screen.getByLabelText('Telefone (com DDD)')).toHaveValue('(11) 34567-8901');
  });
});

describe('validação em tempo real', () => {
  test('CPF inválido mostra erro ao sair do campo, sem enviar', async () => {
    const fetchMock = mockFetchResponse(201, {});
    const user = userEvent.setup();
    renderApp('/cadastro');

    await user.type(screen.getByLabelText('CPF'), '52998224726');
    expect(screen.queryByText('CPF inválido')).not.toBeInTheDocument();
    await user.tab();

    expect(screen.getByText('CPF inválido')).toBeInTheDocument();
    expect(screen.getByLabelText('CPF')).toHaveAttribute('aria-invalid', 'true');
    expect(fetchMock).not.toHaveBeenCalled();
  });

  test('depois do primeiro erro, revalida a cada tecla', async () => {
    const user = userEvent.setup();
    renderApp('/cadastro');

    const cep = screen.getByLabelText('CEP');
    await user.type(cep, '0131010');
    await user.tab();
    expect(screen.getByText('O CEP deve ter 8 dígitos')).toBeInTheDocument();

    await user.type(cep, '0');
    expect(screen.queryByText('O CEP deve ter 8 dígitos')).not.toBeInTheDocument();
  });

  test('checklist de senha destaca o caractere especial como pendente', async () => {
    const user = userEvent.setup();
    renderApp('/cadastro');

    await user.type(screen.getByLabelText('Senha'), 'Segura1234');

    const checklist = screen.getByRole('list', { name: 'Critérios da senha' });
    expect(within(checklist).getByText(/Um caractere especial/).closest('li')).toHaveAttribute('data-met', 'false');
    expect(within(checklist).getByText(/Uma letra maiúscula/).closest('li')).toHaveAttribute('data-met', 'true');
    expect(within(checklist).getByText(/Um dígito/).closest('li')).toHaveAttribute('data-met', 'true');
  });

  test('envio com erros é bloqueado e mostra os campos obrigatórios', async () => {
    const fetchMock = mockFetchResponse(201, {});
    const user = userEvent.setup();
    renderApp('/cadastro');

    await user.click(screen.getByRole('button', { name: 'Cadastrar' }));

    expect(screen.getAllByText('Campo obrigatório')).toHaveLength(12);
    expect(screen.getByLabelText('Nome completo')).toHaveFocus();
    expect(fetchMock).not.toHaveBeenCalled();
  });
});

describe('respostas do servidor', () => {
  test('sucesso redireciona para a confirmação com o e-mail', async () => {
    const fetchMock = mockFetchResponse(201, { email: 'maria@exemplo.com' });
    const user = userEvent.setup();
    renderApp('/cadastro');

    await fillValidForm(user);
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }));

    expect(await screen.findByRole('heading', { name: 'Cadastro realizado!' })).toBeInTheDocument();
    expect(screen.getByText('maria@exemplo.com')).toBeInTheDocument();
    expect(screen.getByText(/expira em 24 horas/)).toBeInTheDocument();

    const sent = JSON.parse(fetchMock.mock.calls[0][1].body as string);
    expect(sent).toMatchObject({ cpf: '529.982.247-25', state: 'SP', complement: '' });
  });

  test('e-mail já cadastrado aparece no campo', async () => {
    mockFetchResponse(
      409,
      problem(409, 'ALREADY_REGISTERED', 'Já existe uma conta com estes dados.', [
        { field: 'email', message: 'E-mail já cadastrado' },
      ]),
    );
    const user = userEvent.setup();
    renderApp('/cadastro');

    await fillValidForm(user);
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }));

    expect(await screen.findByText('E-mail já cadastrado')).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('Já existe uma conta com estes dados.');
  });

  test('erros de validação do servidor aparecem nos campos', async () => {
    mockFetchResponse(
      400,
      problem(400, 'VALIDATION_ERROR', 'Um ou mais campos são inválidos', [
        { field: 'cpf', message: 'CPF inválido' },
      ]),
    );
    const user = userEvent.setup();
    renderApp('/cadastro');

    await fillValidForm(user);
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }));

    expect(await screen.findByText('CPF inválido')).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('Corrija os campos destacados');
  });

  test('cadastro aguardando ativação mostra a orientação', async () => {
    mockFetchResponse(
      409,
      problem(409, 'PENDING_ACTIVATION',
        'Já existe um cadastro aguardando ativação. Verifique seu e-mail para ativar a conta.', [
          { field: 'email', message: 'Este e-mail possui um cadastro aguardando ativação' },
        ]),
    );
    const user = userEvent.setup();
    renderApp('/cadastro');

    await fillValidForm(user);
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Verifique seu e-mail para ativar a conta.');
    expect(screen.getByText('Este e-mail possui um cadastro aguardando ativação')).toBeInTheDocument();
  });

  test('serviço de e-mail indisponível pede nova tentativa', async () => {
    mockFetchResponse(
      503,
      problem(503, 'EMAIL_UNAVAILABLE',
        'Não foi possível enviar o e-mail de ativação. Tente novamente em alguns minutos.'),
    );
    const user = userEvent.setup();
    renderApp('/cadastro');

    await fillValidForm(user);
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Tente novamente em alguns minutos.');
    expect(screen.getByRole('button', { name: 'Cadastrar' })).toBeEnabled();
  });

  test('falha de rede mostra mensagem', async () => {
    mockFetchNetworkError();
    const user = userEvent.setup();
    renderApp('/cadastro');

    await fillValidForm(user);
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível falar com o servidor');
  });
});
