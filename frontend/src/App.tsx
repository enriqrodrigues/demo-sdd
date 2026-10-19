import { useRef, useState } from 'react'
import { ErroConflito, ErroNaoEncontrado, type DadosTarefa, type Tarefa } from './api'
import { CabecalhoScore } from './componentes/CabecalhoScore'
import { DialogoConfirmacao } from './componentes/DialogoConfirmacao'
import { Filtros } from './componentes/Filtros'
import { FormularioTarefa } from './componentes/FormularioTarefa'
import { ListaTarefas } from './componentes/ListaTarefas'
import { useTarefas } from './useTarefas'

const TAREFA_EM_BRANCO: DadosTarefa = { titulo: '', descricao: '', dataVencimento: '', prioridade: 'MEDIA' }

/** {@code abertura} identifica cada abertura do formulário, para ignorar resultados de uma anterior. */
type EstadoFormulario =
  | { modo: 'fechado' }
  | { modo: 'criar'; abertura: number }
  | { modo: 'editar'; abertura: number; tarefa: Tarefa; aviso?: string }

const AVISO_CONFLITO =
  'Esta tarefa foi alterada em outro lugar. Ela foi recarregada; revise os dados e salve de novo.'

const AVISO_CONFLITO_SEM_RECARGA =
  'Esta tarefa foi alterada em outro lugar, mas não foi possível recarregá-la agora. Tente salvar de novo.'

function dadosDa(tarefa: Tarefa): DadosTarefa {
  return {
    titulo: tarefa.titulo,
    descricao: tarefa.descricao ?? '',
    dataVencimento: tarefa.dataVencimento,
    prioridade: tarefa.prioridade,
  }
}

export default function App() {
  const {
    tarefas,
    falhaNaCarga,
    falhaNoScore,
    tentarDeNovo,
    filtro,
    setFiltro,
    score,
    criar,
    editar,
    recarregarTarefa,
    removerInexistente,
    emOperacao,
    alternarConclusao,
    excluir,
    aviso,
    limparAviso,
  } = useTarefas()
  const [formulario, setFormulario] = useState<EstadoFormulario>({ modo: 'fechado' })
  const aberturaAtual = useRef(0)
  const [paraExcluir, setParaExcluir] = useState<Tarefa | null>(null)

  function abrirCriacao() {
    aberturaAtual.current += 1
    setFormulario({ modo: 'criar', abertura: aberturaAtual.current })
  }

  function abrirEdicao(tarefa: Tarefa) {
    aberturaAtual.current += 1
    setFormulario({ modo: 'editar', abertura: aberturaAtual.current, tarefa })
  }

  const fechar = () => setFormulario({ modo: 'fechado' })

  /** Fecha só se o formulário exibido ainda é o daquela abertura (o usuário pode ter aberto outro). */
  function fecharSeAinda(abertura: number) {
    if (aberturaAtual.current === abertura) {
      fechar()
    }
  }

  function confirmarExclusao() {
    if (paraExcluir) {
      excluir(paraExcluir)
      setParaExcluir(null)
    }
  }

  async function salvarNova(abertura: number, dados: DadosTarefa) {
    await criar(dados)
    fecharSeAinda(abertura)
  }

  async function salvarEdicao(abertura: number, tarefa: Tarefa, dados: DadosTarefa) {
    try {
      await editar(tarefa, dados)
      fecharSeAinda(abertura)
    } catch (erro) {
      if (erro instanceof ErroNaoEncontrado) {
        // A tarefa já saiu da lista e o aviso foi exibido; não há mais o que editar.
        fecharSeAinda(abertura)
        return
      }
      if (!(erro instanceof ErroConflito)) {
        throw erro
      }
      await tratarConflito(abertura, tarefa)
    }
  }

  /** Mantém o formulário aberto com o que foi digitado; o próximo salvar usa a versão recarregada. */
  async function tratarConflito(abertura: number, tarefa: Tarefa) {
    let atual = tarefa
    let aviso = AVISO_CONFLITO
    try {
      atual = await recarregarTarefa(tarefa.id)
    } catch (erro) {
      if (erro instanceof ErroNaoEncontrado) {
        removerInexistente(tarefa.id)
        fecharSeAinda(abertura)
        return
      }
      aviso = AVISO_CONFLITO_SEM_RECARGA
    }
    if (aberturaAtual.current === abertura) {
      setFormulario({ modo: 'editar', abertura, tarefa: atual, aviso })
    }
  }

  return (
    <div className="app">
      <header className="cabecalho">
        <h1>To-Do Inteligente</h1>
        <CabecalhoScore score={score} indisponivel={falhaNoScore} />
      </header>
      <main>
        {aviso && (
          <div key={aviso.chave} className="aviso aviso-global" role="alert">
            <span>{aviso.texto}</span>
            <button type="button" onClick={limparAviso}>
              Fechar
            </button>
          </div>
        )}
        {formulario.modo === 'criar' && (
          <FormularioTarefa
            key={formulario.abertura}
            titulo="Nova tarefa"
            valoresIniciais={TAREFA_EM_BRANCO}
            aoSalvar={(dados) => salvarNova(formulario.abertura, dados)}
            aoCancelar={fechar}
          />
        )}
        {formulario.modo === 'editar' && (
          <FormularioTarefa
            key={formulario.abertura}
            titulo="Editar tarefa"
            valoresIniciais={dadosDa(formulario.tarefa)}
            aoSalvar={(dados) => salvarEdicao(formulario.abertura, formulario.tarefa, dados)}
            aoCancelar={fechar}
            aviso={formulario.aviso}
          />
        )}
        {formulario.modo === 'fechado' && (
          <button type="button" className="botao-principal" onClick={abrirCriacao}>
            Nova tarefa
          </button>
        )}
        <Filtros ativo={filtro} aoSelecionar={setFiltro} />
        {tarefas === null ? (
          falhaNaCarga ? (
            <button type="button" onClick={tentarDeNovo}>
              Tentar novamente
            </button>
          ) : (
            <p>Carregando…</p>
          )
        ) : (
          <ListaTarefas
            tarefas={tarefas}
            idEmEdicao={formulario.modo === 'editar' ? formulario.tarefa.id : undefined}
            emOperacao={emOperacao}
            aoEditar={abrirEdicao}
            aoAlternarConclusao={alternarConclusao}
            aoExcluir={setParaExcluir}
          />
        )}
      </main>
      {paraExcluir && (
        <DialogoConfirmacao
          titulo="Excluir tarefa?"
          rotuloConfirmar="Excluir"
          aoConfirmar={confirmarExclusao}
          aoCancelar={() => setParaExcluir(null)}
        >
          <p>
            A tarefa <strong>{paraExcluir.titulo}</strong> será excluída definitivamente.
          </p>
        </DialogoConfirmacao>
      )}
    </div>
  )
}
