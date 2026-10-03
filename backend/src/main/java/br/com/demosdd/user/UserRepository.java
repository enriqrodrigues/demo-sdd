package br.com.demosdd.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** Usuários que conflitam com um novo cadastro pela RN03 (mesmo e-mail ou mesmo CPF). */
    List<User> findByEmailOrCpf(String email, String cpf);

    /** Usuário dono do e-mail, que deve chegar já normalizado (minúsculas, sem espaços). */
    Optional<User> findByEmail(String email);
}
