import { obterSessao } from './api/sessao.ts'

/**
 * Busca a sessão uma vez antes de renderizar a SPA, para que o navegador
 * receba o cookie XSRF-TOKEN antes de qualquer requisição que altere estado.
 * Uma falha aqui não impede a SPA de abrir.
 */
export async function inicializar(): Promise<void> {
  try {
    await obterSessao()
  } catch {
    // erro engolido: o primeiro POST exibirá a mensagem do envelope
  }
}
