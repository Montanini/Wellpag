package com.wellpag.pagamento.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Versao enxuta do SecurityConfig do monolito: reproduz apenas as regras que hoje
 * protegem /professor/banco/** (role PROFESSOR), mais o necessario para
 * Swagger/actuator. Nao ha OAuth2 login nem DaoAuthenticationProvider aqui —
 * login/emissao de token e responsabilidade exclusiva do auth-service; este
 * servico so valida o JWT recebido (ver JwtAuthFilter/JwtService).
 *
 * CORS nao e' configurado aqui: o browser so fala com o gateway (backend/gateway/
 * .../config/CorsConfig.java), nunca diretamente com este servico. Duplicar CORS
 * aqui fazia o gateway repassar dois conjuntos de headers Access-Control-Allow-*
 * (o deste servico + o do proprio gateway), o que o browser rejeita.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/swagger-ui/**",
                    "/api-docs/**",
                    "/actuator/health"
                ).permitAll()
                .requestMatchers("/professor/**").hasRole("PROFESSOR")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
