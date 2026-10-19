import { useEffect, useState } from 'react'
import { listarTarefas, type Tarefa } from './api'
import { PARAMETROS_DO_FILTRO, type Filtro } from './filtros'

/** Estado da tela: filtro ativo e a lista de tarefas correspondente, carregada da API. */
export function useTarefas() {
  const [filtro, setFiltro] = useState<Filtro>('TODAS')
  const [tarefas, setTarefas] = useState<Tarefa[] | null>(null)

  useEffect(() => {
    let ativo = true
    listarTarefas(PARAMETROS_DO_FILTRO[filtro]).then((lista) => {
      if (ativo) {
        setTarefas(lista)
      }
    })
    return () => {
      ativo = false
    }
  }, [filtro])

  return { tarefas, filtro, setFiltro }
}
