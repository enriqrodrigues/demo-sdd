package br.com.demosdd.todo.tarefa;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TarefaRepository extends JpaRepository<Tarefa, UUID> {

    /** Pendentes antes das concluídas; depois por vencimento e título. */
    @Query("""
            select t from Tarefa t
            order by case when t.status = br.com.demosdd.todo.tarefa.StatusTarefa.PENDENTE then 0 else 1 end,
                     t.dataVencimento asc,
                     t.titulo asc
            """)
    List<Tarefa> listarOrdenado();

    List<Tarefa> findByStatusAndDataConclusaoBetween(StatusTarefa status, LocalDate inicio, LocalDate fim);
}
