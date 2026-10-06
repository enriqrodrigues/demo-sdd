package br.demo.usuarios.compartilhado.validacao;

/**
 * Regra de §4.2 do API-CONTRACT aplicada a um campo por {@link Valida} (AD-10). Cada campo tem uma
 * única regra, e o {@link ValidadorCampo} emite no máximo um código por campo.
 */
public enum RegraCampo {

    NOME_COMPLETO,
    EMAIL,
    CPF,
    /** O campo é {@code String} para que data malformada vire erro de campo, e não 400 de JSON. */
    DATA_NASCIMENTO,
    TELEFONE,
    CEP,
    UF,
    LOGRADOURO(150),
    BAIRRO(150),
    CIDADE(150),
    NUMERO(20),
    /** Único campo opcional: vazio é válido. */
    COMPLEMENTO(100),
    SENHA,
    /** Só obrigatório; a comparação com a senha é feita por {@link SenhasConferem}. */
    CONFIRMACAO_SENHA;

    private final int tamanhoMaximo;

    RegraCampo() {
        this(0);
    }

    RegraCampo(int tamanhoMaximo) {
        this.tamanhoMaximo = tamanhoMaximo;
    }

    /** Limite de {@code TAMANHO_EXCEDIDO}, ou 0 quando a regra não é de tamanho. */
    int tamanhoMaximo() {
        return tamanhoMaximo;
    }
}
