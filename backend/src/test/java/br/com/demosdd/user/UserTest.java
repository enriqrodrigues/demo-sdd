package br.com.demosdd.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/** Regras do domínio de {@link User} que não dependem do banco. */
class UserTest {

    private static User maria() {
        return User.pending("Maria da Silva", "52998224725", "maria@exemplo.com", LocalDate.of(1990, 5, 20),
                "hash", "11987654321",
                new Address("01310100", "Avenida Paulista", "1000", "Apto 12", "Bela Vista", "São Paulo", "SP"),
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void updateContactTrocaTelefoneEEnderecoEPreservaOsDadosCriticos() {
        User user = maria();

        user.updateContact("2134567890",
                new Address("20040020", "Rua da Assembleia", "10", null, "Centro", "Rio de Janeiro", "RJ"));

        assertThat(user.getPhone()).isEqualTo("2134567890");
        assertThat(user.getAddress().getCep()).isEqualTo("20040020");
        assertThat(user.getAddress().getStreet()).isEqualTo("Rua da Assembleia");
        assertThat(user.getAddress().getNumber()).isEqualTo("10");
        assertThat(user.getAddress().getComplement()).isNull();
        assertThat(user.getAddress().getDistrict()).isEqualTo("Centro");
        assertThat(user.getAddress().getCity()).isEqualTo("Rio de Janeiro");
        assertThat(user.getAddress().getState()).isEqualTo("RJ");

        // RN01: dados críticos permanecem como no cadastro.
        assertThat(user.getName()).isEqualTo("Maria da Silva");
        assertThat(user.getCpf()).isEqualTo("52998224725");
        assertThat(user.getEmail()).isEqualTo("maria@exemplo.com");
        assertThat(user.getBirthDate()).isEqualTo(LocalDate.of(1990, 5, 20));
    }
}
