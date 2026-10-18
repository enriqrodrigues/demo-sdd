import { useCallback, useEffect, useState } from 'react'
import {
  MENSAGEM_FALHA_COMUNICACAO,
  atualizarTarefa,
  buscarScore,
  criarTarefa,
  excluirTarefa,
  listarTarefas,
  type DadosTarefa,
  type Score,
  type Tarefa,
} from './api'
import { CardScore } from './CardScore'
import { FormularioTarefa } from './FormularioTarefa'
import { ListaTarefas } from './ListaTarefas'
import './App.css'

function mensagemDe(erro: unknown): string {
  return erro instanceof Error ? erro.message : MENSAGEM_FALHA_COMUNICACAO
}

function App() {
  const [tarefas, setTarefas] = useState<Tarefa[] | null>(null)
  const [score, setScore] = useState<Score | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [emEdicao, setEmEdicao] = useState<Tarefa | null>(null)

  // a situação de prazo e o score vêm sempre da API: depois de cada alteração, recarrega os dois
  const recarregar = useCallback(async () => {
    try {
      const [novasTarefas, novoScore] = await Promise.all([listarTarefas(), buscarScore()])
      setTarefas(novasTarefas)
      setScore(novoScore)
      setErro(null)
    } catch (e) {
      setErro(mensagemDe(e))
    }
  }, [])

  useEffect(() => {
    // carga inicial vinda da API; os setState só acontecem depois do await
    // oxlint-disable-next-line react/set-state-in-effect
    void recarregar()
  }, [recarregar])

  async function executar(operacao: () => Promise<unknown>) {
    try {
      await operacao()
    } catch (e) {
      setErro(mensagemDe(e))
      return
    }
    await recarregar()
  }

  async function salvar(dados: DadosTarefa) {
    // erros de validação sobem para o formulário, que os mostra junto aos campos
    if (emEdicao) {
      await atualizarTarefa(emEdicao.id, { ...dados, status: emEdicao.status })
      setEmEdicao(null)
    } else {
      await criarTarefa(dados)
    }
    await recarregar()
  }

  function alternarStatus(tarefa: Tarefa) {
    void executar(() =>
      atualizarTarefa(tarefa.id, {
        titulo: tarefa.titulo,
        descricao: tarefa.descricao,
        dataVencimento: tarefa.dataVencimento,
        prioridade: tarefa.prioridade,
        status: tarefa.status === 'CONCLUIDA' ? 'PENDENTE' : 'CONCLUIDA',
      }),
    )
  }

  function excluir(tarefa: Tarefa) {
    if (emEdicao?.id === tarefa.id) {
      setEmEdicao(null)
    }
    void executar(() => excluirTarefa(tarefa.id))
  }

  return (
    <main className="app">
      <header>
        <h1>To-Do Inteligente</h1>
      </header>

      {erro && (
        <p className="erro-geral" role="alert">
          {erro}
        </p>
      )}

      <div className="layout">
        <div className="coluna-lateral">
          {score && <CardScore score={score} />}
          <FormularioTarefa
            key={emEdicao?.id ?? 'nova'}
            tarefa={emEdicao}
            onSalvar={salvar}
            onCancelar={() => setEmEdicao(null)}
          />
        </div>

        <section className="coluna-principal" aria-label="Lista de tarefas">
          <h2>Tarefas</h2>
          {tarefas === null ? (
            !erro && <p className="carregando">Carregando…</p>
          ) : (
            <ListaTarefas
              tarefas={tarefas}
              onAlternarStatus={alternarStatus}
              onEditar={setEmEdicao}
              onExcluir={excluir}
            />
          )}
        </section>
      </div>
    </main>
  )
}

export default App
