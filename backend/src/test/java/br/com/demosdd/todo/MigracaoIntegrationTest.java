package br.com.demosdd.todo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class MigracaoIntegrationTest extends IntegracaoBase {

    @Test
    void criaTabelaTarefaVazia() {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM tarefa", Integer.class);
        assertThat(total).isZero();
    }

    @Test
    void rejeitaConcluidaSemDataConclusao() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO tarefa (id, titulo, data_vencimento, prioridade, status)
                VALUES (gen_random_uuid(), 'x', DATE '2026-10-16', 'ALTA', 'CONCLUIDA')"""))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejeitaPendenteComDataConclusao() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO tarefa (id, titulo, data_vencimento, prioridade, status, data_conclusao)
                VALUES (gen_random_uuid(), 'x', DATE '2026-10-16', 'ALTA', 'PENDENTE', DATE '2026-10-15')"""))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejeitaPrioridadeForaDoDominio() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO tarefa (id, titulo, data_vencimento, prioridade, status)
                VALUES (gen_random_uuid(), 'x', DATE '2026-10-16', 'URGENTE', 'PENDENTE')"""))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
