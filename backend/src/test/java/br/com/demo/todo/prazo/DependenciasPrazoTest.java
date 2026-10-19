package br.com.demo.todo.prazo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/** Design D1: {@code prazo} é folha — regras puras que não dependem de outros módulos de domínio. */
class DependenciasPrazoTest {

    private static final Path FONTES_PRAZO = Path.of("src/main/java/br/com/demo/todo/prazo");

    @Test
    void prazoNaoImportaOutrosModulosDeDominio() throws IOException {
        List<String> violacoes;
        try (Stream<Path> arquivos = Files.walk(FONTES_PRAZO)) {
            violacoes = arquivos.filter(arquivo -> arquivo.toString().endsWith(".java"))
                    .flatMap(DependenciasPrazoTest::linhas)
                    .filter(linha -> linha.startsWith("import br.com.demo.todo.tarefa")
                            || linha.startsWith("import br.com.demo.todo.produtividade"))
                    .toList();
        }

        assertThat(violacoes).isEmpty();
    }

    private static Stream<String> linhas(Path arquivo) {
        try {
            return Files.readAllLines(arquivo).stream().map(String::strip);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
