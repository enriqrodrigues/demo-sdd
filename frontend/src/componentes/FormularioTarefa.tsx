import { useRef, useState, type FormEvent } from 'react'
import { ErroValidacao, type DadosTarefa, type Prioridade } from '../api'

interface Props {
  /** Título do formulário, também usado como seu nome acessível. */
  titulo: string
  valoresIniciais: DadosTarefa
  /** Rejeita com {@link ErroValidacao} para exibir erros por campo; outros erros ficam com quem chama. */
  aoSalvar: (dados: DadosTarefa) => Promise<void>
  aoCancelar: () => void
  /** Aviso vindo de fora, como o de conflito de edição. */
  aviso?: string
}

export function FormularioTarefa({ titulo, valoresIniciais, aoSalvar, aoCancelar, aviso }: Props) {
  const [valores, setValores] = useState<DadosTarefa>(valoresIniciais)
  const [erros, setErros] = useState<Record<string, string>>({})
  const [erroGeral, setErroGeral] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  // O botão desabilitado só vale após a nova renderização; o ref barra o segundo clique antes disso.
  const enviandoAgora = useRef(false)

  const alterar = <C extends keyof DadosTarefa>(campo: C, valor: DadosTarefa[C]) =>
    setValores((atuais) => ({ ...atuais, [campo]: valor }))

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    if (enviandoAgora.current) {
      return
    }
    enviandoAgora.current = true
    setEnviando(true)
    setErros({})
    setErroGeral(null)
    try {
      await aoSalvar(valores)
    } catch (erro) {
      if (!(erro instanceof ErroValidacao)) {
        throw erro
      }
      setErros(erro.erros)
      if (Object.keys(erro.erros).length === 0) {
        setErroGeral('Não foi possível salvar: verifique os dados informados.')
      }
    } finally {
      enviandoAgora.current = false
      setEnviando(false)
    }
  }

  const propsDoCampo = (campo: keyof DadosTarefa) => ({
    id: `campo-${campo}`,
    'aria-invalid': erros[campo] ? true : undefined,
    'aria-describedby': erros[campo] ? `erro-${campo}` : undefined,
  })

  const mensagem = (campo: keyof DadosTarefa) =>
    erros[campo] ? (
      <span id={`erro-${campo}`} className="erro-campo">
        {erros[campo]}
      </span>
    ) : null

  return (
    <form className="formulario" aria-label={titulo} onSubmit={enviar} noValidate>
      <h2>{titulo}</h2>
      {aviso && !erroGeral && (
        <p className="aviso" role="alert">
          {aviso}
        </p>
      )}
      {erroGeral && (
        <p className="erro-geral" role="alert">
          {erroGeral}
        </p>
      )}
      <label htmlFor="campo-titulo">Título</label>
      <input
        {...propsDoCampo('titulo')}
        type="text"
        maxLength={100}
        value={valores.titulo}
        onChange={(e) => alterar('titulo', e.target.value)}
      />
      {mensagem('titulo')}

      <label htmlFor="campo-descricao">Descrição</label>
      <textarea
        {...propsDoCampo('descricao')}
        maxLength={2000}
        rows={3}
        value={valores.descricao}
        onChange={(e) => alterar('descricao', e.target.value)}
      />
      {mensagem('descricao')}

      <label htmlFor="campo-dataVencimento">Vencimento</label>
      <input
        {...propsDoCampo('dataVencimento')}
        type="date"
        value={valores.dataVencimento}
        onChange={(e) => alterar('dataVencimento', e.target.value)}
      />
      {mensagem('dataVencimento')}

      <label htmlFor="campo-prioridade">Prioridade</label>
      <select
        {...propsDoCampo('prioridade')}
        value={valores.prioridade}
        onChange={(e) => alterar('prioridade', e.target.value as Prioridade)}
      >
        <option value="BAIXA">Baixa</option>
        <option value="MEDIA">Média</option>
        <option value="ALTA">Alta</option>
      </select>
      {mensagem('prioridade')}

      <div className="formulario-acoes">
        <button type="submit" disabled={enviando}>
          Salvar
        </button>
        <button type="button" onClick={aoCancelar} disabled={enviando}>
          Cancelar
        </button>
      </div>
    </form>
  )
}
