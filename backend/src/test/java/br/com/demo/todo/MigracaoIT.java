package br.com.demo.todo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class MigracaoIT extends TesteIntegracao {

    @Test
    void migracaoV1FoiAplicada() {
        Integer aplicadas = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version = '1' and success",
                Integer.class);

        assertThat(aplicadas).isEqualTo(1);
    }

    @Test
    void tarefaConcluidaSemConcluidaEmViolaConstraint() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into tarefa (id, titulo, data_vencimento, prioridade, status, concluida_em, version)
                values (gen_random_uuid(), 'X', date '2026-10-20', 'ALTA', 'CONCLUIDA', null, 0)
                """))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_tarefa_conclusao");
    }

    @Test
    void tarefaPendenteComConcluidaEmViolaConstraint() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into tarefa (id, titulo, data_vencimento, prioridade, status, concluida_em, version)
                values (gen_random_uuid(), 'X', date '2026-10-20', 'ALTA', 'PENDENTE', now(), 0)
                """))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_tarefa_conclusao");
    }

    @Test
    void prioridadeForaDoEnumViolaConstraint() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into tarefa (id, titulo, data_vencimento, prioridade, status, concluida_em, version)
                values (gen_random_uuid(), 'X', date '2026-10-20', '2', 'PENDENTE', null, 0)
                """))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_tarefa_prioridade");
    }

    @Test
    void statusForaDoEnumViolaConstraint() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into tarefa (id, titulo, data_vencimento, prioridade, status, concluida_em, version)
                values (gen_random_uuid(), 'X', date '2026-10-20', 'ALTA', 'ATRASADA', null, 0)
                """))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_tarefa_status");
    }
}
