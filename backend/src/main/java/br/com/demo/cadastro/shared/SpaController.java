package br.com.demo.cadastro.shared;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class SpaController {

    @GetMapping({"/", "/{rota:(?!api$)[^.]*}", "/{rota:(?!api$)[^.]*}/{subrota:[^.]*}"})
    String encaminhar() {
        return "forward:/index.html";
    }
}
