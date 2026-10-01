package br.com.demo.cadastro.usuario;

import br.com.demo.cadastro.shared.Mensagens;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EnderecoDados(
        @NotBlank(message = Mensagens.OBRIGATORIO)
        @Pattern(regexp = "\\d{8}", message = "CEP inválido")
        String cep,

        @NotBlank(message = Mensagens.OBRIGATORIO)
        @Size(max = 200, message = "Máximo de 200 caracteres")
        String logradouro,

        @NotBlank(message = Mensagens.OBRIGATORIO)
        @Size(max = 10, message = "Máximo de 10 caracteres")
        String numero,

        @Size(max = 100, message = "Máximo de 100 caracteres")
        String complemento,

        @NotBlank(message = Mensagens.OBRIGATORIO)
        @Size(max = 100, message = "Máximo de 100 caracteres")
        String bairro,

        @NotBlank(message = Mensagens.OBRIGATORIO)
        @Size(max = 100, message = "Máximo de 100 caracteres")
        String cidade,

        @NotBlank(message = Mensagens.OBRIGATORIO)
        @Pattern(regexp = Uf.REGEX, message = "UF inválida")
        String uf) {}
