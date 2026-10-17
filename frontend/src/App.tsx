import { useCallback, useEffect, useState } from 'react';
import { ErroApi, tarefasApi } from './api/tarefasApi';
import { ScoreCard } from './componentes/ScoreCard';
import { TarefaForm } from './componentes/TarefaForm';
import { TarefaLista } from './componentes/TarefaLista';
import type { ScoreSemanal, StatusTarefa, Tarefa, TarefaInput } from './tipos';

function mensagemDe(erro: unknown): string {
  return erro instanceof ErroApi ? erro.detail : 'Erro inesperado.';
}

function comStatus(tarefa: Tarefa, status: StatusTarefa): TarefaInput {
  return {
    titulo: tarefa.titulo,
    descricao: tarefa.descricao,
    dataVencimento: tarefa.dataVencimento,
    prioridade: tarefa.prioridade,
    status,
  };
}

export function App() {
  const [tarefas, setTarefas] = useState<Tarefa[] | null>(null);
  const [score, setScore] = useState<ScoreSemanal | null>(null);
  const [emEdicao, setEmEdicao] = useState<Tarefa | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  const carregar = useCallback(async () => {
    try {
      const [lista, scoreSemanal] = await Promise.all([tarefasApi.listar(), tarefasApi.scoreSemanal()]);
      setTarefas(lista);
      setScore(scoreSemanal);
      setErro(null);
    } catch (e) {
      setErro(mensagemDe(e));
    }
  }, []);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  // Erros de salvar sobem para o TarefaForm, que os exibe junto aos campos.
  async function salvar(dados: TarefaInput) {
    if (emEdicao) {
      await tarefasApi.atualizar(emEdicao.id, dados);
    } else {
      await tarefasApi.criar(dados);
    }
    setEmEdicao(null);
    await carregar();
  }

  async function executar(acao: () => Promise<unknown>) {
    try {
      await acao();
      await carregar();
    } catch (e) {
      setErro(mensagemDe(e));
    }
  }

  function alternarStatus(tarefa: Tarefa) {
    const novoStatus: StatusTarefa = tarefa.status === 'PENDENTE' ? 'CONCLUIDA' : 'PENDENTE';
    void executar(() => tarefasApi.atualizar(tarefa.id, comStatus(tarefa, novoStatus)));
  }

  function excluir(tarefa: Tarefa) {
    void executar(async () => {
      await tarefasApi.excluir(tarefa.id);
      if (emEdicao?.id === tarefa.id) {
        setEmEdicao(null);
      }
    });
  }

  return (
    <main className="app">
      <h1>To-Do Inteligente</h1>
      {erro && (
        <div className="banner-erro" role="alert">
          {erro}
        </div>
      )}
      {score && <ScoreCard score={score} />}
      <TarefaForm
        key={emEdicao?.id ?? 'nova'}
        tarefaEmEdicao={emEdicao}
        onSalvar={salvar}
        onCancelar={() => setEmEdicao(null)}
      />
      {tarefas === null ? (
        !erro && <p>Carregando…</p>
      ) : (
        <TarefaLista tarefas={tarefas} onEditar={setEmEdicao} onAlternarStatus={alternarStatus} onExcluir={excluir} />
      )}
    </main>
  );
}
