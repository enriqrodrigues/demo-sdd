import { CabecalhoScore } from './componentes/CabecalhoScore'
import { Filtros } from './componentes/Filtros'
import { ListaTarefas } from './componentes/ListaTarefas'
import { useTarefas } from './useTarefas'

export default function App() {
  const { tarefas, filtro, setFiltro, score } = useTarefas()

  return (
    <div className="app">
      <header className="cabecalho">
        <h1>To-Do Inteligente</h1>
        <CabecalhoScore score={score} />
      </header>
      <main>
        <Filtros ativo={filtro} aoSelecionar={setFiltro} />
        {tarefas === null ? <p>Carregando…</p> : <ListaTarefas tarefas={tarefas} />}
      </main>
    </div>
  )
}
