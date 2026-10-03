package br.com.demosdd.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import br.com.demosdd.support.IntegrationTest;
import br.com.demosdd.support.TestUsers;

class UserRepositoryTest extends IntegrationTest {

    @Autowired
    private UserRepository repository;

    @Test
    void gravaELeUsuarioPendente() {
        User saved = repository.saveAndFlush(TestUsers.pending("maria@exemplo.com", "52998224725", clock.instant()));

        User found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getStatus()).isEqualTo(UserStatus.PENDENTE);
        assertThat(found.getEmail()).isEqualTo("maria@exemplo.com");
        assertThat(found.getAddress().getCity()).isEqualTo("São Paulo");
        assertThat(found.getAddress().getComplement()).isNull();
    }

    @Test
    void buscaPorEmailOuCpf() {
        repository.saveAndFlush(TestUsers.pending("maria@exemplo.com", "52998224725", clock.instant()));

        assertThat(repository.findByEmailOrCpf("maria@exemplo.com", "00000000000")).hasSize(1);
        assertThat(repository.findByEmailOrCpf("outro@exemplo.com", "52998224725")).hasSize(1);
        assertThat(repository.findByEmailOrCpf("outro@exemplo.com", "00000000000")).isEmpty();
    }

    @Test
    void bancoRecusaEmailDuplicado() {
        repository.saveAndFlush(TestUsers.pending("maria@exemplo.com", "52998224725", clock.instant()));

        assertThatThrownBy(() -> repository.saveAndFlush(
                TestUsers.pending("maria@exemplo.com", "11144477735", clock.instant())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void bancoRecusaCpfDuplicado() {
        repository.saveAndFlush(TestUsers.pending("maria@exemplo.com", "52998224725", clock.instant()));

        assertThatThrownBy(() -> repository.saveAndFlush(
                TestUsers.pending("joao@exemplo.com", "52998224725", clock.instant())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
