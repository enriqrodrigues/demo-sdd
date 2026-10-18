package com.demosdd.todo.tarefa;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tarefa")
public class Tarefa {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(nullable = false)
    private LocalDate dataVencimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Prioridade prioridade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 9)
    private StatusTarefa status;

    /** Preenchida só por {@link #alterarStatus}; não há setter público. */
    private LocalDate dataConclusao;

    protected Tarefa() {
        // exigido pelo JPA
    }

    public Tarefa(String titulo, String descricao, LocalDate dataVencimento, Prioridade prioridade) {
        this.id = UUID.randomUUID();
        this.status = StatusTarefa.PENDENTE;
        alterarDados(titulo, descricao, dataVencimento, prioridade);
    }

    public void alterarDados(String titulo, String descricao, LocalDate dataVencimento, Prioridade prioridade) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.dataVencimento = dataVencimento;
        this.prioridade = prioridade;
    }

    /**
     * Aplica a transição de status: concluir define a conclusão como hoje, manter
     * {@code CONCLUIDA} preserva a data e reabrir apaga a data.
     */
    public void alterarStatus(StatusTarefa novoStatus, LocalDate hoje) {
        if (novoStatus == StatusTarefa.CONCLUIDA && status == StatusTarefa.PENDENTE) {
            dataConclusao = hoje;
        } else if (novoStatus == StatusTarefa.PENDENTE) {
            dataConclusao = null;
        }
        status = novoStatus;
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

    public LocalDate getDataConclusao() {
        return dataConclusao;
    }
}
