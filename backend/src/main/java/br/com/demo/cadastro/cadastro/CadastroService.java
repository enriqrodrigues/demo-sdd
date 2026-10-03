package br.com.demo.cadastro.cadastro;

import br.com.demo.cadastro.usuario.NovoUsuario;
import br.com.demo.cadastro.usuario.Usuario;
import br.com.demo.cadastro.usuario.UsuarioService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CadastroService {

    private final UsuarioService usuarios;
    private final ApplicationEventPublisher eventos;

    CadastroService(UsuarioService usuarios, ApplicationEventPublisher eventos) {
        this.usuarios = usuarios;
        this.eventos = eventos;
    }

    @Transactional
    public CadastroResposta cadastrar(NovoUsuario dados) {
        Usuario usuario = usuarios.cadastrar(dados);
        eventos.publishEvent(new UsuarioCadastrado(usuario.getId(), usuario.getNome(), usuario.getEmail()));
        return new CadastroResposta(usuario.getId(), usuario.getEmail(), usuario.getStatus().name());
    }
}
