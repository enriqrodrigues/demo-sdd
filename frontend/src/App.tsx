import { useState } from 'react'
import type { DadosTarefa } from './api'
import { CabecalhoScore } from './componentes/CabecalhoScore'
import { Filtros } from './componentes/Filtros'
import { FormularioTarefa } from './componentes/FormularioTarefa'
import { ListaTarefas } from './componentes/ListaTarefas'
import { useTarefas } from './useTarefas'

const TAREFA_EM_BRANCO: DadosTarefa = { titulo: '', descricao: '', dataVencimento: '', prioridade: 'MEDIA' }

export default function App() {
  const { tarefas, filtro, setFiltro, score, criar } = useTarefas()
  const [criando, setCriando] = useState(false)

  async function salvarNova(dados: DadosTarefa) {
    await criar(dados)
    setCriando(false)
  }

  return (
    <div className="app">
      <header className="cabecalho">
        <h1>To-Do Inteligente</h1>
        <CabecalhoScore score={score} />
      </header>
      <main>
        {criando ? (
          <FormularioTarefa
            titulo="Nova tarefa"
            valoresIniciais={TAREFA_EM_BRANCO}
            aoSalvar={salvarNova}
            aoCancelar={() => setCriando(false)}
          />
        ) : (
          <button type="button" className="botao-principal" onClick={() => setCriando(true)}>
            Nova tarefa
          </button>
        )}
        <Filtros ativo={filtro} aoSelecionar={setFiltro} />
        {tarefas === null ? <p>Carregando…</p> : <ListaTarefas tarefas={tarefas} />}
      </main>
    </div>
  )
}
