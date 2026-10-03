package br.com.demosdd.activation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ActivationProperties.class)
class ActivationConfig {
}
