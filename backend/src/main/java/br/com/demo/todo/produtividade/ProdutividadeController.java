package br.com.demo.todo.produtividade;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/produtividade")
public class ProdutividadeController {

    private final ServicoProdutividade servico;

    public ProdutividadeController(ServicoProdutividade servico) {
        this.servico = servico;
    }

    @GetMapping("/semana")
    public ScoreSemanal semana() {
        return servico.semanaCorrente();
    }
}
