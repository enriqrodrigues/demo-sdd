package br.com.demosdd.activation;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ativação via POST, disparada pelo botão da página {@code /ativar}: abrir o
 * link (GET) apenas carrega a SPA e não altera a conta.
 */
@RestController
@RequestMapping("/api/activations")
class ActivationController {

    private final ActivationService activationService;

    ActivationController(ActivationService activationService) {
        this.activationService = activationService;
    }

    @PostMapping
    ActivationResponse activate(@RequestBody ActivationRequest request) {
        return new ActivationResponse(activationService.activate(request.token()).getEmail());
    }

    record ActivationRequest(String token) {
    }

    record ActivationResponse(String email) {
    }
}
