package br.com.demo.todo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/** Design D1: nenhum outro módulo depende de {@code produtividade}. */
class DependenciasModulosTest {

    private static final Path FONTES = Path.of("src/main/java/br/com/demo/todo");

    @Test
    void nadaForaDeProdutividadeImportaProdutividade() throws IOException {
        List<Path> violacoes;
        try (Stream<Path> arquivos = Files.walk(FONTES)) {
            violacoes = arquivos.filter(arquivo -> arquivo.toString().endsWith(".java"))
                    .filter(arquivo -> !arquivo.startsWith(FONTES.resolve("produtividade")))
                    .filter(DependenciasModulosTest::importaProdutividade)
                    .toList();
        }

        assertThat(violacoes).isEmpty();
    }

    private static boolean importaProdutividade(Path arquivo) {
        try {
            return Files.readAllLines(arquivo).stream()
                    .anyMatch(linha -> linha.strip().startsWith("import br.com.demo.todo.produtividade"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
