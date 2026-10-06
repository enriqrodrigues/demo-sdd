package br.demo.usuarios.compartilhado.validacao;

/**
 * Requisição que traz a senha e a confirmação, validada por {@link SenhasConferem}. Um record com os
 * componentes {@code senha} e {@code confirmacaoSenha} só precisa declarar
 * {@code implements ComConfirmacaoSenha}: os acessores gerados já cumprem o contrato.
 */
public interface ComConfirmacaoSenha {

    String senha();

    String confirmacaoSenha();
}
