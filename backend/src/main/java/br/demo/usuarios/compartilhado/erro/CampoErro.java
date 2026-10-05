package br.demo.usuarios.compartilhado.erro;

/**
 * Erro de um campo no envelope. {@code campo} é o caminho JSON com ponto (ex.: {@code endereco.cep}).
 */
public record CampoErro(String campo, String codigo, String mensagem) {

    public CampoErro(String campo, CodigoCampo codigo) {
        this(campo, codigo.name(), codigo.mensagem());
    }
}
