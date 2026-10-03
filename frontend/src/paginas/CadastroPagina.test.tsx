import { screen } from '@testing-library/react';
import userEvent, { type UserEvent } from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { renderizar } from '../test/renderizar';
import { servidor } from '../test/servidor';
import { CadastroConcluidoPagina } from './CadastroConcluidoPagina';
import { CadastroPagina } from './CadastroPagina';

function renderizarCadastro() {
  return renderizar('/cadastro', [
    { caminho: '/cadastro', elemento: <CadastroPagina /> },
    { caminho: '/cadastro/concluido', elemento: <CadastroConcluidoPagina /> },
  ]);
}

async function preencherFormularioValido(user: UserEvent) {
  await user.type(screen.getByLabelText('Nome completo'), 'Maria da Silva');
  await user.type(screen.getByLabelText('CPF'), '52998224725');
  await user.type(screen.getByLabelText('E-mail'), 'Maria@Teste.local');
  await user.type(screen.getByLabelText('Data de nascimento'), '1990-05-20');
  await user.type(screen.getByLabelText('Senha'), 'Senha@123');
  await user.type(screen.getByLabelText('Confirmação de senha'), 'Senha@123');
  await user.type(screen.getByLabelText('Telefone'), '11987654321');
  await user.type(screen.getByLabelText('CEP'), '01310100');
  await user.type(screen.getByLabelText('Logradouro'), 'Avenida Paulista');
  await user.type(screen.getByLabelText('Número'), '1000');
  await user.type(screen.getByLabelText('Bairro'), 'Bela Vista');
  await user.type(screen.getByLabelText('Cidade'), 'São Paulo');
  await user.selectOptions(screen.getByLabelText('UF'), 'SP');
}

describe('CadastroPagina', () => {
  it('mostra os obrigatórios ao enviar vazio, sem chamar a API', async () => {
    const user = userEvent.setup();
    renderizarCadastro();

    await user.click(screen.getByRole('button', { name: 'Cadastrar' }));

    expect(await screen.findAllByText('Campo obrigatório')).toHaveLength(13);
  });

  it('aplica máscara e valida o CPF ao sair do campo', async () => {
    const user = userEvent.setup();
    renderizarCadastro();
    const cpf = screen.getByLabelText('CPF');

    await user.type(cpf, '52998224724');
    await user.tab();

    expect(cpf).toHaveValue('529.982.247-24');
    expect(await screen.findByText('CPF inválido')).toBeInTheDocument();
    expect(cpf).toHaveAttribute('aria-invalid', 'true');
  });

  it('avisa quando as senhas não conferem', async () => {
    const user = userEvent.setup();
    renderizarCadastro();

    await user.type(screen.getByLabelText('Senha'), 'Senha@123');
    await user.type(screen.getByLabelText('Confirmação de senha'), 'Senha@124');
    await user.tab();

    expect(await screen.findByText('As senhas não conferem')).toBeInTheDocument();
  });

  it('envia os dados normalizados e mostra a confirmação', async () => {
    let corpo: Record<string, unknown> | undefined;
    servidor.use(
      http.post('/api/usuarios', async ({ request }) => {
        corpo = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json(
          { id: 'abc', email: 'maria@teste.local', status: 'PENDENTE_ATIVACAO' },
          { status: 201 },
        );
      }),
    );
    const user = userEvent.setup();
    renderizarCadastro();

    await preencherFormularioValido(user);
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }));

    expect(await screen.findByRole('heading', { name: 'Verifique seu e-mail' })).toBeInTheDocument();
    expect(screen.getByText('maria@teste.local')).toBeInTheDocument();
    expect(corpo).toEqual({
      nome: 'Maria da Silva',
      cpf: '52998224725',
      email: 'Maria@Teste.local',
      dataNascimento: '1990-05-20',
      senha: 'Senha@123',
      telefone: '11987654321',
      endereco: {
        cep: '01310100',
        logradouro: 'Avenida Paulista',
        numero: '1000',
        complemento: null,
        bairro: 'Bela Vista',
        cidade: 'São Paulo',
        uf: 'SP',
      },
    });
  });

  it('marca o campo e-mail quando a API responde 409', async () => {
    servidor.use(
      http.post('/api/usuarios', () =>
        HttpResponse.json(
          {
            detail: 'E-mail já cadastrado',
            codigo: 'EMAIL_JA_CADASTRADO',
            erros: [{ campo: 'email', mensagem: 'E-mail já cadastrado' }],
          },
          { status: 409, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );
    const user = userEvent.setup();
    renderizarCadastro();

    await preencherFormularioValido(user);
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }));

    expect(await screen.findByText('E-mail já cadastrado')).toBeInTheDocument();
    expect(screen.getByLabelText('E-mail')).toHaveAttribute('aria-invalid', 'true');
  });

  it('mostra erro geral quando a API falha sem erros de campo', async () => {
    servidor.use(http.post('/api/usuarios', () => new HttpResponse(null, { status: 500 })));
    const user = userEvent.setup();
    renderizarCadastro();

    await preencherFormularioValido(user);
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }));

    expect(await screen.findByText('Erro inesperado')).toBeInTheDocument();
  });
});
