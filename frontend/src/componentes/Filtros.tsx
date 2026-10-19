import { OPCOES_DE_FILTRO, type Filtro } from '../filtros'

interface Props {
  ativo: Filtro
  aoSelecionar: (filtro: Filtro) => void
}

export function Filtros({ ativo, aoSelecionar }: Props) {
  return (
    <div className="filtros" role="group" aria-label="Filtrar tarefas">
      {OPCOES_DE_FILTRO.map(({ valor, rotulo }) => (
        <button
          key={valor}
          type="button"
          aria-pressed={ativo === valor}
          className={ativo === valor ? 'filtro ativo' : 'filtro'}
          onClick={() => aoSelecionar(valor)}
        >
          {rotulo}
        </button>
      ))}
    </div>
  )
}
