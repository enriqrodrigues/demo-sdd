package br.demo.usuarios.autenticacao.internal;

import br.demo.usuarios.compartilhado.erro.CodigoErro;
import br.demo.usuarios.compartilhado.erro.EscritorErro;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cadeia de segurança da API (AD-7, AD-15): CSRF de SPA sem isenções, sem CORS, só as cinco rotas
 * públicas (por método) e respostas 401/403 no envelope de erro.
 */
@Configuration(proxyBeanMethods = false)
class ConfiguracaoSeguranca {

    @Bean
    SecurityFilterChain cadeiaSeguranca(HttpSecurity http, EscritorErro escritor) throws Exception {
        http
                .csrf(csrf -> csrf.spa())
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.POST,
                                "/api/cadastro", "/api/ativacao", "/api/auth/login", "/api/auth/logout")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/sessao").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint((requisicao, resposta, excecao) ->
                                escritor.escrever(resposta, CodigoErro.NAO_AUTENTICADO))
                        .accessDeniedHandler((requisicao, resposta, excecao) ->
                                escritor.escrever(resposta, CodigoErro.CSRF_INVALIDO)))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                // O HttpSessionRequestCache criaria uma Sessão a cada 401 anônimo.
                .requestCache(cache -> cache.disable());
        return http.build();
    }

    /**
     * Impede o Boot de criar o usuário em memória com senha gerada. Nesta fase nenhuma rota autentica;
     * o Login substitui este bean.
     */
    @Bean
    AuthenticationManager authenticationManager() {
        return autenticacao -> {
            throw new ProviderNotFoundException("Nenhum mecanismo de autenticação configurado.");
        };
    }
}
