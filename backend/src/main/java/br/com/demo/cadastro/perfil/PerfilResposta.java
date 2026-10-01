package br.com.demo.cadastro.perfil;

import br.com.demo.cadastro.usuario.EnderecoDados;
import br.com.demo.cadastro.usuario.Usuario;
import java.time.LocalDate;

record PerfilResposta(String nome, String cpf, String email, LocalDate dataNascimento, String telefone,
                      EnderecoDados endereco, String status) {

    static PerfilResposta de(Usuario usuario) {
        return new PerfilResposta(usuario.getNome(), usuario.getCpf(), usuario.getEmail(),
                usuario.getDataNascimento(), usuario.getTelefone(), EnderecoDados.de(usuario.getEndereco()),
                usuario.getStatus().name());
    }
}
