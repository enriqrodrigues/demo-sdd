import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { renderizar } from '../test/renderizar';
import { servidor } from '../test/servidor';
import { InicioPagina } from './InicioPagina';

const rotas = [
  { caminho: '/', elemento: <InicioPagina /> },
  { caminho: '/perfil', elemento: <p>Página de perfil</p> },
  { caminho: '/login', elemento: <p>Página de login</p> },
];

describe('InicioPagina', () => {
  it('vai para o perfil quando há sessão', async () => {
    servidor.use(http.get('/api/perfil', () => HttpResponse.json({})));
    renderizar('/', rotas);
    expect(await screen.findByText('Página de perfil')).toBeInTheDocument();
  });

  it('vai para o login sem sessão', async () => {
    servidor.use(http.get('/api/perfil', () => HttpResponse.json({}, { status: 401 })));
    renderizar('/', rotas);
    expect(await screen.findByText('Página de login')).toBeInTheDocument();
  });
});
