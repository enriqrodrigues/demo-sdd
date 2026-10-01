package br.com.demo.cadastro.ativacao;

import br.com.demo.cadastro.cadastro.UsuarioCadastrado;
import br.com.demo.cadastro.shared.NegocioException;
import br.com.demo.cadastro.usuario.UsuarioService;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AtivacaoService {

    private final TokenAtivacaoRepository tokens;
    private final GeradorToken gerador;
    private final EmailAtivacao email;
    private final UsuarioService usuarios;
    private final Clock clock;
    private final String baseUrl;
    private final Duration validade;

    AtivacaoService(TokenAtivacaoRepository tokens, GeradorToken gerador, EmailAtivacao email,
                    UsuarioService usuarios, Clock clock,
                    @Value("${app.base-url}") String baseUrl,
                    @Value("${app.ativacao.expiracao}") Duration validade) {
        this.tokens = tokens;
        this.gerador = gerador;
        this.email = email;
        this.usuarios = usuarios;
        this.clock = clock;
        this.baseUrl = baseUrl;
        this.validade = validade;
    }

    @ApplicationModuleListener
    public void aoCadastrarUsuario(UsuarioCadastrado evento) {
        String token = gerador.gerar();
        tokens.save(new TokenAtivacao(evento.usuarioId(), GeradorToken.hash(token), clock.instant(), validade));
        email.enviar(evento.email(), evento.nome(), baseUrl + "/ativar?token=" + token);
    }

    @Transactional
    public void ativar(String token) {
        if (token == null || token.isBlank()) {
            throw tokenInvalido();
        }
        TokenAtivacao registro = tokens.findByTokenHash(GeradorToken.hash(token)).orElseThrow(this::tokenInvalido);
        registro.usar(clock.instant());
        usuarios.ativar(registro.getUsuarioId());
    }

    private NegocioException tokenInvalido() {
        return new NegocioException(HttpStatus.BAD_REQUEST, "TOKEN_INVALIDO", "Link de ativação inválido");
    }
}
