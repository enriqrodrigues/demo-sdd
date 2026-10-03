package br.com.demo.cadastro.perfil;

import br.com.demo.cadastro.shared.NegocioException;
import br.com.demo.cadastro.usuario.UsuarioService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/perfil")
class PerfilController {

    private final UsuarioService usuarios;

    PerfilController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping
    PerfilResposta obter(Authentication autenticacao) {
        return usuarios.buscarPorId(idDe(autenticacao))
                .map(PerfilResposta::de)
                .orElseThrow(() -> new NegocioException(
                        HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO", "Autenticação necessária"));
    }

    @PutMapping
    PerfilResposta atualizar(Authentication autenticacao, @Valid @RequestBody AtualizacaoPerfil requisicao) {
        return PerfilResposta.de(
                usuarios.atualizarContato(idDe(autenticacao), requisicao.telefone(), requisicao.endereco()));
    }

    private static UUID idDe(Authentication autenticacao) {
        return UUID.fromString(autenticacao.getName());
    }
}
