package br.com.demo.todo.tarefa;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TarefaRepository extends JpaRepository<Tarefa, UUID>, JpaSpecificationExecutor<Tarefa> {

    /** Tarefas com o status dado e concluídas no intervalo semiaberto [inicio, fim). */
    List<Tarefa> findByStatusAndConcluidaEmGreaterThanEqualAndConcluidaEmLessThan(
            StatusTarefa status, Instant inicio, Instant fim);
}
