import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import type { Perfil } from '../api/tipos';
import { renderizar } from '../test/renderizar';
import { servidor } from '../test/servidor';
import { PerfilPagina } from './PerfilPagina';

const perfil: Perfil = {
  nome: 'Maria da Silva',
  cpf: '52998224725',
  email: 'maria@teste.local',
  dataNascimento: '1990-05-20',
  telefone: '11987654321',
  endereco: {
    cep: '01310100',
    logradouro: 'Avenida Paulista',
    numero: '1000',
    complemento: 'Apto 12',
    bairro: 'Bela Vista',
    cidade: 'São Paulo',
    uf: 'SP',
  },
  status: 'ATIVO',
};

function renderizarPerfil() {
  return renderizar('/perfil', [
    { caminho: '/perfil', elemento: <PerfilPagina /> },
    { caminho: '/login', elemento: <p>Página de login</p> },
  ]);
}

describe('PerfilPagina', () => {
  it('mostra os dados com os imutáveis apenas como texto', async () => {
    servidor.use(http.get('/api/perfil', () => HttpResponse.json(perfil)));
    renderizarPerfil();

    expect(await screen.findByText('Maria da Silva')).toBeInTheDocument();
    expect(screen.getByText('529.982.247-25')).toBeInTheDocument();
    expect(screen.getByText('maria@teste.local')).toBeInTheDocument();
    expect(screen.getByText('20/05/1990')).toBeInTheDocument();
    expect(screen.getByText('(11) 98765-4321')).toBeInTheDocument();
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
  });

  it('redireciona para o login sem sessão', async () => {
    servidor.use(
      http.get('/api/perfil', () =>
        HttpResponse.json({ codigo: 'NAO_AUTENTICADO' }, { status: 401 }),
      ),
    );
    renderizarPerfil();

    expect(await screen.findByText('Página de login')).toBeInTheDocument();
  });

  it('edita telefone e endereço enviando só os campos editáveis', async () => {
    let corpo: Record<string, unknown> | undefined;
    servidor.use(
      http.get('/api/perfil', () => HttpResponse.json(perfil)),
      http.put('/api/perfil', async ({ request }) => {
        corpo = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...perfil, telefone: '1133334444' });
      }),
    );
    const user = userEvent.setup();
    renderizarPerfil();

    await user.click(await screen.findByRole('button', { name: 'Editar contato' }));
    expect(screen.queryByLabelText('Nome completo')).not.toBeInTheDocument();
    const telefone = screen.getByLabelText('Telefone');
    await user.clear(telefone);
    await user.type(telefone, '1133334444');
    await user.click(screen.getByRole('button', { name: 'Salvar' }));

    expect(await screen.findByText('Dados atualizados com sucesso.')).toBeInTheDocument();
    expect(screen.getByText('(11) 3333-4444')).toBeInTheDocument();
    expect(corpo).toEqual({ telefone: '1133334444', endereco: perfil.endereco });
  });

  it('mostra erro de validação vindo da API no campo', async () => {
    servidor.use(
      http.get('/api/perfil', () => HttpResponse.json(perfil)),
      http.put('/api/perfil', () =>
        HttpResponse.json(
          { detail: 'Dados inválidos', codigo: 'VALIDACAO', erros: [{ campo: 'endereco.cidade', mensagem: 'Máximo de 100 caracteres' }] },
          { status: 400, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );
    const user = userEvent.setup();
    renderizarPerfil();

    await user.click(await screen.findByRole('button', { name: 'Editar contato' }));
    await user.click(screen.getByRole('button', { name: 'Salvar' }));

    expect(await screen.findByText('Máximo de 100 caracteres')).toBeInTheDocument();
  });

  it('cancelar volta para a visualização sem chamar a API', async () => {
    servidor.use(http.get('/api/perfil', () => HttpResponse.json(perfil)));
    const user = userEvent.setup();
    renderizarPerfil();

    await user.click(await screen.findByRole('button', { name: 'Editar contato' }));
    await user.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(screen.getByRole('button', { name: 'Editar contato' })).toBeInTheDocument();
  });

  it('sair encerra a sessão e vai para o login', async () => {
    let saiu = false;
    servidor.use(
      http.get('/api/perfil', () => HttpResponse.json(perfil)),
      http.post('/api/auth/logout', () => {
        saiu = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const user = userEvent.setup();
    renderizarPerfil();

    await user.click(await screen.findByRole('button', { name: 'Sair' }));

    expect(await screen.findByText('Página de login')).toBeInTheDocument();
    expect(saiu).toBe(true);
  });
});
