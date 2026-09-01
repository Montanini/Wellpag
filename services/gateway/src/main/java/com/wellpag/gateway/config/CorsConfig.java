package com.wellpag.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * CORS configurado no proprio gateway, ja que agora e' ele quem responde ao
 * browser primeiro (os 7 servicos + o monolito continuam com seu proprio CORS
 * tambem, mas deixam de ser acessados diretamente pelo frontend). Mesmo
 * padrao permissivo usado em todos os outros servicos (allowedOriginPatterns
 * "*", credentials true, GET/POST/PUT/DELETE/PATCH/OPTIONS).
 *
 * Usa um CorsFilter (servlet Filter) em vez de WebMvcConfigurer.addCorsMappings
 * porque as rotas do gateway sao RouterFunction (endpoints funcionais), e um
 * Filter garante que o CORS e' aplicado de forma uniforme a toda requisicao
 * antes do despacho, independente do tipo de handler.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
