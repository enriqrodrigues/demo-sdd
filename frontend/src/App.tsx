import { ListaTarefas } from './componentes/ListaTarefas'
import { useTarefas } from './useTarefas'

export default function App() {
  const { tarefas } = useTarefas()

  return (
    <div className="app">
      <header className="cabecalho">
        <h1>To-Do Inteligente</h1>
      </header>
      <main>{tarefas === null ? <p>Carregando…</p> : <ListaTarefas tarefas={tarefas} />}</main>
    </div>
  )
}
