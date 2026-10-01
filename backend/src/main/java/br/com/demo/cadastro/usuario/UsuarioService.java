package br.com.demo.cadastro.usuario;

import br.com.demo.cadastro.shared.NegocioException;
import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository repositorio;
    private final PasswordEncoder encoder;
    private final Clock clock;

    UsuarioService(UsuarioRepository repositorio, PasswordEncoder encoder, Clock clock) {
        this.repositorio = repositorio;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Transactional
    public Usuario cadastrar(NovoUsuario dados) {
        String email = normalizarEmail(dados.email());
        if (repositorio.existsByEmail(email)) {
            throw emailDuplicado();
        }
        if (repositorio.existsByCpf(dados.cpf())) {
            throw cpfDuplicado();
        }
        Usuario usuario = new Usuario(dados.nome().trim(), dados.cpf(), email, dados.dataNascimento(),
                encoder.encode(dados.senha()), dados.telefone(), dados.endereco().paraEndereco(), clock.instant());
        try {
            return repositorio.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            throw traduzirViolacao(e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(UUID id) {
        return repositorio.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorEmail(String email) {
        return email == null ? Optional.empty() : repositorio.findByEmail(normalizarEmail(email));
    }

    public boolean senhaConfere(Usuario usuario, String senha) {
        return senha != null && !Senha.excedeLimite(senha) && encoder.matches(senha, usuario.getSenhaHash());
    }

    @Transactional
    public void ativar(UUID id) {
        buscarExistente(id).ativar(clock.instant());
    }

    @Transactional
    public Usuario atualizarContato(UUID id, String telefone, EnderecoDados endereco) {
        Usuario usuario = buscarExistente(id);
        usuario.atualizarContato(telefone, endereco.paraEndereco(), clock.instant());
        return usuario;
    }

    static NegocioException traduzirViolacao(DataIntegrityViolationException e) {
        String mensagem = String.valueOf(e.getMostSpecificCause().getMessage());
        return mensagem.contains("uk_usuario_email") ? emailDuplicado() : cpfDuplicado();
    }

    private Usuario buscarExistente(UUID id) {
        return repositorio.findById(id).orElseThrow(() ->
                new NegocioException(HttpStatus.NOT_FOUND, "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado"));
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static NegocioException emailDuplicado() {
        return new NegocioException(HttpStatus.CONFLICT, "EMAIL_JA_CADASTRADO", "E-mail já cadastrado", "email");
    }

    private static NegocioException cpfDuplicado() {
        return new NegocioException(HttpStatus.CONFLICT, "CPF_JA_CADASTRADO", "CPF já cadastrado", "cpf");
    }
}
