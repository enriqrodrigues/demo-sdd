package br.com.demosdd;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Garante as fronteiras entre os módulos de domínio (registration, activation,
 * user, shared): sem ciclos e sem acesso a pacotes internos de outro módulo.
 */
class ModularityTest {

    private final ApplicationModules modules = ApplicationModules.of(DemoSddApplication.class);

    @Test
    void verificaFronteirasEntreModulos() {
        modules.verify();
    }
}
