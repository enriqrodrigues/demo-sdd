import {
  EMPTY_VALUES,
  REQUIRED,
  isValidCpf,
  passwordCriteria,
  todayIso,
  validateField,
  validateForm,
  type RegistrationValues,
} from './validation';

const TODAY = '2026-10-03';

const VALID: RegistrationValues = {
  name: 'Maria da Silva',
  cpf: '529.982.247-25',
  email: 'maria@exemplo.com',
  birthDate: '1990-05-20',
  password: 'Segura@123',
  phone: '(11) 98765-4321',
  cep: '01310-100',
  street: 'Avenida Paulista',
  number: '1000',
  complement: '',
  district: 'Bela Vista',
  city: 'São Paulo',
  state: 'SP',
};

const withValue = (field: keyof RegistrationValues, value: string) =>
  validateField(field, { ...VALID, [field]: value }, TODAY);

describe('formulário completo', () => {
  test('cadastro válido sem complemento não tem erros', () => {
    expect(validateForm(VALID, TODAY)).toEqual({});
  });

  test('formulário vazio aponta todos os campos obrigatórios, exceto complemento', () => {
    const errors = validateForm(EMPTY_VALUES, TODAY);
    expect(Object.keys(errors)).toHaveLength(12);
    expect(errors.complement).toBeUndefined();
    expect(errors.name).toBe(REQUIRED);
  });
});

describe('campos obrigatórios', () => {
  test('nome só com espaços é obrigatório', () => {
    expect(withValue('name', '   ')).toBe(REQUIRED);
  });
});

describe('CPF', () => {
  test('aceita CPF válido com máscara', () => {
    expect(isValidCpf('529.982.247-25')).toBe(true);
    expect(withValue('cpf', '529.982.247-25')).toBeUndefined();
  });

  test('recusa dígito verificador incorreto', () => {
    expect(withValue('cpf', '529.982.247-26')).toBe('CPF inválido');
  });

  test('recusa dígitos repetidos', () => {
    expect(withValue('cpf', '111.111.111-11')).toBe('CPF inválido');
  });

  test('recusa letras', () => {
    expect(withValue('cpf', '529.982.247-2a')).toBe('CPF inválido');
  });
});

describe('e-mail', () => {
  test('recusa e-mail sem domínio', () => {
    expect(withValue('email', 'maria@')).toBe('E-mail inválido');
  });

  test('aceita e-mail com espaços e maiúsculas', () => {
    expect(withValue('email', ' Maria@Exemplo.com ')).toBeUndefined();
  });

  test('recusa domínio sem ponto', () => {
    expect(withValue('email', 'maria@localhost')).toBe('E-mail inválido');
  });
});

describe('data de nascimento', () => {
  test('recusa data futura', () => {
    expect(withValue('birthDate', '2026-10-04')).toBe('A data de nascimento não pode ser futura');
  });

  test('aceita a data de hoje', () => {
    expect(withValue('birthDate', TODAY)).toBeUndefined();
  });

  test('todayIso usa a data local', () => {
    expect(todayIso(new Date(2026, 0, 5, 23, 30))).toBe('2026-01-05');
  });
});

describe('senha', () => {
  test('aceita senha forte', () => {
    expect(withValue('password', 'Segura@123')).toBeUndefined();
  });

  test('recusa senha sem caractere especial indicando o critério', () => {
    expect(withValue('password', 'Segura1234')).toBe('A senha deve conter: um caractere especial');
  });

  test('recusa senha curta', () => {
    expect(withValue('password', 'Se@1')).toBe('A senha deve ter ao menos 8 caracteres');
  });

  test('checklist mostra critérios atendidos e pendentes', () => {
    const criteria = Object.fromEntries(passwordCriteria('Segura1234').map((c) => [c.id, c.met]));
    expect(criteria).toEqual({ length: true, upper: true, lower: true, digit: true, special: false });
  });
});

describe('telefone', () => {
  test('aceita celular com máscara', () => {
    expect(withValue('phone', '(11) 98765-4321')).toBeUndefined();
  });

  test('aceita fixo com máscara', () => {
    expect(withValue('phone', '(11) 3456-7890')).toBeUndefined();
  });

  test('recusa quantidade inválida de dígitos', () => {
    expect(withValue('phone', '(11) 3456-789')).toBe(
      'O telefone deve ter 10 dígitos (fixo) ou 11 dígitos (celular), incluindo o DDD',
    );
  });
});

describe('endereço', () => {
  test('aceita CEP com hífen', () => {
    expect(withValue('cep', '01310-100')).toBeUndefined();
  });

  test('recusa CEP com 7 dígitos', () => {
    expect(withValue('cep', '0131010')).toBe('O CEP deve ter 8 dígitos');
  });

  test('recusa UF inexistente', () => {
    expect(withValue('state', 'XX')).toBe('UF inválida');
  });

  test('complemento é opcional mas tem tamanho máximo', () => {
    expect(withValue('complement', '')).toBeUndefined();
    expect(withValue('complement', 'x'.repeat(101))).toBe('Máximo de 100 caracteres');
  });
});
