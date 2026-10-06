/**
 * Códigos de erro por campo e suas mensagens (API-CONTRACT §4.2, AD-10). Cópia literal de
 * `CodigoCampo.java`: o teste `MensagensFrontendTest` do backend lê este arquivo, por isso cada
 * mensagem fica numa linha no formato `CODIGO: 'texto',`.
 */
export type CodigoCampo =
  | 'CAMPO_OBRIGATORIO'
  | 'NOME_INVALIDO'
  | 'EMAIL_INVALIDO'
  | 'CPF_INVALIDO'
  | 'DATA_NASCIMENTO_INVALIDA'
  | 'TELEFONE_INVALIDO'
  | 'CEP_INVALIDO'
  | 'UF_INVALIDA'
  | 'TAMANHO_EXCEDIDO'
  | 'SENHA_FRACA'
  | 'SENHA_LONGA'
  | 'SENHAS_DIFERENTES'

export const MENSAGENS: Record<CodigoCampo, string> = {
  CAMPO_OBRIGATORIO: 'Campo obrigatório.',
  NOME_INVALIDO: 'Informe nome e sobrenome usando apenas letras.',
  EMAIL_INVALIDO: 'E-mail inválido.',
  CPF_INVALIDO: 'CPF inválido.',
  DATA_NASCIMENTO_INVALIDA: 'Data de nascimento inválida.',
  TELEFONE_INVALIDO: 'Telefone inválido. Informe DDD e número.',
  CEP_INVALIDO: 'CEP inválido.',
  UF_INVALIDA: 'UF inválida.',
  TAMANHO_EXCEDIDO: 'Texto muito longo.',
  SENHA_FRACA: 'A senha deve ter no mínimo 8 caracteres, com letra maiúscula, letra minúscula, número e caractere especial.',
  SENHA_LONGA: 'A senha deve ter no máximo 64 caracteres.',
  SENHAS_DIFERENTES: 'As senhas não conferem.',
}
