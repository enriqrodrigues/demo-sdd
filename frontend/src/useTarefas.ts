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
import { atendeAoFiltro, PARAMETROS_DO_FILTRO, type Filtro } from './filtros'
import { FALHA_COMUNICACAO, TAREFA_ALTERADA, TAREFA_REMOVIDA } from './mensagens'

/** Mensagem para o usuário; a chave muda a cada aviso para que leitores de tela o anunciem de novo. */
export interface Aviso {
  texto: string
  chave: number
}

/**
 * Estado da tela: filtro ativo, lista de tarefas, score da semana, tarefas com requisição em
 * andamento e o aviso exibido ao usuário.
 */
export function useTarefas() {
  const [filtro, setFiltro] = useState<Filtro>('TODAS')
  const [tarefas, setTarefas] = useState<Tarefa[] | null>(null)
  const [score, setScore] = useState<Score | null>(null)
  const [falhaNaCarga, setFalhaNaCarga] = useState(false)
  const [falhaNoScore, setFalhaNoScore] = useState(false)
  const [aviso, setAviso] = useState<Aviso | null>(null)
  // Incrementar a geração faz lista e score serem consultados de novo.
  const [geracao, setGeracao] = useState(0)

  const avisar = useCallback((texto: string) => setAviso((anterior) => ({ texto, chave: (anterior?.chave ?? 0) + 1 })), [])
  const limparAviso = useCallback(() => setAviso(null), [])

  // Filtro da lista exibida; se a carga de outro filtro falhar, a lista exibida não corresponde a ele.
  const filtroCarregado = useRef<Filtro | null>(null)

  useEffect(() => {
    let ativo = true
    listarTarefas(PARAMETROS_DO_FILTRO[filtro])
      .then((lista) => {
        if (ativo) {
          filtroCarregado.current = filtro
          setTarefas(lista)
          setFalhaNaCarga(false)
        }
      })
      .catch(() => {
        if (ativo) {
          if (filtroCarregado.current !== filtro) {
            // A lista exibida é de outro filtro: some até a carga dar certo.
            setTarefas(null)
            setFalhaNaCarga(true)
          }
          avisar(FALHA_COMUNICACAO)
        }
      })
    return () => {
      ativo = false
    }
  }, [filtro, geracao, avisar])

  useEffect(() => {
    let ativo = true
    consultarScore()
      .then((atual) => {
        if (ativo) {
          setScore(atual)
          setFalhaNoScore(false)
        }
      })
      .catch(() => {
        if (ativo) {
          setFalhaNoScore(true)
          avisar(FALHA_COMUNICACAO)
        }
      })
    return () => {
      ativo = false
    }
  }, [geracao, avisar])

  const recarregar = useCallback(() => setGeracao((g) => g + 1), [])

  const tentarDeNovo = useCallback(() => {
    limparAviso()
    recarregar()
  }, [limparAviso, recarregar])

  /** A API respondeu 404: a tarefa foi excluída em outro lugar e sai da lista. */
  const removerInexistente = useCallback(
    (id: string) => {
      setTarefas((lista) => lista?.filter((t) => t.id !== id) ?? null)
      avisar(TAREFA_REMOVIDA)
      recarregar()
    },
    [avisar, recarregar],
  )

  /** Se a nova tarefa não atende ao filtro ativo, volta para "Todas" para que ela apareça. */
  const criar = useCallback(
    async (dados: DadosTarefa) => {
      const criada = await criarTarefa(dados)
      limparAviso()
      if (!atendeAoFiltro(criada, filtro)) {
        setFiltro('TODAS')
      }
      recarregar()
    },
    [filtro, limparAviso, recarregar],
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
      limparAviso()
      recarregar()
    },
    [limparAviso, recarregar, removerInexistente],
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
        limparAviso()
      } catch (erro) {
        if (erro instanceof ErroNaoEncontrado) {
          removerInexistente(id)
        } else if (erro instanceof ErroConflito) {
          avisar(TAREFA_ALTERADA)
          recarregar()
        } else {
          avisar(FALHA_COMUNICACAO)
        }
      } finally {
        operacoesAtivas.current.delete(id)
        setEmOperacao(new Set(operacoesAtivas.current))
      }
    },
    [avisar, limparAviso, recarregar, removerInexistente],
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

  return {
    tarefas,
    falhaNaCarga,
    falhaNoScore,
    tentarDeNovo,
    filtro,
    setFiltro,
    score,
    criar,
    editar,
    recarregarTarefa,
    removerInexistente,
    emOperacao,
    alternarConclusao,
    excluir,
    aviso,
    limparAviso,
  }
}
