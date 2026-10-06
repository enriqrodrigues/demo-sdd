/**
 * Regras de §4.2 do API-CONTRACT (AD-10), as mesmas de `ValidadorCampo.java`: letra é `\p{L}`,
 * dígito de senha é `\p{N}`, `\d` é só ASCII. Cada campo produz no máximo um código.
 */
import type { CodigoCampo } from './mensagens.ts'
import { normalizarCampo, type Campo } from './normalizacao.ts'

const FUSO = 'America/Sao_Paulo'

const EM_BRANCO = /^\s*$/
const NOME = /^[\p{L}'-]+( [\p{L}'-]+)+$/u
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const ONZE_DIGITOS = /^\d{11}$/
const DATA_ISO = /^(\d{4})-(\d{2})-(\d{2})$/
const TELEFONE = /^[1-9]{2}(\d{8}|9\d{8})$/
const CEP = /^\d{8}$/
const MAIUSCULA = /\p{Lu}/u
const MINUSCULA = /\p{Ll}/u
const DIGITO = /\p{N}/u
const ESPECIAL = /[^\p{L}\p{N}\s]/u

const UFS = new Set([
  'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG', 'PA',
  'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO',
])

const TAMANHO_MAXIMO: Partial<Record<Campo, number>> = {
  'endereco.logradouro': 150,
  'endereco.bairro': 150,
  'endereco.cidade': 150,
  'endereco.numero': 20,
  'endereco.complemento': 100,
}

/** Dígito verificador por módulo 11 sobre os `quantidade` primeiros dígitos. */
function digitoVerificador(cpf: string, quantidade: number): number {
  let soma = 0
  for (let i = 0; i < quantidade; i++) {
    soma += Number(cpf[i]) * (quantidade + 1 - i)
  }
  const resto = soma % 11
  return resto < 2 ? 0 : 11 - resto
}

function cpfValido(cpf: string): boolean {
  if (!ONZE_DIGITOS.test(cpf) || new Set(cpf).size === 1) {
    return false
  }
  return digitoVerificador(cpf, 9) === Number(cpf[9]) && digitoVerificador(cpf, 10) === Number(cpf[10])
}

function bissexto(ano: number): boolean {
  return (ano % 4 === 0 && ano % 100 !== 0) || ano % 400 === 0
}

function diasNoMes(ano: number, mes: number): number {
  return [31, bissexto(ano) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][mes - 1]
}

/** Data de `agora` no fuso de São Paulo, no formato `yyyy-MM-dd`. */
function hojeEmSaoPaulo(agora: Date): string {
  const partes = new Intl.DateTimeFormat('en-US', {
    timeZone: FUSO,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(agora)
  const parte = (tipo: Intl.DateTimeFormatPartTypes) => partes.find((p) => p.type === tipo)?.value ?? ''
  return `${parte('year').padStart(4, '0')}-${parte('month')}-${parte('day')}`
}

function dataNascimentoValida(valor: string, agora: Date): boolean {
  const partes = DATA_ISO.exec(valor)
  if (!partes) {
    return false
  }
  const ano = Number(partes[1])
  const mes = Number(partes[2])
  const dia = Number(partes[3])
  if (mes < 1 || mes > 12 || dia < 1 || dia > diasNoMes(ano, mes)) {
    return false
  }
  // As duas datas estão em yyyy-MM-dd com ano de 4 dígitos: a ordem de texto é a ordem de calendário.
  return valor < hojeEmSaoPaulo(agora)
}

function validarSenha(senha: string): CodigoCampo | null {
  if (senha.length > 64) {
    return 'SENHA_LONGA'
  }
  const forte =
    senha.length >= 8 &&
    MAIUSCULA.test(senha) &&
    MINUSCULA.test(senha) &&
    DIGITO.test(senha) &&
    ESPECIAL.test(senha)
  return forte ? null : 'SENHA_FRACA'
}

/**
 * Normaliza o valor cru do campo e devolve o código da primeira regra violada, ou `null` se válido.
 * "Hoje" é a data de `agora` em `America/Sao_Paulo`. Para `confirmacaoSenha` só checa obrigatório;
 * a comparação com a senha é de `validarConfirmacaoSenha`.
 */
export function validarCampo(
  campo: Campo,
  valor: string | null | undefined,
  agora: Date = new Date(),
): CodigoCampo | null {
  const normalizado = normalizarCampo(campo, valor)
  if (normalizado === null || EM_BRANCO.test(normalizado)) {
    return campo === 'endereco.complemento' ? null : 'CAMPO_OBRIGATORIO'
  }
  const maximo = TAMANHO_MAXIMO[campo]
  if (maximo !== undefined) {
    return normalizado.length <= maximo ? null : 'TAMANHO_EXCEDIDO'
  }
  switch (campo) {
    case 'nomeCompleto':
      return normalizado.length <= 150 && NOME.test(normalizado) ? null : 'NOME_INVALIDO'
    case 'email':
      return normalizado.length <= 254 && EMAIL.test(normalizado) ? null : 'EMAIL_INVALIDO'
    case 'cpf':
      return cpfValido(normalizado) ? null : 'CPF_INVALIDO'
    case 'dataNascimento':
      return dataNascimentoValida(normalizado, agora) ? null : 'DATA_NASCIMENTO_INVALIDA'
    case 'telefone':
      return TELEFONE.test(normalizado) ? null : 'TELEFONE_INVALIDO'
    case 'endereco.cep':
      return CEP.test(normalizado) ? null : 'CEP_INVALIDO'
    case 'endereco.uf':
      return UFS.has(normalizado) ? null : 'UF_INVALIDA'
    case 'senha':
      return validarSenha(normalizado)
    default:
      return null
  }
}

/**
 * Código do campo `confirmacaoSenha`: `CAMPO_OBRIGATORIO` se vazia, `SENHAS_DIFERENTES` se não for
 * exatamente igual à senha (sem `trim`), ou `null`.
 */
export function validarConfirmacaoSenha(
  senha: string | null | undefined,
  confirmacao: string | null | undefined,
): CodigoCampo | null {
  const obrigatorio = validarCampo('confirmacaoSenha', confirmacao)
  if (obrigatorio !== null) {
    return obrigatorio
  }
  return confirmacao === senha ? null : 'SENHAS_DIFERENTES'
}
