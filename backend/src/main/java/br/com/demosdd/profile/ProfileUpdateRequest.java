package br.com.demosdd.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;

import br.com.demosdd.shared.validation.Cep;
import br.com.demosdd.shared.validation.InputNormalizer;
import br.com.demosdd.shared.validation.Phone;
import br.com.demosdd.shared.validation.Uf;

/**
 * Alteração do perfil (RF07). Telefone e endereço seguem as mesmas regras e a
 * mesma normalização do cadastro (design D4). Nome, CPF, e-mail e data de
 * nascimento são declarados só para recusar qualquer valor enviado (RN01,
 * design D3); como {@code Object}, qualquer tipo de valor cai na mesma regra.
 */
record ProfileUpdateRequest(
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
        String state,

        @Null(message = IMMUTABLE)
        Object name,

        @Null(message = IMMUTABLE)
        Object cpf,

        @Null(message = IMMUTABLE)
        Object email,

        @Null(message = IMMUTABLE)
        Object birthDate) {

    static final String REQUIRED = "Campo obrigatório";
    static final String IMMUTABLE = "Este campo não pode ser alterado";

    ProfileUpdateRequest {
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
