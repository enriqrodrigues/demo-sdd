import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import App from '../App';

/** Renderiza a aplicação inteira (com as rotas reais) a partir da URL informada. */
export function renderApp(initialPath: string) {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <App />
    </MemoryRouter>,
  );
}
