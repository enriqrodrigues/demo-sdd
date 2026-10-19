package br.com.demo.todo.tarefa;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tarefas")
public class TarefaController {

    private final ServicoTarefa servico;
    private final MapeadorTarefa mapeador;

    public TarefaController(ServicoTarefa servico, MapeadorTarefa mapeador) {
        this.servico = servico;
        this.mapeador = mapeador;
    }

    @PostMapping
    public ResponseEntity<TarefaResposta> criar(@Valid @RequestBody NovaTarefaRequisicao requisicao) {
        Tarefa tarefa = servico.criar(requisicao);
        return ResponseEntity.created(URI.create("/api/tarefas/" + tarefa.getId()))
                .body(mapeador.paraResposta(tarefa));
    }

    @GetMapping("/{id}")
    public TarefaResposta consultar(@PathVariable UUID id) {
        return mapeador.paraResposta(servico.consultar(id));
    }
}
