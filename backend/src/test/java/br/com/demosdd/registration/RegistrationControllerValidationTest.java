package br.com.demosdd.registration;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Formato de erro da validação (RF02, design D6) sem banco: o serviço é
 * simulado e nunca deve ser chamado com dados inválidos.
 */
@WebMvcTest(RegistrationController.class)
class RegistrationControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

    @Test
    void payloadInvalidoRetorna400ComTodosOsCamposInvalidos() throws Exception {
        String body = """
                {
                  "name": "   ",
                  "cpf": "529.982.247-26",
                  "email": "maria@",
                  "birthDate": "2999-01-01",
                  "password": "Segura1234",
                  "phone": "(11) 3456-789",
                  "cep": "0131010",
                  "street": "",
                  "number": "",
                  "district": "",
                  "city": "",
                  "state": "XX"
                }
                """;

        mockMvc.perform(post("/api/registrations").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder(
                        "name", "cpf", "email", "birthDate", "password", "phone", "cep",
                        "street", "number", "district", "city", "state")))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')].message").value(hasItem("Campo obrigatório")))
                .andExpect(jsonPath("$.errors[?(@.field == 'cpf')].message").value(hasItem("CPF inválido")))
                .andExpect(jsonPath("$.errors[?(@.field == 'password')].message")
                        .value(hasItem("A senha deve conter ao menos um caractere especial")));

        verify(registrationService, never()).register(any());
    }

    @Test
    void cpfInvalidoETelefoneCurtoSaoApontadosPorCampo() throws Exception {
        String body = TestPayloads.validRegistration()
                .replace("\"529.982.247-25\"", "\"529.982.247-26\"")
                .replace("\"(11) 98765-4321\"", "\"(11) 8765-432\"");

        mockMvc.perform(post("/api/registrations").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("cpf", "phone")));

        verify(registrationService, never()).register(any());
    }

    @Test
    void dataEmFormatoInvalidoEhApontadaNoCampo() throws Exception {
        String body = TestPayloads.validRegistration().replace("\"1990-05-20\"", "\"20/05/1990\"");

        mockMvc.perform(post("/api/registrations").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("birthDate"));
    }

    @Test
    void campoAusenteEhObrigatorio() throws Exception {
        mockMvc.perform(post("/api/registrations").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(12))
                .andExpect(jsonPath("$.errors[*].message").value(hasItem("Campo obrigatório")));
    }
}
