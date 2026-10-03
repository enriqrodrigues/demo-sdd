import { describe, expect, it } from 'vitest';
import type { ZodError } from 'zod';
import type { Perfil } from '../api/tipos';
import {
  cadastroParaRequisicao,
  cadastroSchema,
  MENSAGEM_SENHA_FRACA,
  MENSAGEM_SENHA_LONGA,
  OBRIGATORIO,
  perfilParaFormulario,
  perfilParaRequisicao,
  type CadastroForm,
} from './esquemas';

function mensagens(resultado: { success: true } | { success: false; error: ZodError }) {
  const mapa: Record<string, string> = {};
  if (!resultado.success) {
    for (const issue of resultado.error.issues) {
      mapa[issue.path.join('.')] ??= issue.message;
    }
  }
  return mapa;
}

const valido: CadastroForm = {
  nome: 'Maria da Silva',
  cpf: '529.982.247-25',
  email: 'maria@teste.local',
  dataNascimento: '1990-05-20',
  senha: 'Senha@123',
  confirmacaoSenha: 'Senha@123',
  telefone: '(11) 98765-4321',
  endereco: {
    cep: '01310-100',
    logradouro: 'Avenida Paulista',
    numero: '1000',
    complemento: '',
    bairro: 'Bela Vista',
    cidade: 'São Paulo',
    uf: 'SP',
  },
};

describe('cadastroSchema', () => {
  it('aceita formulário válido sem complemento', () => {
    expect(cadastroSchema.safeParse(valido).success).toBe(true);
  });

  it('marca obrigatórios com a mensagem da spec', () => {
    const vazio = mensagens(
      cadastroSchema.safeParse({
        ...valido,
        nome: ' ',
        cpf: '',
        senha: '',
        endereco: { ...valido.endereco, cep: '', uf: '' },
      }),
    );
    expect(vazio).toMatchObject({
      nome: OBRIGATORIO,
      cpf: OBRIGATORIO,
      senha: OBRIGATORIO,
      'endereco.cep': OBRIGATORIO,
      'endereco.uf': OBRIGATORIO,
    });
  });

  it('valida formatos', () => {
    const erros = mensagens(
      cadastroSchema.safeParse({
        ...valido,
        cpf: '529.982.247-24',
        email: 'maria@',
        dataNascimento: '2999-01-01',
        senha: 'fraca',
        confirmacaoSenha: 'fraca',
        telefone: '(11) 123',
        endereco: { ...valido.endereco, cep: '0131', uf: 'XX' },
      }),
    );
    expect(erros).toMatchObject({
      cpf: 'CPF inválido',
      email: 'E-mail inválido',
      dataNascimento: 'Data de nascimento inválida',
      senha: MENSAGEM_SENHA_FRACA,
      telefone: 'Telefone inválido',
      'endereco.cep': 'CEP inválido',
      'endereco.uf': 'UF inválida',
    });
  });

  it('rejeita datas de nascimento impossíveis', () => {
    for (const dataNascimento of ['2020-13-45', '1990-02-30']) {
      expect(mensagens(cadastroSchema.safeParse({ ...valido, dataNascimento })).dataNascimento).toBe(
        'Data de nascimento inválida',
      );
    }
  });

  it('rejeita senha acima de 72 bytes', () => {
    const senha = 'Aa1!' + 'é'.repeat(40);
    expect(mensagens(cadastroSchema.safeParse({ ...valido, senha, confirmacaoSenha: senha })).senha).toBe(
      MENSAGEM_SENHA_LONGA,
    );
  });

  it('acusa senhas diferentes mesmo com outros campos inválidos', () => {
    const erros = mensagens(cadastroSchema.safeParse({ ...valido, nome: '', confirmacaoSenha: 'Outra@123' }));
    expect(erros.confirmacaoSenha).toBe('As senhas não conferem');
  });
});

describe('conversões', () => {
  it('envia só dígitos, complemento nulo e sem confirmação de senha', () => {
    expect(cadastroParaRequisicao(valido)).toEqual({
      nome: 'Maria da Silva',
      cpf: '52998224725',
      email: 'maria@teste.local',
      dataNascimento: '1990-05-20',
      senha: 'Senha@123',
      telefone: '11987654321',
      endereco: {
        cep: '01310100',
        logradouro: 'Avenida Paulista',
        numero: '1000',
        complemento: null,
        bairro: 'Bela Vista',
        cidade: 'São Paulo',
        uf: 'SP',
      },
    });
  });

  it('converte perfil para formulário mascarado e de volta', () => {
    const perfil: Perfil = {
      nome: 'Maria da Silva',
      cpf: '52998224725',
      email: 'maria@teste.local',
      dataNascimento: '1990-05-20',
      telefone: '11987654321',
      endereco: { ...valido.endereco, cep: '01310100', complemento: null },
      status: 'ATIVO',
    };
    const formulario = perfilParaFormulario(perfil);
    expect(formulario.telefone).toBe('(11) 98765-4321');
    expect(formulario.endereco.cep).toBe('01310-100');
    expect(formulario.endereco.complemento).toBe('');
    expect(perfilParaRequisicao(formulario)).toEqual({
      telefone: '11987654321',
      endereco: { ...perfil.endereco },
    });
  });
});
