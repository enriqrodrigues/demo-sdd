package br.demo.usuarios.compartilhado.validacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.demo.usuarios.compartilhado.erro.CodigoCampo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NFR-5: mensagens de validação iguais no backend e no frontend")
class MensagensFrontendTest {

    private static final Path MENSAGENS_TS = Path.of("../frontend/src/validacao/mensagens.ts");

    /** Uma entrada por linha: {@code CODIGO: 'texto',}. */
    private static final Pattern ENTRADA = Pattern.compile("^\\s*([A-Z_]+): '([^']*)',\\s*$");

    @Test
    @DisplayName("FR-2, NFR-5: mensagens.ts tem os mesmos pares código→mensagem de CodigoCampo")
    void mesmasMensagens() throws IOException {
        Map<String, String> frontend = new LinkedHashMap<>();
        for (String linha : Files.readAllLines(MENSAGENS_TS, StandardCharsets.UTF_8)) {
            Matcher m = ENTRADA.matcher(linha);
            if (m.matches()) {
                assertThat(frontend.put(m.group(1), m.group(2))).as("código repetido: %s", m.group(1)).isNull();
            }
        }

        Map<String, String> backend = new LinkedHashMap<>();
        for (CodigoCampo codigo : CodigoCampo.values()) {
            backend.put(codigo.name(), codigo.mensagem());
        }

        assertThat(frontend).containsExactlyInAnyOrderEntriesOf(backend);
    }
}
