package br.com.demosdd.registration;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import br.com.demosdd.shared.validation.Cep;
import br.com.demosdd.shared.validation.Cpf;
import br.com.demosdd.shared.validation.InputNormalizer;
import br.com.demosdd.shared.validation.Phone;
import br.com.demosdd.shared.validation.StrongPassword;
import br.com.demosdd.shared.validation.Uf;

/**
 * Dados do cadastro (RF01). O construtor normaliza a entrada antes da
 * validação (RF02): remove espaços nas pontas, trata texto só com espaços como
 * vazio, coloca o e-mail em minúsculas e tira as máscaras de CPF, telefone e CEP.
 * A senha não é alterada (apenas uma senha só com espaços é tratada como vazia).
 */
public record RegistrationRequest(
        @NotBlank(message = REQUIRED)
        @Size(max = 150, message = "Máximo de 150 caracteres")
        String name,

        @NotBlank(message = REQUIRED)
        @Cpf
        String cpf,

        @NotBlank(message = REQUIRED)
        @Size(max = 254, message = "Máximo de 254 caracteres")
        @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "E-mail inválido")
        String email,

        @NotNull(message = REQUIRED)
        @PastOrPresent(message = "A data de nascimento não pode ser futura")
        LocalDate birthDate,

        @NotBlank(message = REQUIRED)
        @StrongPassword
        String password,

        @NotBlank(message = REQUIRED)
        @Phone
        String phone,

        @NotBlank(message = REQUIRED)
        @Cep
        String cep,

        @NotBlank(message = REQUIRED)
        @Size(max = 200, message = "Máximo de 200 caracteres")
        String street,

        @NotBlank(message = REQUIRED)
        @Size(max = 20, message = "Máximo de 20 caracteres")
        String number,

        @Size(max = 100, message = "Máximo de 100 caracteres")
        String complement,

        @NotBlank(message = REQUIRED)
        @Size(max = 100, message = "Máximo de 100 caracteres")
        String district,

        @NotBlank(message = REQUIRED)
        @Size(max = 100, message = "Máximo de 100 caracteres")
        String city,

        @NotBlank(message = REQUIRED)
        @Uf
        String state) {

    static final String REQUIRED = "Campo obrigatório";

    public RegistrationRequest {
        name = InputNormalizer.trimToNull(name);
        cpf = InputNormalizer.cpf(cpf);
        email = InputNormalizer.email(email);
        password = password == null || password.isBlank() ? null : password;
        phone = InputNormalizer.phone(phone);
        cep = InputNormalizer.cep(cep);
        street = InputNormalizer.trimToNull(street);
        number = InputNormalizer.trimToNull(number);
        complement = InputNormalizer.trimToNull(complement);
        district = InputNormalizer.trimToNull(district);
        city = InputNormalizer.trimToNull(city);
        state = InputNormalizer.uf(state);
    }
}
