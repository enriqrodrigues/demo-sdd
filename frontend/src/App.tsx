import { useState } from 'react'
import { ErroConflito, type DadosTarefa, type Tarefa } from './api'
import { CabecalhoScore } from './componentes/CabecalhoScore'
import { Filtros } from './componentes/Filtros'
import { FormularioTarefa } from './componentes/FormularioTarefa'
import { ListaTarefas } from './componentes/ListaTarefas'
import { useTarefas } from './useTarefas'

const TAREFA_EM_BRANCO: DadosTarefa = { titulo: '', descricao: '', dataVencimento: '', prioridade: 'MEDIA' }

type EstadoFormulario =
  | { modo: 'fechado' }
  | { modo: 'criar' }
  | { modo: 'editar'; tarefa: Tarefa; aviso?: string }

const AVISO_CONFLITO =
  'Esta tarefa foi alterada em outro lugar. Ela foi recarregada; revise os dados e salve de novo.'

function dadosDa(tarefa: Tarefa): DadosTarefa {
  return {
    titulo: tarefa.titulo,
    descricao: tarefa.descricao ?? '',
    dataVencimento: tarefa.dataVencimento,
    prioridade: tarefa.prioridade,
  }
}

export default function App() {
  const { tarefas, filtro, setFiltro, score, criar, editar, recarregarTarefa, emOperacao, alternarConclusao } =
    useTarefas()
  const [formulario, setFormulario] = useState<EstadoFormulario>({ modo: 'fechado' })
  const fechar = () => setFormulario({ modo: 'fechado' })

  async function salvarNova(dados: DadosTarefa) {
    await criar(dados)
    fechar()
  }

  async function salvarEdicao(tarefa: Tarefa, dados: DadosTarefa) {
    try {
      await editar(tarefa, dados)
      fechar()
    } catch (erro) {
      if (!(erro instanceof ErroConflito)) {
        throw erro
      }
      // O formulário continua aberto com o que foi digitado; o próximo salvar usa a versão recarregada.
      const atual = await recarregarTarefa(tarefa.id)
      setFormulario({ modo: 'editar', tarefa: atual, aviso: AVISO_CONFLITO })
    }
  }

  return (
    <div className="app">
      <header className="cabecalho">
        <h1>To-Do Inteligente</h1>
        <CabecalhoScore score={score} />
      </header>
      <main>
        {formulario.modo === 'criar' && (
          <FormularioTarefa
            titulo="Nova tarefa"
            valoresIniciais={TAREFA_EM_BRANCO}
            aoSalvar={salvarNova}
            aoCancelar={fechar}
          />
        )}
        {formulario.modo === 'editar' && (
          <FormularioTarefa
            key={formulario.tarefa.id}
            titulo="Editar tarefa"
            valoresIniciais={dadosDa(formulario.tarefa)}
            aoSalvar={(dados) => salvarEdicao(formulario.tarefa, dados)}
            aoCancelar={fechar}
            aviso={formulario.aviso}
          />
        )}
        {formulario.modo === 'fechado' && (
          <button type="button" className="botao-principal" onClick={() => setFormulario({ modo: 'criar' })}>
            Nova tarefa
          </button>
        )}
        <Filtros ativo={filtro} aoSelecionar={setFiltro} />
        {tarefas === null ? (
          <p>Carregando…</p>
        ) : (
          <ListaTarefas
            tarefas={tarefas}
            idEmEdicao={formulario.modo === 'editar' ? formulario.tarefa.id : undefined}
            emOperacao={emOperacao}
            aoEditar={(tarefa) => setFormulario({ modo: 'editar', tarefa })}
            aoAlternarConclusao={alternarConclusao}
          />
        )}
      </main>
    </div>
  )
}
