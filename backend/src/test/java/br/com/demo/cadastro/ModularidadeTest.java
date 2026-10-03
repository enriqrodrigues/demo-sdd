package br.com.demo.cadastro;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularidadeTest {

    @Test
    void respeitaAsFronteirasDosModulos() {
        ApplicationModules.of(CadastroApplication.class).verify();
    }
}
