package br.com.demosdd.user;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Usuário do sistema. Os dados chegam aqui já normalizados (e-mail em
 * minúsculas; CPF e telefone somente com dígitos) e com a senha em hash.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 11, unique = true)
    private String cpf;

    @Column(nullable = false, length = 254, unique = true)
    private String email;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 11)
    private String phone;

    @Embedded
    private Address address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    protected User() {
        // JPA
    }

    private User(String name, String cpf, String email, LocalDate birthDate, String passwordHash,
                 String phone, Address address, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.cpf = cpf;
        this.email = email;
        this.birthDate = birthDate;
        this.passwordHash = passwordHash;
        this.phone = phone;
        this.address = address;
        this.status = UserStatus.PENDENTE;
        this.createdAt = createdAt;
    }

    /** Novo cadastro: sempre começa como {@link UserStatus#PENDENTE} (RF03). */
    public static User pending(String name, String cpf, String email, LocalDate birthDate, String passwordHash,
                               String phone, Address address, Instant now) {
        return new User(name, cpf, email, birthDate, passwordHash, phone, address, now);
    }

    /** Ativa a conta (RF05). */
    public void activate(Instant now) {
        if (status != UserStatus.PENDENTE) {
            throw new IllegalStateException("Somente contas pendentes podem ser ativadas");
        }
        this.status = UserStatus.ATIVO;
        this.activatedAt = now;
    }

    /**
     * Atualiza o contato pelo perfil (RF07): troca o telefone e o endereço inteiro.
     * Nome, CPF, e-mail e data de nascimento não têm forma de alteração (RN01).
     */
    public void updateContact(String phone, Address address) {
        this.phone = phone;
        this.address = address;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCpf() {
        return cpf;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPhone() {
        return phone;
    }

    public Address getAddress() {
        return address;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }
}
