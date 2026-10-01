package br.com.demo.cadastro.usuario;

import br.com.demo.cadastro.shared.Mensagens;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record NovoUsuario(
        @NotBlank(message = Mensagens.OBRIGATORIO)
        @Size(max = 150, message = "Máximo de 150 caracteres")
        String nome,

        @NotBlank(message = Mensagens.OBRIGATORIO)
        @CpfValido
        String cpf,

        @NotBlank(message = Mensagens.OBRIGATORIO)
        @Email(message = "E-mail inválido")
        @Size(max = 254, message = "Máximo de 254 caracteres")
        String email,

        @NotNull(message = Mensagens.OBRIGATORIO)
        @Past(message = "Data de nascimento inválida")
        LocalDate dataNascimento,

        @NotBlank(message = Mensagens.OBRIGATORIO)
        @SenhaForte
        String senha,

        @NotBlank(message = Mensagens.OBRIGATORIO)
        @Pattern(regexp = "\\d{10,11}", message = "Telefone inválido")
        String telefone,

        @NotNull(message = Mensagens.OBRIGATORIO)
        @Valid
        EnderecoDados endereco) {}
