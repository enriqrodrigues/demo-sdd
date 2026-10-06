import { requisitar } from './cliente.ts'

export type Sessao =
  | { autenticado: false }
  | { autenticado: true; conta: { nomeCompleto: string; email: string } }

export function obterSessao(): Promise<Sessao> {
  return requisitar<Sessao>('GET', '/api/auth/sessao')
}
