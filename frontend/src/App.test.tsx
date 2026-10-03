import { render, screen } from '@testing-library/react';
import App from './App';

test('renderiza o título da aplicação', () => {
  render(<App />);
  expect(screen.getByRole('heading', { name: 'Cadastro de Usuários' })).toBeInTheDocument();
});
