package com.demosdd.todo.tarefa;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TarefaRepository extends JpaRepository<Tarefa, UUID> {

    @Query("""
            select count(t) from Tarefa t
            where t.status = com.demosdd.todo.tarefa.StatusTarefa.CONCLUIDA
              and t.dataConclusao between :inicio and :fim
            """)
    long contarConcluidasEntre(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("""
            select count(t) from Tarefa t
            where t.status = com.demosdd.todo.tarefa.StatusTarefa.CONCLUIDA
              and t.dataConclusao between :inicio and :fim
              and t.dataConclusao <= t.dataVencimento
            """)
    long contarConcluidasNoPrazoEntre(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}
