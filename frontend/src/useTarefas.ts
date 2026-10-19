import { useCallback, useEffect, useState } from 'react'
import { consultarScore, criarTarefa, listarTarefas, type DadosTarefa, type Score, type Tarefa } from './api'
import { PARAMETROS_DO_FILTRO, type Filtro } from './filtros'

/** Estado da tela: filtro ativo, a lista de tarefas correspondente e o score da semana. */
export function useTarefas() {
  const [filtro, setFiltro] = useState<Filtro>('TODAS')
  const [tarefas, setTarefas] = useState<Tarefa[] | null>(null)
  const [score, setScore] = useState<Score | null>(null)
  // Incrementar a geração faz lista e score serem consultados de novo.
  const [geracao, setGeracao] = useState(0)

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
  }, [filtro, geracao])

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
  }, [geracao])

  const recarregar = useCallback(() => setGeracao((g) => g + 1), [])

  const criar = useCallback(
    async (dados: DadosTarefa) => {
      await criarTarefa(dados)
      recarregar()
    },
    [recarregar],
  )

  return { tarefas, filtro, setFiltro, score, criar }
}
