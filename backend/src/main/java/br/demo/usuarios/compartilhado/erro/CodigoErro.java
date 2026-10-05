package br.demo.usuarios.compartilhado.erro;

import org.springframework.http.HttpStatus;

/**
 * Catálogo fechado dos códigos de erro geral do envelope (API-CONTRACT §4.1, AD-11).
 * Código novo exige atualizar o contrato antes.
 */
public enum CodigoErro {

    VALIDACAO(HttpStatus.BAD_REQUEST, "Verifique os campos destacados."),
    REQUISICAO_INVALIDA(HttpStatus.BAD_REQUEST, "Requisição inválida."),
    DADO_IMUTAVEL(HttpStatus.BAD_REQUEST,
            "Nome, CPF, e-mail e data de nascimento não podem ser alterados."),
    CAMPO_DESCONHECIDO(HttpStatus.BAD_REQUEST, "A requisição contém campos não permitidos."),
    EMAIL_DUPLICADO(HttpStatus.CONFLICT, "E-mail já cadastrado"),
    CPF_DUPLICADO(HttpStatus.CONFLICT, "CPF já cadastrado"),
    CREDENCIAIS_INVALIDAS(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos"),
    CONTA_PENDENTE(HttpStatus.FORBIDDEN, "Sua conta ainda não foi ativada. Verifique seu e-mail."),
    NAO_AUTENTICADO(HttpStatus.UNAUTHORIZED, "Sua sessão expirou. Faça login novamente."),
    CSRF_INVALIDO(HttpStatus.FORBIDDEN, "Sua sessão de navegação expirou. Recarregue a página."),
    FALHA_ENVIO_EMAIL(HttpStatus.SERVICE_UNAVAILABLE,
            "Não foi possível enviar o e-mail de ativação. Tente novamente mais tarde."),
    NAO_ENCONTRADO(HttpStatus.NOT_FOUND, "Recurso não encontrado."),
    ERRO_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR,
            "Ocorreu um erro inesperado. Tente novamente mais tarde.");

    private final HttpStatus status;
    private final String mensagem;

    CodigoErro(HttpStatus status, String mensagem) {
        this.status = status;
        this.mensagem = mensagem;
    }

    public HttpStatus status() {
        return status;
    }

    public String mensagem() {
        return mensagem;
    }
}
