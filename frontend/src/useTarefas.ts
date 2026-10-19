import { useEffect, useState } from 'react'
import { listarTarefas, type Tarefa } from './api'

/** Estado da tela: lista de tarefas carregada da API. */
export function useTarefas() {
  const [tarefas, setTarefas] = useState<Tarefa[] | null>(null)

  useEffect(() => {
    let ativo = true
    listarTarefas().then((lista) => {
      if (ativo) {
        setTarefas(lista)
      }
    })
    return () => {
      ativo = false
    }
  }, [])

  return { tarefas }
}
