import { describe, expect, it } from 'vitest'
import casosFixture from '../../../contratos/validacao-casos.json'
import { MENSAGENS, type CodigoCampo } from './mensagens.ts'
import type { Campo } from './normalizacao.ts'
import { validarCampo, validarConfirmacaoSenha } from './regras.ts'

type Entrada = string | null | { senha: string | null; confirmacaoSenha: string | null }

interface Caso {
  regra: CodigoCampo
  campo: Campo
  entrada: Entrada
  valido: boolean
  codigo: CodigoCampo | null
}

const casos = casosFixture as Caso[]

/** 22h30 de 05/10/2026 em São Paulo, já 06/10 em UTC. */
const AGORA = new Date('2026-10-06T01:30:00Z')

function descrever(caso: Caso): string {
  let texto = JSON.stringify(caso.entrada)
  if (texto.length > 40) {
    texto = `${texto.slice(0, 37)}...(${texto.length})`
  }
  return `${caso.regra} | ${caso.campo} = ${texto} -> ${caso.valido ? 'válido' : caso.codigo}`
}

function validar(caso: Caso): CodigoCampo | null {
  const entrada = caso.entrada
  if (entrada !== null && typeof entrada === 'object') {
    return validarConfirmacaoSenha(entrada.senha, entrada.confirmacaoSenha)
  }
  return validarCampo(caso.campo, entrada, AGORA)
}

describe('FR-2: regras de validação compartilhadas (contratos/validacao-casos.json)', () => {
  it.each(casos.map((caso) => [descrever(caso), caso] as const))('FR-2: %s', (_nome, caso) => {
    expect(caso.valido).toBe(caso.codigo === null)
    expect(validar(caso)).toBe(caso.codigo)
  })

  it('FR-2: cada um dos 12 códigos de campo tem caso válido e inválido no fixture', () => {
    const codigos = Object.keys(MENSAGENS) as CodigoCampo[]
    expect(codigos).toHaveLength(12)
    for (const codigo of codigos) {
      expect(casos.some((c) => c.valido && c.regra === codigo), `válido de ${codigo}`).toBe(true)
      expect(casos.some((c) => !c.valido && c.codigo === codigo), `inválido de ${codigo}`).toBe(true)
    }
  })
})

describe('FR-2: validarCampo', () => {
  it('FR-2: "hoje" é a data em America/Sao_Paulo, não em UTC', () => {
    const agora = new Date('2026-10-06T01:30:00Z')
    expect(validarCampo('dataNascimento', '2026-10-05', agora)).toBe('DATA_NASCIMENTO_INVALIDA')
    expect(validarCampo('dataNascimento', '2026-10-04', agora)).toBeNull()
    const depoisDaMeiaNoite = new Date('2026-10-06T03:00:00Z')
    expect(validarCampo('dataNascimento', '2026-10-05', depoisDaMeiaNoite)).toBeNull()
  })

  it('FR-2: campo vazio produz só CAMPO_OBRIGATORIO, exceto complemento', () => {
    expect(validarCampo('cpf', undefined)).toBe('CAMPO_OBRIGATORIO')
    expect(validarCampo('senha', '   ')).toBe('CAMPO_OBRIGATORIO')
    expect(validarCampo('endereco.complemento', undefined)).toBeNull()
  })
})

describe('FR-2: validarConfirmacaoSenha', () => {
  it('FR-2: confirmação vazia é só obrigatória; diferente da senha é SENHAS_DIFERENTES', () => {
    expect(validarConfirmacaoSenha('Senha@123', '')).toBe('CAMPO_OBRIGATORIO')
    expect(validarConfirmacaoSenha('', '')).toBe('CAMPO_OBRIGATORIO')
    expect(validarConfirmacaoSenha('Senha@123', 'Senha@124')).toBe('SENHAS_DIFERENTES')
    expect(validarConfirmacaoSenha(undefined, 'Senha@123')).toBe('SENHAS_DIFERENTES')
    expect(validarConfirmacaoSenha('Senha@123', 'Senha@123')).toBeNull()
  })
})
