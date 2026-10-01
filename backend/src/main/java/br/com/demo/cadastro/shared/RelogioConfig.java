package br.com.demo.cadastro.shared;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class RelogioConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
