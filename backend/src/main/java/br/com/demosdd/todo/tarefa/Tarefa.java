package br.com.demosdd.todo.tarefa;

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

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Prioridade prioridade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusTarefa status;

    @Column(name = "data_conclusao")
    private LocalDate dataConclusao;

    protected Tarefa() {
        // exigido pelo JPA
    }

    public Tarefa(String titulo, String descricao, LocalDate dataVencimento, Prioridade prioridade) {
        this.id = UUID.randomUUID();
        this.status = StatusTarefa.PENDENTE;
        atualizarDados(titulo, descricao, dataVencimento, prioridade);
    }

    public void atualizarDados(String titulo, String descricao, LocalDate dataVencimento, Prioridade prioridade) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.dataVencimento = dataVencimento;
        this.prioridade = prioridade;
    }

    /** Marca como concluída hoje; concluir de novo preserva a data original. */
    public void concluir(LocalDate hoje) {
        if (status == StatusTarefa.CONCLUIDA) {
            return;
        }
        this.status = StatusTarefa.CONCLUIDA;
        this.dataConclusao = hoje;
    }

    public void reabrir() {
        this.status = StatusTarefa.PENDENTE;
        this.dataConclusao = null;
    }

    public void alterarStatus(StatusTarefa novoStatus, LocalDate hoje) {
        if (novoStatus == StatusTarefa.CONCLUIDA) {
            concluir(hoje);
        } else {
            reabrir();
        }
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
