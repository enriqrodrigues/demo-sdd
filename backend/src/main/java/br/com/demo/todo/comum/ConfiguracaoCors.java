package br.com.demo.todo.comum;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Libera a API apenas para as origens configuradas (por padrão, o servidor do Vite). */
@Configuration(proxyBeanMethods = false)
public class ConfiguracaoCors implements WebMvcConfigurer {

    private final String[] origensPermitidas;

    public ConfiguracaoCors(
            @Value("${app.cors.origens-permitidas:http://localhost:5173}") String[] origensPermitidas) {
        this.origensPermitidas = origensPermitidas;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origensPermitidas)
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
