package br.com.demosdd.shared.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.PastOrPresent;

/** Testa as anotações de campo pelo Bean Validation, como são usadas no DTO. */
class FieldConstraintsTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final Instant NOW = Instant.parse("2026-10-03T15:00:00Z");

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.byDefaultProvider().configure()
                .clockProvider(() -> Clock.fixed(NOW, ZONE))
                .buildValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    record Sample(@Phone String phone, @Cep String cep, @Uf String uf,
                  @PastOrPresent LocalDate birthDate, @StrongPassword String password) {

        static Sample phone(String phone) {
            return new Sample(phone, null, null, null, null);
        }

        static Sample cep(String cep) {
            return new Sample(null, cep, null, null, null);
        }

        static Sample uf(String uf) {
            return new Sample(null, null, uf, null, null);
        }

        static Sample birthDate(LocalDate birthDate) {
            return new Sample(null, null, null, birthDate, null);
        }

        static Sample password(String password) {
            return new Sample(null, null, null, null, password);
        }
    }

    private List<String> messages(Sample sample) {
        Set<ConstraintViolation<Sample>> violations = validator.validate(sample);
        return violations.stream().map(ConstraintViolation::getMessage).toList();
    }

    @ParameterizedTest
    @ValueSource(strings = {"11987654321", "1134567890"})
    void aceitaCelularEFixo(String phone) {
        assertThat(messages(Sample.phone(phone))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"113456789", "119876543210", "1198765432a"})
    void recusaTelefoneComQuantidadeInvalidaDeDigitos(String phone) {
        assertThat(messages(Sample.phone(phone))).containsExactly(
                "O telefone deve ter 10 dígitos (fixo) ou 11 dígitos (celular), incluindo o DDD");
    }

    @Test
    void aceitaCepCom8Digitos() {
        assertThat(messages(Sample.cep("01310100"))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0131010", "013101000", "0131010a"})
    void recusaCepSem8Digitos(String cep) {
        assertThat(messages(Sample.cep(cep))).containsExactly("O CEP deve ter 8 dígitos");
    }

    @ParameterizedTest
    @ValueSource(strings = {"SP", "RJ", "DF", "TO"})
    void aceitaUfValida(String uf) {
        assertThat(messages(Sample.uf(uf))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"XX", "S", "SPP"})
    void recusaUfInexistente(String uf) {
        assertThat(messages(Sample.uf(uf))).containsExactly("UF inválida");
    }

    @Test
    void aceitaDataDeNascimentoDeHoje() {
        assertThat(messages(Sample.birthDate(LocalDate.ofInstant(NOW, ZONE)))).isEmpty();
    }

    @Test
    void recusaDataDeNascimentoFutura() {
        assertThat(messages(Sample.birthDate(LocalDate.ofInstant(NOW, ZONE).plusDays(1)))).hasSize(1);
    }

    @Test
    void senhaFracaGeraUmaMensagemPorCriterio() {
        assertThat(messages(Sample.password("abcdefgh"))).containsExactlyInAnyOrder(
                "A senha deve conter ao menos uma letra maiúscula",
                "A senha deve conter ao menos um dígito",
                "A senha deve conter ao menos um caractere especial");
    }
}
