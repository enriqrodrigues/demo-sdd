package br.com.demo.todo.tarefa;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "tarefa")
public class Tarefa {

    @Id
    private UUID id;

    private String titulo;

    private String descricao;

    private LocalDate dataVencimento;

    @Enumerated(EnumType.STRING)
    private Prioridade prioridade;

    @Enumerated(EnumType.STRING)
    private StatusTarefa status;

    private Instant concluidaEm;

    @Version
    private Long version;

    protected Tarefa() {
    }

    /** Toda tarefa nasce pendente, sem instante de conclusão. */
    public static Tarefa nova(String titulo, String descricao, LocalDate dataVencimento, Prioridade prioridade) {
        Tarefa tarefa = new Tarefa();
        tarefa.id = UUID.randomUUID();
        tarefa.titulo = titulo;
        tarefa.descricao = descricao;
        tarefa.dataVencimento = dataVencimento;
        tarefa.prioridade = prioridade;
        tarefa.status = StatusTarefa.PENDENTE;
        return tarefa;
    }

    public UUID getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public StatusTarefa getStatus() {
        return status;
    }

    public Instant getConcluidaEm() {
        return concluidaEm;
    }

    public Long getVersion() {
        return version;
    }
}
