package br.demo.usuarios.compartilhado.erro;

/**
 * Catálogo dos códigos de erro por campo e suas mensagens (API-CONTRACT §4.2, AD-10).
 * As regras ficam nos validadores; aqui está só a tabela de mensagens.
 */
public enum CodigoCampo {

    CAMPO_OBRIGATORIO("Campo obrigatório."),
    NOME_INVALIDO("Informe nome e sobrenome usando apenas letras."),
    EMAIL_INVALIDO("E-mail inválido."),
    CPF_INVALIDO("CPF inválido."),
    DATA_NASCIMENTO_INVALIDA("Data de nascimento inválida."),
    TELEFONE_INVALIDO("Telefone inválido. Informe DDD e número."),
    CEP_INVALIDO("CEP inválido."),
    UF_INVALIDA("UF inválida."),
    TAMANHO_EXCEDIDO("Texto muito longo."),
    SENHA_FRACA("A senha deve ter no mínimo 8 caracteres, com letra maiúscula, letra minúscula, "
            + "número e caractere especial."),
    SENHA_LONGA("A senha deve ter no máximo 64 caracteres."),
    SENHAS_DIFERENTES("As senhas não conferem.");

    private final String mensagem;

    CodigoCampo(String mensagem) {
        this.mensagem = mensagem;
    }

    public String mensagem() {
        return mensagem;
    }
}
