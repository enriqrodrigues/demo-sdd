package br.com.demosdd.todo.produtividade;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.demosdd.todo.produtividade.dto.ScoreSemanalResponse;

@RestController
@RequestMapping("/api/produtividade")
public class ProdutividadeController {

    private final ProdutividadeService service;

    public ProdutividadeController(ProdutividadeService service) {
        this.service = service;
    }

    @GetMapping("/score-semanal")
    public ScoreSemanalResponse scoreSemanal() {
        return service.scoreSemanal();
    }
}
