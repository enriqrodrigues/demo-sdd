import { z } from 'zod';
import type { EnderecoDados, Perfil } from '../api/tipos';
import { mascaraCep, mascaraTelefone } from './mascaras';
import { cpfValido, dataPassada, emailValido, senhaExcedeLimite, senhaForte, soDigitos, UFS } from './regras';

export const OBRIGATORIO = 'Campo obrigatório';
export const MENSAGEM_SENHA_FRACA =
  'A senha deve ter ao menos 8 caracteres, com letra maiúscula, minúscula, número e caractere especial';
export const MENSAGEM_SENHA_LONGA = 'A senha é longa demais';

const maximo = (n: number) => `Máximo de ${n} caracteres`;
const obrigatorio = () => z.string().trim().min(1, OBRIGATORIO);

const telefone = obrigatorio().refine((v) => /^\d{10,11}$/.test(soDigitos(v)), 'Telefone inválido');

export const enderecoSchema = z.object({
  cep: obrigatorio().refine((v) => soDigitos(v).length === 8, 'CEP inválido'),
  logradouro: obrigatorio().max(200, maximo(200)),
  numero: obrigatorio().max(10, maximo(10)),
  complemento: z.string().trim().max(100, maximo(100)),
  bairro: obrigatorio().max(100, maximo(100)),
  cidade: obrigatorio().max(100, maximo(100)),
  uf: obrigatorio().refine((v) => (UFS as readonly string[]).includes(v), 'UF inválida'),
});

export const cadastroSchema = z
  .object({
    nome: obrigatorio().max(150, maximo(150)),
    cpf: obrigatorio().refine(cpfValido, 'CPF inválido'),
    email: obrigatorio().max(254, maximo(254)).refine(emailValido, 'E-mail inválido'),
    dataNascimento: obrigatorio().refine(dataPassada, 'Data de nascimento inválida'),
    senha: z
      .string()
      .min(1, OBRIGATORIO)
      .refine((s) => !senhaExcedeLimite(s), MENSAGEM_SENHA_LONGA)
      .refine(senhaForte, MENSAGEM_SENHA_FRACA),
    confirmacaoSenha: z.string().min(1, OBRIGATORIO),
    telefone,
    endereco: enderecoSchema,
  })
  .refine((d) => d.senha === d.confirmacaoSenha, {
    message: 'As senhas não conferem',
    path: ['confirmacaoSenha'],
    // roda mesmo quando outros campos têm erro, para o aviso aparecer em tempo real
    when(payload) {
      const valor = payload.value as { senha?: unknown; confirmacaoSenha?: unknown };
      return typeof valor?.senha === 'string' && typeof valor?.confirmacaoSenha === 'string';
    },
  });

export const perfilSchema = z.object({ telefone, endereco: enderecoSchema });

export const loginSchema = z.object({
  email: obrigatorio(),
  senha: z.string().min(1, OBRIGATORIO),
});

export type EnderecoForm = z.infer<typeof enderecoSchema>;
export type CadastroForm = z.infer<typeof cadastroSchema>;
export type PerfilForm = z.infer<typeof perfilSchema>;
export type LoginForm = z.infer<typeof loginSchema>;

export const ENDERECO_VAZIO: EnderecoForm = {
  cep: '',
  logradouro: '',
  numero: '',
  complemento: '',
  bairro: '',
  cidade: '',
  uf: '',
};

function enderecoParaRequisicao(e: EnderecoForm): EnderecoDados {
  return {
    cep: soDigitos(e.cep),
    logradouro: e.logradouro.trim(),
    numero: e.numero.trim(),
    complemento: e.complemento.trim() || null,
    bairro: e.bairro.trim(),
    cidade: e.cidade.trim(),
    uf: e.uf,
  };
}

export function cadastroParaRequisicao(d: CadastroForm) {
  return {
    nome: d.nome.trim(),
    cpf: soDigitos(d.cpf),
    email: d.email.trim(),
    dataNascimento: d.dataNascimento,
    senha: d.senha,
    telefone: soDigitos(d.telefone),
    endereco: enderecoParaRequisicao(d.endereco),
  };
}

export function perfilParaFormulario(p: Perfil): PerfilForm {
  return {
    telefone: mascaraTelefone(p.telefone),
    endereco: { ...p.endereco, cep: mascaraCep(p.endereco.cep), complemento: p.endereco.complemento ?? '' },
  };
}

export function perfilParaRequisicao(d: PerfilForm) {
  return { telefone: soDigitos(d.telefone), endereco: enderecoParaRequisicao(d.endereco) };
}
