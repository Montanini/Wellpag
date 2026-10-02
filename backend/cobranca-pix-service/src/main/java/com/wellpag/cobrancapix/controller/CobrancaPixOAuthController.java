package com.wellpag.cobrancapix.controller;

import com.wellpag.cobrancapix.service.CobrancaPixOAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

/**
 * Fluxo OAuth Connect com o Mercado Pago (issue #35).
 * <p>
 * {@code GET /professor/cobranca-pix/connect} e autenticado (hasRole
 * PROFESSOR, ja imposto por SecurityConfig em /professor/**) e so devolve a
 * URL de autorizacao — nao redireciona ele mesmo, ja que quem consome isso e
 * o frontend (que decide quando navegar o browser ate ela).
 * <p>
 * {@code GET /cobranca-pix/oauth/callback} e publico (permitAll, ja
 * pre-declarado em SecurityConfig) — e o navegador do professor que bate
 * aqui, redirecionado pelo Mercado Pago apos autorizar (ou recusar) a
 * conexao. Por isso ele sempre responde com um redirect 302 (sucesso ou
 * erro), nunca com um JSON de erro cru — uma excecao nao tratada aqui viraria
 * uma experiencia ruim pra um browser (ver GlobalExceptionHandler, cujo
 * fallback e pensado para clientes que consomem JSON, nao para esta rota).
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CobrancaPixOAuthController {

    private final CobrancaPixOAuthService cobrancaPixOAuthService;

    @Value("${wellpag.cobranca-pix.oauth-success-redirect-url}")
    private String successRedirectUrl;

    @Value("${wellpag.cobranca-pix.oauth-error-redirect-url}")
    private String errorRedirectUrl;

    @GetMapping("/professor/cobranca-pix/connect")
    public ResponseEntity<Map<String, String>> connect(Authentication authentication) {
        String professorId = authentication.getName();
        String authorizationUrl = cobrancaPixOAuthService.construirUrlAutorizacao(professorId);
        return ResponseEntity.ok(Map.of("authorizationUrl", authorizationUrl));
    }

    @GetMapping("/cobranca-pix/oauth/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "state", required = false) String state) {
        try {
            cobrancaPixOAuthService.conectar(code, state);
            return redirectTo(successRedirectUrl);
        } catch (Exception e) {
            log.warn("Callback OAuth do Mercado Pago rejeitado: {}", e.getMessage());
            return redirectTo(errorRedirectUrl);
        }
    }

    private ResponseEntity<Void> redirectTo(String url) {
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(url))
            .build();
    }
}
