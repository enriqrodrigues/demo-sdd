package br.demo.usuarios.compartilhado.validacao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FR-2: normalização antes da validação (AD-5)")
class NormalizacaoTest {

    @Test
    @DisplayName("FR-2: texto remove espaço em branco das bordas, inclusive NBSP e tabulação")
    void texto() {
        assertThat(Normalizacao.texto("  Ana Souza \t")).isEqualTo("Ana Souza");
        assertThat(Normalizacao.texto(" Ana　")).isEqualTo("Ana");
        assertThat(Normalizacao.texto("Ana  Souza")).isEqualTo("Ana  Souza");
        assertThat(Normalizacao.texto("   ")).isEmpty();
        assertThat(Normalizacao.texto(null)).isNull();
    }

    @Test
    @DisplayName("FR-2: textoOpcional transforma vazio em null")
    void textoOpcional() {
        assertThat(Normalizacao.textoOpcional(" Apto 12 ")).isEqualTo("Apto 12");
        assertThat(Normalizacao.textoOpcional("")).isNull();
        assertThat(Normalizacao.textoOpcional("   ")).isNull();
        assertThat(Normalizacao.textoOpcional(null)).isNull();
    }

    @Test
    @DisplayName("FR-2: email faz trim e minúsculas independentes de locale")
    void email() {
        assertThat(Normalizacao.email(" Ana@X.COM ")).isEqualTo("ana@x.com");
        assertThat(Normalizacao.email("TITULO@EXEMPLO.COM")).isEqualTo("titulo@exemplo.com");
        assertThat(Normalizacao.email(null)).isNull();
    }

    @Test
    @DisplayName("FR-2: soDigitos remove só . - ( ) / e espaço em branco")
    void soDigitos() {
        assertThat(Normalizacao.soDigitos("529.982.247-25")).isEqualTo("52998224725");
        assertThat(Normalizacao.soDigitos("(11) 98765-4321")).isEqualTo("11987654321");
        assertThat(Normalizacao.soDigitos("01/310 100")).isEqualTo("01310100");
        assertThat(Normalizacao.soDigitos("52998224725a")).isEqualTo("52998224725a");
        assertThat(Normalizacao.soDigitos("529_982+247")).isEqualTo("529_982+247");
        assertThat(Normalizacao.soDigitos(" . ")).isEmpty();
        assertThat(Normalizacao.soDigitos(null)).isNull();
    }
}
