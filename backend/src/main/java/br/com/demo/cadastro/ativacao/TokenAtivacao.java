package br.com.demo.cadastro.ativacao;

import br.com.demo.cadastro.shared.NegocioException;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "token_ativacao")
class TokenAtivacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID usuarioId;
    private String tokenHash;
    private Instant expiraEm;
    private Instant usadoEm;
    private Instant criadoEm;

    protected TokenAtivacao() {}

    TokenAtivacao(UUID usuarioId, String tokenHash, Instant agora, Duration validade) {
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.criadoEm = agora;
        this.expiraEm = agora.plus(validade);
    }

    void usar(Instant agora) {
        if (usadoEm != null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "TOKEN_JA_UTILIZADO",
                    "Este link de ativação já foi utilizado");
        }
        if (!agora.isBefore(expiraEm)) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "TOKEN_EXPIRADO", "Este link de ativação expirou");
        }
        usadoEm = agora;
    }

    UUID getUsuarioId() {
        return usuarioId;
    }
}
