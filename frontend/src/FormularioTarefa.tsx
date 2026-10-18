import { useState, type FormEvent } from 'react'
import { ErroValidacao, type DadosTarefa, type Prioridade, type Tarefa } from './api'
import { ROTULO_PRIORIDADE } from './formatacao'

interface Props {
  /** Tarefa sendo editada; ausente para criar uma nova. */
  tarefa?: Tarefa | null
  /** Deve rejeitar com {@link ErroValidacao} quando a API recusar os dados. */
  onSalvar: (dados: DadosTarefa) => Promise<void>
  onCancelar?: () => void
}

interface Campos {
  titulo: string
  descricao: string
  dataVencimento: string
  prioridade: Prioridade | ''
}

const CAMPOS_VAZIOS: Campos = { titulo: '', descricao: '', dataVencimento: '', prioridade: '' }

function camposDe(tarefa: Tarefa | null | undefined): Campos {
  if (!tarefa) {
    return CAMPOS_VAZIOS
  }
  return {
    titulo: tarefa.titulo,
    descricao: tarefa.descricao ?? '',
    dataVencimento: tarefa.dataVencimento,
    prioridade: tarefa.prioridade,
  }
}

const PRIORIDADES: Prioridade[] = ['ALTA', 'MEDIA', 'BAIXA']

/**
 * Formulário de criação e edição. Não valida no cliente: envia o que foi digitado
 * e mostra junto a cada campo a mensagem que a API devolver.
 */
export function FormularioTarefa({ tarefa, onSalvar, onCancelar }: Props) {
  const editando = Boolean(tarefa)
  const [campos, setCampos] = useState<Campos>(() => camposDe(tarefa))
  const [erros, setErros] = useState<Record<string, string>>({})
  const [erroGeral, setErroGeral] = useState<string | null>(null)
  const [salvando, setSalvando] = useState(false)

  function alterar<K extends keyof Campos>(campo: K, valor: Campos[K]) {
    setCampos((atuais) => ({ ...atuais, [campo]: valor }))
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setSalvando(true)
    setErros({})
    setErroGeral(null)
    try {
      await onSalvar({
        titulo: campos.titulo,
        descricao: campos.descricao === '' ? null : campos.descricao,
        dataVencimento: campos.dataVencimento === '' ? null : campos.dataVencimento,
        prioridade: campos.prioridade === '' ? null : campos.prioridade,
      })
      if (!editando) {
        setCampos(CAMPOS_VAZIOS)
      }
    } catch (erro) {
      if (erro instanceof ErroValidacao) {
        setErros(erro.erros)
      } else {
        setErroGeral(erro instanceof Error ? erro.message : String(erro))
      }
    } finally {
      setSalvando(false)
    }
  }

  return (
    <form className="formulario" onSubmit={enviar} noValidate aria-label={editando ? 'Editar tarefa' : 'Nova tarefa'}>
      <h2>{editando ? 'Editar tarefa' : 'Nova tarefa'}</h2>

      {erroGeral && (
        <p className="erro-geral" role="alert">
          {erroGeral}
        </p>
      )}

      <div className="campo">
        <label htmlFor="titulo">Título</label>
        <input
          id="titulo"
          value={campos.titulo}
          onChange={(e) => alterar('titulo', e.target.value)}
          aria-invalid={Boolean(erros.titulo)}
          aria-describedby={erros.titulo ? 'erro-titulo' : undefined}
        />
        {erros.titulo && (
          <span id="erro-titulo" className="erro-campo">
            {erros.titulo}
          </span>
        )}
      </div>

      <div className="campo">
        <label htmlFor="descricao">Descrição</label>
        <textarea
          id="descricao"
          value={campos.descricao}
          onChange={(e) => alterar('descricao', e.target.value)}
          aria-invalid={Boolean(erros.descricao)}
          aria-describedby={erros.descricao ? 'erro-descricao' : undefined}
        />
        {erros.descricao && (
          <span id="erro-descricao" className="erro-campo">
            {erros.descricao}
          </span>
        )}
      </div>

      <div className="campo">
        <label htmlFor="dataVencimento">Vencimento</label>
        <input
          id="dataVencimento"
          type="date"
          value={campos.dataVencimento}
          onChange={(e) => alterar('dataVencimento', e.target.value)}
          aria-invalid={Boolean(erros.dataVencimento)}
          aria-describedby={erros.dataVencimento ? 'erro-dataVencimento' : undefined}
        />
        {erros.dataVencimento && (
          <span id="erro-dataVencimento" className="erro-campo">
            {erros.dataVencimento}
          </span>
        )}
      </div>

      <div className="campo">
        <label htmlFor="prioridade">Prioridade</label>
        <select
          id="prioridade"
          value={campos.prioridade}
          onChange={(e) => alterar('prioridade', e.target.value as Prioridade | '')}
          aria-invalid={Boolean(erros.prioridade)}
          aria-describedby={erros.prioridade ? 'erro-prioridade' : undefined}
        >
          <option value="">Selecione</option>
          {PRIORIDADES.map((prioridade) => (
            <option key={prioridade} value={prioridade}>
              {ROTULO_PRIORIDADE[prioridade]}
            </option>
          ))}
        </select>
        {erros.prioridade && (
          <span id="erro-prioridade" className="erro-campo">
            {erros.prioridade}
          </span>
        )}
      </div>

      <div className="formulario-acoes">
        <button type="submit" className="primario" disabled={salvando}>
          {editando ? 'Salvar alterações' : 'Adicionar tarefa'}
        </button>
        {editando && onCancelar && (
          <button type="button" onClick={onCancelar}>
            Cancelar
          </button>
        )}
      </div>
    </form>
  )
}
