import { render, screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { MemoryRouter } from 'react-router';
import { describe, expect, it } from 'vitest';
import { App } from './App';
import { servidor } from './test/servidor';

describe('App', () => {
  it('redireciona rota desconhecida para o início e daí para o login sem sessão', async () => {
    servidor.use(http.get('/api/perfil', () => HttpResponse.json({}, { status: 401 })));

    render(
      <MemoryRouter initialEntries={['/rota-inexistente']}>
        <App />
      </MemoryRouter>,
    );

    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument();
  });

  it('abre a tela de cadastro', () => {
    render(
      <MemoryRouter initialEntries={['/cadastro']}>
        <App />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: 'Criar conta' })).toBeInTheDocument();
  });
});
