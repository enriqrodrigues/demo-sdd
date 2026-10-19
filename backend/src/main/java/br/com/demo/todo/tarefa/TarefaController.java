package br.com.demo.todo.tarefa;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.groups.Default;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.demo.todo.comum.DataCorrente;
import br.com.demo.todo.prazo.SituacaoPrazo;

@RestController
@RequestMapping("/api/tarefas")
public class TarefaController {

    private final ServicoTarefa servico;
    private final MapeadorTarefa mapeador;
    private final DataCorrente dataCorrente;

    public TarefaController(ServicoTarefa servico, MapeadorTarefa mapeador, DataCorrente dataCorrente) {
        this.servico = servico;
        this.mapeador = mapeador;
        this.dataCorrente = dataCorrente;
    }

    @PostMapping
    public ResponseEntity<TarefaResposta> criar(@Valid @RequestBody TarefaRequisicao requisicao) {
        Tarefa tarefa = servico.criar(requisicao);
        return ResponseEntity.created(URI.create("/api/tarefas/" + tarefa.getId()))
                .body(mapeador.paraResposta(tarefa));
    }

    @GetMapping
    public List<TarefaResposta> listar(
            @RequestParam(required = false) StatusTarefa status,
            @RequestParam(required = false) SituacaoPrazo situacaoPrazo) {
        LocalDate hoje = dataCorrente.hoje();
        return servico.listar(status, situacaoPrazo, hoje).stream()
                .map(tarefa -> mapeador.paraResposta(tarefa, hoje))
                .toList();
    }

    @GetMapping("/{id}")
    public TarefaResposta consultar(@PathVariable UUID id) {
        return mapeador.paraResposta(servico.consultar(id));
    }

    @PutMapping("/{id}")
    public TarefaResposta editar(@PathVariable UUID id,
            @Validated({Default.class, TarefaRequisicao.Edicao.class}) @RequestBody TarefaRequisicao requisicao) {
        return mapeador.paraResposta(servico.editar(id, requisicao));
    }
}
