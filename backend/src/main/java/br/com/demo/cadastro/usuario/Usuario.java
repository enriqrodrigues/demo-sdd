package br.com.demo.cadastro.usuario;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nome;
    private String cpf;
    private String email;
    private LocalDate dataNascimento;
    private String senhaHash;
    private String telefone;

    @Embedded
    private Endereco endereco;

    @Enumerated(EnumType.STRING)
    private StatusUsuario status;

    private Instant criadoEm;
    private Instant atualizadoEm;

    protected Usuario() {}

    Usuario(String nome, String cpf, String email, LocalDate dataNascimento, String senhaHash,
            String telefone, Endereco endereco, Instant agora) {
        this.nome = nome;
        this.cpf = cpf;
        this.email = email;
        this.dataNascimento = dataNascimento;
        this.senhaHash = senhaHash;
        this.telefone = telefone;
        this.endereco = endereco;
        this.status = StatusUsuario.PENDENTE_ATIVACAO;
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    void ativar(Instant agora) {
        if (status != StatusUsuario.ATIVO) {
            status = StatusUsuario.ATIVO;
            atualizadoEm = agora;
        }
    }

    void atualizarContato(String telefone, Endereco endereco, Instant agora) {
        this.telefone = telefone;
        this.endereco = endereco;
        this.atualizadoEm = agora;
    }

    String getSenhaHash() { return senhaHash; }

    public boolean isAtivo() { return status == StatusUsuario.ATIVO; }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getCpf() { return cpf; }
    public String getEmail() { return email; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public String getTelefone() { return telefone; }
    public Endereco getEndereco() { return endereco; }
    public StatusUsuario getStatus() { return status; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
}
