import { describe, expect, it } from 'vitest';
import { cpfValido, dataPassada, emailValido, senhaExcedeLimite, senhaForte, soDigitos } from './regras';

describe('regras', () => {
  it('extrai só os dígitos', () => {
    expect(soDigitos('529.982.247-25')).toBe('52998224725');
  });

  it.each(['52998224725', '529.982.247-25', '11144477735'])('aceita CPF válido %s', (cpf) => {
    expect(cpfValido(cpf)).toBe(true);
  });

  it.each(['52998224724', '11111111111', '5299822472', ''])('rejeita CPF inválido %s', (cpf) => {
    expect(cpfValido(cpf)).toBe(false);
  });

  it('exige maiúscula, minúscula, número, especial e 8 caracteres', () => {
    expect(senhaForte('Senha@123')).toBe(true);
    expect(senhaForte('Sen@1')).toBe(false);
    expect(senhaForte('senha@123')).toBe(false);
    expect(senhaForte('SENHA@123')).toBe(false);
    expect(senhaForte('Senha@abc')).toBe(false);
    expect(senhaForte('Senha1234')).toBe(false);
  });

  it('conta o limite da senha em bytes UTF-8', () => {
    expect(senhaExcedeLimite('Aa1!' + 'x'.repeat(68))).toBe(false);
    expect(senhaExcedeLimite('Aa1!' + 'é'.repeat(35))).toBe(true);
  });

  it('aceita só datas anteriores a hoje', () => {
    expect(dataPassada('1990-05-20')).toBe(true);
    expect(dataPassada('2999-01-01')).toBe(false);
    expect(dataPassada('')).toBe(false);
  });

  it('valida formato de e-mail', () => {
    expect(emailValido('maria@teste.local')).toBe(true);
    expect(emailValido('maria@')).toBe(false);
    expect(emailValido('maria teste@x.com')).toBe(false);
  });
});
