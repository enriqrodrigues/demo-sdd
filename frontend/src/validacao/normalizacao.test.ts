import { describe, expect, it } from 'vitest'
import { email, normalizarCampo, soDigitos, texto, textoOpcional } from './normalizacao.ts'

describe('FR-2: normalização antes da validação (AD-5)', () => {
  it('FR-2: texto remove espaço em branco das bordas, inclusive NBSP e tabulação', () => {
    expect(texto('  Ana Souza \t')).toBe('Ana Souza')
    expect(texto(' Ana　')).toBe('Ana')
    expect(texto('Ana  Souza')).toBe('Ana  Souza')
    expect(texto('   ')).toBe('')
    expect(texto(null)).toBeNull()
    expect(texto(undefined)).toBeNull()
  })

  it('FR-2: textoOpcional transforma vazio em null', () => {
    expect(textoOpcional(' Apto 12 ')).toBe('Apto 12')
    expect(textoOpcional('')).toBeNull()
    expect(textoOpcional('   ')).toBeNull()
    expect(textoOpcional(null)).toBeNull()
  })

  it('FR-2: email faz trim e minúsculas', () => {
    expect(email(' Ana@X.COM ')).toBe('ana@x.com')
    expect(email('TITULO@EXEMPLO.COM')).toBe('titulo@exemplo.com')
    expect(email(null)).toBeNull()
  })

  it('FR-2: soDigitos remove só . - ( ) / e espaço em branco', () => {
    expect(soDigitos('529.982.247-25')).toBe('52998224725')
    expect(soDigitos('(11) 98765-4321')).toBe('11987654321')
    expect(soDigitos('01/310 100')).toBe('01310100')
    expect(soDigitos('52998224725a')).toBe('52998224725a')
    expect(soDigitos('529_982+247')).toBe('529_982+247')
    expect(soDigitos(' . ')).toBe('')
    expect(soDigitos(null)).toBeNull()
  })

  it('FR-2: normalizarCampo aplica a normalização de cada campo', () => {
    expect(normalizarCampo('email', ' Ana@X.com ')).toBe('ana@x.com')
    expect(normalizarCampo('cpf', '529.982.247-25')).toBe('52998224725')
    expect(normalizarCampo('telefone', '(11) 98765-4321')).toBe('11987654321')
    expect(normalizarCampo('endereco.cep', '01310-100')).toBe('01310100')
    expect(normalizarCampo('endereco.complemento', '  ')).toBeNull()
    expect(normalizarCampo('nomeCompleto', '  Ana Souza ')).toBe('Ana Souza')
    expect(normalizarCampo('endereco.uf', ' sp ')).toBe('sp')
    expect(normalizarCampo('senha', ' Senha@123 ')).toBe(' Senha@123 ')
    expect(normalizarCampo('confirmacaoSenha', undefined)).toBeNull()
  })
})
