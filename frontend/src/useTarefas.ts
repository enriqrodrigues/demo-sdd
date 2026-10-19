import { useEffect, useState } from 'react'
import { consultarScore, listarTarefas, type Score, type Tarefa } from './api'
import { PARAMETROS_DO_FILTRO, type Filtro } from './filtros'

/** Estado da tela: filtro ativo, a lista de tarefas correspondente e o score da semana. */
export function useTarefas() {
  const [filtro, setFiltro] = useState<Filtro>('TODAS')
  const [tarefas, setTarefas] = useState<Tarefa[] | null>(null)
  const [score, setScore] = useState<Score | null>(null)

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

  useEffect(() => {
    let ativo = true
    consultarScore().then((atual) => {
      if (ativo) {
        setScore(atual)
      }
    })
    return () => {
      ativo = false
    }
  }, [])

  return { tarefas, filtro, setFiltro, score }
}
