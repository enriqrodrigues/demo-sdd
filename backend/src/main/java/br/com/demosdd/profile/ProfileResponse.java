package br.com.demosdd.profile;

import java.time.LocalDate;

import br.com.demosdd.user.Address;
import br.com.demosdd.user.User;

/**
 * Dados do perfil (design D5). CPF, telefone e CEP vão só com dígitos: a
 * formatação é feita pelo frontend.
 */
record ProfileResponse(
        String name,
        String cpf,
        String email,
        LocalDate birthDate,
        String phone,
        String cep,
        String street,
        String number,
        String complement,
        String district,
        String city,
        String state) {

    static ProfileResponse of(User user) {
        Address address = user.getAddress();
        return new ProfileResponse(user.getName(), user.getCpf(), user.getEmail(), user.getBirthDate(),
                user.getPhone(), address.getCep(), address.getStreet(), address.getNumber(),
                address.getComplement(), address.getDistrict(), address.getCity(), address.getState());
    }
}
