package br.demo.usuarios.compartilhado.tempo;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import br.demo.usuarios.compartilhado.TesteIntegracao;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

@DisplayName("AD-14: Clock único em UTC")
class RelogioTest extends TesteIntegracao {

    private static final DescribedPredicate<JavaMethodCall> LE_RELOGIO_DO_SISTEMA =
            DescribedPredicate.describe("lê o relógio do sistema sem o Clock injetado", chamada -> {
                var alvo = chamada.getTarget();
                String dono = alvo.getOwner().getName();
                String nome = alvo.getName();
                List<JavaClass> parametros = alvo.getRawParameterTypes();
                boolean nowComClock = parametros.size() == 1
                        && parametros.get(0).getName().equals(Clock.class.getName());
                return (dono.startsWith("java.time.") && nome.equals("now") && !nowComClock)
                        || (dono.equals(System.class.getName()) && nome.equals("currentTimeMillis"))
                        || (dono.equals(Clock.class.getName()) && nome.startsWith("system"));
            });

    private static final ArchRule SO_CONFIGURACAO_TEMPO_LE_O_RELOGIO = noClasses()
            .that().doNotHaveFullyQualifiedName(ConfiguracaoTempo.class.getName())
            .should().callMethodWhere(LE_RELOGIO_DO_SISTEMA)
            .because("o tempo vem sempre do bean Clock (AD-14)");

    @Autowired
    ApplicationContext contexto;

    @Test
    @DisplayName("AD-14: os beans Clock são exatamente clock (UTC, de produção) e relogioAjustavel (testes)")
    void beansClock() {
        assertThat(contexto.getBeanNamesForType(Clock.class))
                .containsExactlyInAnyOrder("clock", "relogioAjustavel");
        assertThat(contexto.getBean("clock", Clock.class).getZone()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    @DisplayName("AD-14: nenhuma classe de produção lê o relógio do sistema, exceto ConfiguracaoTempo")
    void producaoNaoLeRelogioDoSistema() {
        var producao = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("br.demo.usuarios");

        SO_CONFIGURACAO_TEMPO_LE_O_RELOGIO.check(producao);
    }

    @Test
    @DisplayName("AD-14: a regra de relógio detecta Instant.now() sem Clock")
    void regraDetectaViolacao() {
        var violador = new ClassFileImporter().importClasses(Violador.class);

        assertThatThrownBy(() -> SO_CONFIGURACAO_TEMPO_LE_O_RELOGIO.check(violador))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Instant.now()");
    }

    static class Violador {
        Instant agora() {
            return Instant.now();
        }
    }
}
