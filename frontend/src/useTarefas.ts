import { useCallback, useEffect, useRef, useState } from 'react'
import {
  concluirTarefa,
  consultarScore,
  consultarTarefa,
  criarTarefa,
  editarTarefa,
  ErroConflito,
  ErroNaoEncontrado,
  excluirTarefa,
  listarTarefas,
  reabrirTarefa,
  type DadosTarefa,
  type Score,
  type Tarefa,
} from './api'
import { PARAMETROS_DO_FILTRO, type Filtro } from './filtros'
import { FALHA_COMUNICACAO, TAREFA_ALTERADA, TAREFA_REMOVIDA } from './mensagens'

/**
 * Estado da tela: filtro ativo, lista de tarefas, score da semana, tarefas com requisição em
 * andamento e o aviso exibido ao usuário.
 */
export function useTarefas() {
  const [filtro, setFiltro] = useState<Filtro>('TODAS')
  const [tarefas, setTarefas] = useState<Tarefa[] | null>(null)
  const [score, setScore] = useState<Score | null>(null)
  const [falhaNaCarga, setFalhaNaCarga] = useState(false)
  const [aviso, setAviso] = useState<string | null>(null)
  // Incrementar a geração faz lista e score serem consultados de novo.
  const [geracao, setGeracao] = useState(0)

  useEffect(() => {
    let ativo = true
    listarTarefas(PARAMETROS_DO_FILTRO[filtro])
      .then((lista) => {
        if (ativo) {
          setTarefas(lista)
          setFalhaNaCarga(false)
        }
      })
      .catch(() => {
        if (ativo) {
          setFalhaNaCarga(true)
          setAviso(FALHA_COMUNICACAO)
        }
      })
    return () => {
      ativo = false
    }
  }, [filtro, geracao])

  useEffect(() => {
    let ativo = true
    consultarScore()
      .then((atual) => {
        if (ativo) {
          setScore(atual)
        }
      })
      .catch(() => {
        if (ativo) {
          setAviso(FALHA_COMUNICACAO)
        }
      })
    return () => {
      ativo = false
    }
  }, [geracao])

  const recarregar = useCallback(() => setGeracao((g) => g + 1), [])

  /** A API respondeu 404: a tarefa foi excluída em outro lugar e sai da lista. */
  const removerInexistente = useCallback(
    (id: string) => {
      setTarefas((lista) => lista?.filter((t) => t.id !== id) ?? null)
      setAviso(TAREFA_REMOVIDA)
      recarregar()
    },
    [recarregar],
  )

  const criar = useCallback(
    async (dados: DadosTarefa) => {
      await criarTarefa(dados)
      recarregar()
    },
    [recarregar],
  )

  /** Rejeita com ErroNaoEncontrado (depois de remover a tarefa) para que o formulário seja fechado. */
  const editar = useCallback(
    async (tarefa: Tarefa, dados: DadosTarefa) => {
      try {
        await editarTarefa(tarefa.id, dados, tarefa.version)
      } catch (erro) {
        if (erro instanceof ErroNaoEncontrado) {
          removerInexistente(tarefa.id)
        }
        throw erro
      }
      recarregar()
    },
    [recarregar, removerInexistente],
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

  /**
   * Executa uma ação sobre a tarefa com o controle dela bloqueado até a resposta. Falhas viram
   * aviso: 404 remove a tarefa; 409 recarrega a lista; rede ou 5xx mantêm o estado exibido.
   */
  const operar = useCallback(
    async (id: string, acao: () => Promise<void>) => {
      if (operacoesAtivas.current.has(id)) {
        return
      }
      operacoesAtivas.current.add(id)
      setEmOperacao(new Set(operacoesAtivas.current))
      try {
        await acao()
      } catch (erro) {
        if (erro instanceof ErroNaoEncontrado) {
          removerInexistente(id)
        } else if (erro instanceof ErroConflito) {
          setAviso(TAREFA_ALTERADA)
          recarregar()
        } else {
          setAviso(FALHA_COMUNICACAO)
        }
      } finally {
        operacoesAtivas.current.delete(id)
        setEmOperacao(new Set(operacoesAtivas.current))
      }
    },
    [recarregar, removerInexistente],
  )

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

  const limparAviso = useCallback(() => setAviso(null), [])

  return {
    tarefas,
    falhaNaCarga,
    filtro,
    setFiltro,
    score,
    criar,
    editar,
    recarregarTarefa,
    emOperacao,
    alternarConclusao,
    excluir,
    aviso,
    limparAviso,
  }
}
