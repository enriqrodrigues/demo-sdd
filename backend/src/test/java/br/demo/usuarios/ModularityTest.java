package br.demo.usuarios;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

    @Test
    @DisplayName("AD-1: os módulos respeitam o grafo de dependências permitido")
    void verificaModulos() {
        ApplicationModules.of(UsuariosApplication.class).verify();
    }
}
