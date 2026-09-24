package com.wellpag.cobrancapix.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Segue o mesmo padrao enxuto ja estabelecido pelos outros servicos: nao ha
 * OAuth2 login nem DaoAuthenticationProvider aqui — login/emissao de token e
 * responsabilidade exclusiva do auth-service; este servico so valida o JWT
 * recebido (ver JwtAuthFilter/JwtService).
 *
 * CORS nao e' configurado aqui: o browser so fala com o gateway (backend/gateway/
 * .../config/CorsConfig.java), nunca diretamente com este servico. Duplicar CORS
 * aqui fazia o gateway repassar dois conjuntos de headers Access-Control-Allow-*
 * (o deste servico + o do proprio gateway), o que o browser rejeita (bug real,
 * corrigido no PR #28) — regra explicita do projeto: CORS vive so no gateway.
 *
 * Duas rotas publicas (permitAll, sem JWT) sao pre-declaradas mesmo sem
 * controller ainda (issue #33/#35 as implementam): serao chamadas por
 * terceiros sem Authorization header —
 *   - GET /cobranca-pix/oauth/callback: navegador do professor, redirecionado
 *     pelo Mercado Pago apos autorizar a conexao OAuth.
 *   - POST /webhooks/mercadopago: servidores do Mercado Pago notificando
 *     eventos de pagamento.
 * A seguranca dessas duas rotas vem de validacao de state assinado / HMAC
 * implementada dentro dos futuros controllers, nao do Spring Security.
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
                // Pre-declaradas para as issues #35+ — ver Javadoc da classe.
                .requestMatchers(HttpMethod.GET, "/cobranca-pix/oauth/callback").permitAll()
                .requestMatchers(HttpMethod.POST, "/webhooks/mercadopago").permitAll()
                .requestMatchers("/professor/**").hasRole("PROFESSOR")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
