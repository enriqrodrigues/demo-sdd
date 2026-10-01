import { render } from '@testing-library/react';
import { StrictMode, type ReactNode } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router';

export interface RotaTeste {
  caminho: string;
  elemento: ReactNode;
}

export function renderizar(rotaInicial: string, rotas: RotaTeste[], opcoes: { strict?: boolean } = {}) {
  const arvore = (
    <MemoryRouter initialEntries={[rotaInicial]}>
      <Routes>
        {rotas.map((r) => (
          <Route key={r.caminho} path={r.caminho} element={r.elemento} />
        ))}
      </Routes>
    </MemoryRouter>
  );
  return render(opcoes.strict ? <StrictMode>{arvore}</StrictMode> : arvore);
}
