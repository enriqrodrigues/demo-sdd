package br.com.demosdd.support;

import java.time.Instant;
import java.time.LocalDate;

import br.com.demosdd.user.Address;
import br.com.demosdd.user.User;

/** Fábrica de usuários para testes que gravam direto no repositório. */
public final class TestUsers {

    private TestUsers() {
    }

    public static User pending(String email, String cpf, Instant now) {
        return User.pending("Maria da Silva", cpf, email, LocalDate.of(1990, 5, 20),
                "$2a$10$hashdetesteqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq", "11987654321",
                new Address("01310100", "Avenida Paulista", "1000", null, "Bela Vista", "São Paulo", "SP"),
                now);
    }
}
