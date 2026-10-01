package br.com.demo.cadastro.autenticacao;

import br.com.demo.cadastro.shared.Mensagens;
import br.com.demo.cadastro.usuario.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AutenticacaoController {

    record LoginRequisicao(
            @NotBlank(message = Mensagens.OBRIGATORIO) String email,
            @NotBlank(message = Mensagens.OBRIGATORIO) String senha) {}

    record UsuarioLogado(String nome, String email) {}

    private final AutenticacaoService servico;
    private final SecurityContextRepository repositorioContexto = new HttpSessionSecurityContextRepository();

    AutenticacaoController(AutenticacaoService servico) {
        this.servico = servico;
    }

    @PostMapping("/login")
    UsuarioLogado login(@Valid @RequestBody LoginRequisicao requisicao,
                        HttpServletRequest request, HttpServletResponse response) {
        Usuario usuario = servico.autenticar(requisicao.email(), requisicao.senha());

        HttpSession anterior = request.getSession(false);
        if (anterior != null) {
            anterior.invalidate();
        }
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(usuario.getId().toString(), null, List.of()));
        SecurityContextHolder.setContext(contexto);
        repositorioContexto.saveContext(contexto, request, response);

        return new UsuarioLogado(usuario.getNome(), usuario.getEmail());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        if (sessao != null) {
            sessao.invalidate();
        }
        SecurityContextHolder.clearContext();
    }
}
