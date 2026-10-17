import { useState, type FormEvent } from 'react';
import { ErroApi } from '../api/tarefasApi';
import { ROTULOS_PRIORIDADE } from '../formatacao';
import type { Prioridade, Tarefa, TarefaInput } from '../tipos';

type CampoFormulario = 'titulo' | 'descricao' | 'dataVencimento' | 'prioridade';
type ErrosCampos = Partial<Record<CampoFormulario, string>>;

const CAMPOS_FORMULARIO: string[] = ['titulo', 'descricao', 'dataVencimento', 'prioridade'];

/**
 * O formulário lê os valores iniciais só na montagem. O pai deve renderizar com
 * key={tarefa?.id ?? 'nova'} para recarregar os campos quando a tarefa em edição mudar.
 */
interface Props {
  tarefaEmEdicao: Tarefa | null;
  onSalvar: (dados: TarefaInput) => Promise<void>;
  onCancelar: () => void;
}

const PRIORIDADES = Object.keys(ROTULOS_PRIORIDADE) as Prioridade[];

export function TarefaForm({ tarefaEmEdicao, onSalvar, onCancelar }: Props) {
  const [titulo, setTitulo] = useState(tarefaEmEdicao?.titulo ?? '');
  const [descricao, setDescricao] = useState(tarefaEmEdicao?.descricao ?? '');
  const [dataVencimento, setDataVencimento] = useState(tarefaEmEdicao?.dataVencimento ?? '');
  const [prioridade, setPrioridade] = useState<Prioridade>(tarefaEmEdicao?.prioridade ?? 'MEDIA');
  const [erros, setErros] = useState<ErrosCampos>({});
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  function validar(): ErrosCampos {
    const encontrados: ErrosCampos = {};
    const tituloLimpo = titulo.trim();
    if (tituloLimpo === '') {
      encontrados.titulo = 'Título é obrigatório';
    } else if (tituloLimpo.length > 100) {
      encontrados.titulo = 'Título deve ter no máximo 100 caracteres';
    }
    if (dataVencimento === '') {
      encontrados.dataVencimento = 'Data de vencimento é obrigatória';
    }
    return encontrados;
  }

  function limpar() {
    setTitulo('');
    setDescricao('');
    setDataVencimento('');
    setPrioridade('MEDIA');
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    const encontrados = validar();
    setErros(encontrados);
    setErroGeral(null);
    if (Object.keys(encontrados).length > 0) {
      return;
    }
    setEnviando(true);
    try {
      await onSalvar({
        titulo: titulo.trim(),
        descricao: descricao.trim() === '' ? null : descricao,
        dataVencimento,
        prioridade,
        status: tarefaEmEdicao?.status ?? 'PENDENTE',
      });
      if (!tarefaEmEdicao) {
        limpar();
      }
    } catch (erro) {
      if (erro instanceof ErroApi && erro.erros.length > 0) {
        const camposConhecidos = erro.erros.filter((e) => CAMPOS_FORMULARIO.includes(e.campo));
        setErros(Object.fromEntries(camposConhecidos.map((e) => [e.campo, e.mensagem])) as ErrosCampos);
        if (camposConhecidos.length < erro.erros.length) {
          setErroGeral(erro.detail);
        }
      } else {
        setErroGeral(erro instanceof ErroApi ? erro.detail : 'Erro inesperado ao salvar.');
      }
    } finally {
      setEnviando(false);
    }
  }

  return (
    <form className="tarefa-form" onSubmit={enviar} noValidate>
      <h2>{tarefaEmEdicao ? 'Editar tarefa' : 'Nova tarefa'}</h2>

      <div className="campo">
        <label htmlFor="campo-titulo">Título</label>
        <input id="campo-titulo" value={titulo} onChange={(e) => setTitulo(e.target.value)} />
        {erros.titulo && <span className="erro-campo">{erros.titulo}</span>}
      </div>

      <div className="campo">
        <label htmlFor="campo-descricao">Descrição</label>
        <textarea id="campo-descricao" value={descricao} onChange={(e) => setDescricao(e.target.value)} />
        {erros.descricao && <span className="erro-campo">{erros.descricao}</span>}
      </div>

      <div className="campo">
        <label htmlFor="campo-vencimento">Vencimento</label>
        <input
          id="campo-vencimento"
          type="date"
          value={dataVencimento}
          onChange={(e) => setDataVencimento(e.target.value)}
        />
        {erros.dataVencimento && <span className="erro-campo">{erros.dataVencimento}</span>}
      </div>

      <div className="campo">
        <label htmlFor="campo-prioridade">Prioridade</label>
        <select
          id="campo-prioridade"
          value={prioridade}
          onChange={(e) => setPrioridade(e.target.value as Prioridade)}
        >
          {PRIORIDADES.map((p) => (
            <option key={p} value={p}>
              {ROTULOS_PRIORIDADE[p]}
            </option>
          ))}
        </select>
        {erros.prioridade && <span className="erro-campo">{erros.prioridade}</span>}
      </div>

      {erroGeral && (
        <p className="erro-campo" role="alert">
          {erroGeral}
        </p>
      )}

      <div className="acoes-form">
        <button type="submit" disabled={enviando}>
          {tarefaEmEdicao ? 'Salvar alterações' : 'Adicionar tarefa'}
        </button>
        {tarefaEmEdicao && (
          <button type="button" onClick={onCancelar}>
            Cancelar
          </button>
        )}
      </div>
    </form>
  );
}
