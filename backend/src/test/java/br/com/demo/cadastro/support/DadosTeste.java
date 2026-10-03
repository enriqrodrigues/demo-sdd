package br.com.demo.cadastro.support;

import br.com.demo.cadastro.usuario.EnderecoDados;
import br.com.demo.cadastro.usuario.NovoUsuario;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class DadosTeste {

    public static final String SENHA = "Senha@123";

    private DadosTeste() {}

    public static String emailUnico() {
        return "u" + UUID.randomUUID().toString().substring(0, 12) + "@teste.local";
    }

    public static String cpfValido() {
        int[] d = new int[11];
        do {
            for (int i = 0; i < 9; i++) {
                d[i] = ThreadLocalRandom.current().nextInt(10);
            }
        } while (todosIguais(d));
        d[9] = digito(d, 9);
        d[10] = digito(d, 10);
        StringBuilder cpf = new StringBuilder();
        for (int digito : d) {
            cpf.append(digito);
        }
        return cpf.toString();
    }

    public static NovoUsuario novoUsuario(String email, String cpf) {
        return new NovoUsuario("Maria da Silva", cpf, email, LocalDate.of(1990, 5, 20), SENHA, "11987654321",
                new EnderecoDados("01310100", "Avenida Paulista", "1000", "Apto 12", "Bela Vista", "São Paulo", "SP"));
    }

    public static String cadastroJson(String email, String cpf) {
        return cadastroJson(email, cpf, "Maria da Silva");
    }

    public static String cadastroJson(String email, String cpf, String nome) {
        return """
                {"nome":"%s","cpf":"%s","email":"%s","dataNascimento":"1990-05-20","senha":"%s",
                 "telefone":"11987654321",
                 "endereco":{"cep":"01310100","logradouro":"Avenida Paulista","numero":"1000",
                             "complemento":"Apto 12","bairro":"Bela Vista","cidade":"São Paulo","uf":"SP"}}
                """.formatted(nome, cpf, email, SENHA);
    }

    private static boolean todosIguais(int[] d) {
        for (int i = 1; i < 9; i++) {
            if (d[i] != d[0]) {
                return false;
            }
        }
        return true;
    }

    private static int digito(int[] d, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += d[i] * (quantidade + 1 - i);
        }
        int resto = soma * 10 % 11;
        return resto == 10 ? 0 : resto;
    }
}
