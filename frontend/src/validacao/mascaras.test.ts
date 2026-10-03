import { describe, expect, it } from 'vitest';
import { formatarData, mascaraCep, mascaraCpf, mascaraTelefone } from './mascaras';

describe('máscaras', () => {
  it('formata CPF progressivamente', () => {
    expect(mascaraCpf('529')).toBe('529');
    expect(mascaraCpf('5299822')).toBe('529.982.2');
    expect(mascaraCpf('52998224725')).toBe('529.982.247-25');
    expect(mascaraCpf('529.982.247-2599')).toBe('529.982.247-25');
  });

  it('formata telefone fixo e celular', () => {
    expect(mascaraTelefone('11')).toBe('(11');
    expect(mascaraTelefone('1133334444')).toBe('(11) 3333-4444');
    expect(mascaraTelefone('11987654321')).toBe('(11) 98765-4321');
  });

  it('formata CEP', () => {
    expect(mascaraCep('01310')).toBe('01310');
    expect(mascaraCep('01310100')).toBe('01310-100');
  });

  it('formata data ISO para o padrão brasileiro', () => {
    expect(formatarData('1990-05-20')).toBe('20/05/1990');
  });
});
