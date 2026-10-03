import { maskCep, maskCpf, maskPhone } from './masks';

test('máscara de CPF', () => {
  expect(maskCpf('52998224725')).toBe('529.982.247-25');
  expect(maskCpf('5299')).toBe('529.9');
  expect(maskCpf('529.982.247-25999')).toBe('529.982.247-25');
});

test('máscara de telefone se adapta a fixo e celular', () => {
  expect(maskPhone('1134567890')).toBe('(11) 3456-7890');
  expect(maskPhone('11987654321')).toBe('(11) 98765-4321');
  expect(maskPhone('11')).toBe('(11');
  expect(maskPhone('')).toBe('');
});

test('máscara de CEP', () => {
  expect(maskCep('01310100')).toBe('01310-100');
  expect(maskCep('0131')).toBe('0131');
});
