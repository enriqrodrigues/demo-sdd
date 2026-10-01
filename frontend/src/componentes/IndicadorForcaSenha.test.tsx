import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { IndicadorForcaSenha } from './IndicadorForcaSenha';

describe('IndicadorForcaSenha', () => {
  it('marca apenas os critérios atendidos', () => {
    render(<IndicadorForcaSenha senha="abc1" />);

    expect(screen.getByText(/Letra minúscula/)).toHaveAttribute('data-ok', 'true');
    expect(screen.getByText(/Número/)).toHaveAttribute('data-ok', 'true');
    expect(screen.getByText(/Letra maiúscula/)).toHaveAttribute('data-ok', 'false');
    expect(screen.getByText(/Caractere especial/)).toHaveAttribute('data-ok', 'false');
    expect(screen.getByText(/Mínimo de 8 caracteres/)).toHaveAttribute('data-ok', 'false');
  });
});
