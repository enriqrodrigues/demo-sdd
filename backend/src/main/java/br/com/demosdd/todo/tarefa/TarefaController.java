package br.com.demosdd.todo.tarefa;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.com.demosdd.todo.tarefa.dto.TarefaRequest;
import br.com.demosdd.todo.tarefa.dto.TarefaResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tarefas")
public class TarefaController {

    private final TarefaService service;

    public TarefaController(TarefaService service) {
        this.service = service;
    }

    @GetMapping
    public List<TarefaResponse> listar() {
        LocalDate hoje = service.hoje();
        return service.listar().stream().map(tarefa -> TarefaResponse.de(tarefa, hoje)).toList();
    }

    @GetMapping("/{id}")
    public TarefaResponse buscar(@PathVariable UUID id) {
        return TarefaResponse.de(service.buscar(id), service.hoje());
    }

    @PostMapping
    public ResponseEntity<TarefaResponse> criar(@Valid @RequestBody TarefaRequest dados) {
        Tarefa tarefa = service.criar(dados);
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(tarefa.getId()).toUri();
        return ResponseEntity.created(local).body(TarefaResponse.de(tarefa, service.hoje()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID id) {
        service.excluir(id);
    }
}
