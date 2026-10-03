package br.com.demo.cadastro.autenticacao;

import br.com.demo.cadastro.shared.NegocioException;
import br.com.demo.cadastro.usuario.Usuario;
import br.com.demo.cadastro.usuario.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
class AutenticacaoService {

    private final UsuarioService usuarios;

    AutenticacaoService(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    Usuario autenticar(String email, String senha) {
        Usuario usuario = usuarios.buscarPorEmail(email)
                .filter(u -> usuarios.senhaConfere(u, senha))
                .orElseThrow(() -> new NegocioException(
                        HttpStatus.UNAUTHORIZED, "CREDENCIAIS_INVALIDAS", "E-mail ou senha inválidos"));
        if (!usuario.isAtivo()) {
            throw new NegocioException(HttpStatus.FORBIDDEN, "CONTA_PENDENTE",
                    "Sua conta ainda não foi ativada. Verifique seu e-mail.");
        }
        return usuario;
    }
}
