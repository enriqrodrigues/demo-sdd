package br.com.demo.cadastro.cadastro;

import br.com.demo.cadastro.usuario.NovoUsuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CadastroController {

    private final CadastroService servico;

    CadastroController(CadastroService servico) {
        this.servico = servico;
    }

    @PostMapping("/api/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    CadastroResposta cadastrar(@Valid @RequestBody NovoUsuario dados) {
        return servico.cadastrar(dados);
    }
}
