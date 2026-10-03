package br.com.demosdd.registration;

/** Corpos JSON de cadastro usados nos testes da API. */
public final class TestPayloads {

    private TestPayloads() {
    }

    /** Cadastro válido, com máscaras e sem complemento. */
    public static String validRegistration() {
        return registration("maria@exemplo.com", "529.982.247-25");
    }

    public static String registration(String email, String cpf) {
        return """
                {
                  "name": "Maria da Silva",
                  "cpf": "%s",
                  "email": "%s",
                  "birthDate": "1990-05-20",
                  "password": "Segura@123",
                  "phone": "(11) 98765-4321",
                  "cep": "01310-100",
                  "street": "Avenida Paulista",
                  "number": "1000",
                  "complement": "",
                  "district": "Bela Vista",
                  "city": "São Paulo",
                  "state": "SP"
                }
                """.formatted(cpf, email);
    }
}
