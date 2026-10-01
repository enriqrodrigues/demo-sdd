package br.com.demo.cadastro.perfil;

import br.com.demo.cadastro.shared.Mensagens;
import br.com.demo.cadastro.usuario.EnderecoDados;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

record AtualizacaoPerfil(
        @NotBlank(message = Mensagens.OBRIGATORIO)
        @Pattern(regexp = "\\d{10,11}", message = "Telefone inválido")
        String telefone,

        @NotNull(message = Mensagens.OBRIGATORIO)
        @Valid
        EnderecoDados endereco) {}
