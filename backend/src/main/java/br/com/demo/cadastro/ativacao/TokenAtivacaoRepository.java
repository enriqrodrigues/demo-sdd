package br.com.demo.cadastro.ativacao;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface TokenAtivacaoRepository extends JpaRepository<TokenAtivacao, UUID> {

    Optional<TokenAtivacao> findByTokenHash(String tokenHash);
}
