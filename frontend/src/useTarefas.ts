import { useCallback, useEffect, useRef, useState } from 'react'
import {
  concluirTarefa,
  consultarScore,
  consultarTarefa,
  criarTarefa,
  editarTarefa,
  excluirTarefa,
  listarTarefas,
  reabrirTarefa,
  type DadosTarefa,
  type Score,
  type Tarefa,
} from './api'
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

  const editar = useCallback(
    async (tarefa: Tarefa, dados: DadosTarefa) => {
      await editarTarefa(tarefa.id, dados, tarefa.version)
      recarregar()
    },
    [recarregar],
  )

  /** Busca a versão atual de uma tarefa e atualiza lista e score. */
  const recarregarTarefa = useCallback(
    async (id: string) => {
      const atual = await consultarTarefa(id)
      recarregar()
      return atual
    },
    [recarregar],
  )

  const [emOperacao, setEmOperacao] = useState<ReadonlySet<string>>(new Set())
  // O estado só reflete após renderizar; o ref barra um segundo clique no mesmo instante.
  const operacoesAtivas = useRef(new Set<string>())

  /** Executa uma ação sobre a tarefa, com o controle dela bloqueado até a resposta. */
  const operar = useCallback(async (id: string, acao: () => Promise<void>) => {
    if (operacoesAtivas.current.has(id)) {
      return
    }
    operacoesAtivas.current.add(id)
    setEmOperacao(new Set(operacoesAtivas.current))
    try {
      await acao()
    } finally {
      operacoesAtivas.current.delete(id)
      setEmOperacao(new Set(operacoesAtivas.current))
    }
  }, [])

  const alternarConclusao = useCallback(
    (tarefa: Tarefa) =>
      operar(tarefa.id, async () => {
        const atualizada =
          tarefa.status === 'CONCLUIDA' ? await reabrirTarefa(tarefa.id) : await concluirTarefa(tarefa.id)
        setTarefas((lista) => lista?.map((t) => (t.id === atualizada.id ? atualizada : t)) ?? null)
        // Lista (a tarefa pode sair do filtro ativo) e score são consultados de novo.
        recarregar()
      }),
    [operar, recarregar],
  )

  const excluir = useCallback(
    (tarefa: Tarefa) =>
      operar(tarefa.id, async () => {
        await excluirTarefa(tarefa.id)
        setTarefas((lista) => lista?.filter((t) => t.id !== tarefa.id) ?? null)
        recarregar()
      }),
    [operar, recarregar],
  )

  return {
    tarefas,
    filtro,
    setFiltro,
    score,
    criar,
    editar,
    recarregarTarefa,
    emOperacao,
    alternarConclusao,
    excluir,
  }
}
