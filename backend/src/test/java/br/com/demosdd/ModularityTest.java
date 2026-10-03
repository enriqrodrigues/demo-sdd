package br.com.demosdd;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * Garante as fronteiras entre os módulos de domínio (registration, activation,
 * user, auth, shared): sem ciclos e sem acesso a pacotes internos de outro módulo.
 */
class ModularityTest {

    private final ApplicationModules modules = ApplicationModules.of(DemoSddApplication.class);

    @Test
    void verificaFronteirasEntreModulos() {
        modules.verify();
    }

    /** A configuração de segurança conhece só padrões de URL, nunca os módulos de domínio (design D8). */
    @Test
    void sharedNaoDependeDosModulosDeDominio() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("br.com.demosdd");

        noClasses().that().resideInAPackage("br.com.demosdd.shared..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "br.com.demosdd.auth..", "br.com.demosdd.user..",
                        "br.com.demosdd.registration..", "br.com.demosdd.activation..")
                .check(classes);
    }
}
