package com.demosdd.todo.tarefa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.demosdd.todo.TestcontainersConfiguration;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class TarefaRepositoryTest {

    @Autowired
    TarefaRepository repository;

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void flywayAplicaAMigracaoV1() {
        String versao = jdbc.queryForObject(
                "select version from flyway_schema_history where success order by installed_rank desc limit 1",
                String.class);

        assertThat(versao).isEqualTo("1");
    }

    @Test
    void salvarELerPreservaTodosOsCampos() {
        Tarefa tarefa = new Tarefa("Pagar conta", "Luz", LocalDate.of(2026, 10, 20), Prioridade.ALTA);
        tarefa.alterarStatus(StatusTarefa.CONCLUIDA, LocalDate.of(2026, 10, 18));
        repository.saveAndFlush(tarefa);
        entityManager.clear();

        Tarefa lida = repository.findById(tarefa.getId()).orElseThrow();

        assertThat(lida).isNotSameAs(tarefa);
        assertThat(lida.getId()).isEqualTo(tarefa.getId());
        assertThat(lida.getTitulo()).isEqualTo("Pagar conta");
        assertThat(lida.getDescricao()).isEqualTo("Luz");
        assertThat(lida.getDataVencimento()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(lida.getPrioridade()).isEqualTo(Prioridade.ALTA);
        assertThat(lida.getStatus()).isEqualTo(StatusTarefa.CONCLUIDA);
        assertThat(lida.getDataConclusao()).isEqualTo(LocalDate.of(2026, 10, 18));
    }

    @Test
    void bancoRejeitaStatusInconsistenteComDataDeConclusao() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into tarefa (id, titulo, data_vencimento, prioridade, status, data_conclusao)
                values (gen_random_uuid(), 'x', date '2026-10-20', 'ALTA', 'PENDENTE', date '2026-10-18')
                """))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void bancoRejeitaPrioridadeForaDoEnum() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into tarefa (id, titulo, data_vencimento, prioridade, status)
                values (gen_random_uuid(), 'x', date '2026-10-20', 'URGEN', 'PENDENTE')
                """))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
