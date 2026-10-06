/**
 * Normalização aplicada antes da validação (AD-5), igual à de `Normalizacao.java`.
 * Espaço em branco é `\s`/`trim`. O backend usa `\p{IsWhite_Space}`, que difere em dois caracteres
 * (diferença aceita): U+FEFF só é espaço aqui, e U+0085 só é espaço lá. Todos aceitam `null`/`undefined`.
 */

export type Campo =
  | 'nomeCompleto'
  | 'email'
  | 'cpf'
  | 'dataNascimento'
  | 'telefone'
  | 'endereco.cep'
  | 'endereco.logradouro'
  | 'endereco.numero'
  | 'endereco.complemento'
  | 'endereco.bairro'
  | 'endereco.cidade'
  | 'endereco.uf'
  | 'senha'
  | 'confirmacaoSenha'

type Valor = string | null | undefined

/** Remove espaço em branco do início e do fim. */
export function texto(valor: Valor): string | null {
  return valor == null ? null : valor.trim()
}

/** Como `texto`, mas texto vazio vira `null`. */
export function textoOpcional(valor: Valor): string | null {
  const aparado = texto(valor)
  return aparado === null || aparado === '' ? null : aparado
}

/** `trim` seguido de minúsculas. */
export function email(valor: Valor): string | null {
  const aparado = texto(valor)
  return aparado === null ? null : aparado.toLowerCase()
}

/** Remove só `.`, `-`, `(`, `)`, `/` e espaço em branco; o resto fica. */
export function soDigitos(valor: Valor): string | null {
  return valor == null ? null : valor.replace(/[.\-()/\s]/g, '')
}

/** A normalização de cada campo do formulário; `senha` e `confirmacaoSenha` não mudam. */
export function normalizarCampo(campo: Campo, valor: Valor): string | null {
  switch (campo) {
    case 'email':
      return email(valor)
    case 'cpf':
    case 'telefone':
    case 'endereco.cep':
      return soDigitos(valor)
    case 'endereco.complemento':
      return textoOpcional(valor)
    case 'senha':
    case 'confirmacaoSenha':
      return valor ?? null
    default:
      return texto(valor)
  }
}
