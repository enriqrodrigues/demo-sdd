import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { renderizar } from '../test/renderizar';
import { servidor } from '../test/servidor';
import { LoginPagina } from './LoginPagina';

function renderizarLogin() {
  return renderizar('/login', [
    { caminho: '/login', elemento: <LoginPagina /> },
    { caminho: '/perfil', elemento: <p>Página de perfil</p> },
  ]);
}

function problema(status: number, codigo: string) {
  return HttpResponse.json(
    { detail: 'erro', codigo },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  );
}

async function entrar() {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText('E-mail'), 'maria@teste.local');
  await user.type(screen.getByLabelText('Senha'), 'Senha@123');
  await user.click(screen.getByRole('button', { name: 'Entrar' }));
}

describe('LoginPagina', () => {
  it('envia as credenciais e vai para o perfil', async () => {
    let corpo: unknown;
    servidor.use(
      http.post('/api/auth/login', async ({ request }) => {
        corpo = await request.json();
        return HttpResponse.json({ nome: 'Maria da Silva', email: 'maria@teste.local' });
      }),
    );
    renderizarLogin();

    await entrar();

    expect(await screen.findByText('Página de perfil')).toBeInTheDocument();
    expect(corpo).toEqual({ email: 'maria@teste.local', senha: 'Senha@123' });
  });

  it('mostra mensagem genérica para credenciais inválidas', async () => {
    servidor.use(http.post('/api/auth/login', () => problema(401, 'CREDENCIAIS_INVALIDAS')));
    renderizarLogin();

    await entrar();

    expect(await screen.findByText('E-mail ou senha inválidos.')).toBeInTheDocument();
  });

  it('avisa que a conta está pendente de ativação', async () => {
    servidor.use(http.post('/api/auth/login', () => problema(403, 'CONTA_PENDENTE')));
    renderizarLogin();

    await entrar();

    expect(await screen.findByText('Sua conta ainda não foi ativada. Verifique seu e-mail.')).toBeInTheDocument();
  });

  it('exige e-mail e senha', async () => {
    const user = userEvent.setup();
    renderizarLogin();

    await user.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(await screen.findAllByText('Campo obrigatório')).toHaveLength(2);
  });
});
