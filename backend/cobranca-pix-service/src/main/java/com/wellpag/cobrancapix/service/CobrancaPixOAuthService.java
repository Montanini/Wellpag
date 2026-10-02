package com.wellpag.cobrancapix.service;

import com.wellpag.cobrancapix.client.MercadoPagoClient;
import com.wellpag.cobrancapix.client.MercadoPagoTokenResponse;
import com.wellpag.cobrancapix.config.MercadoPagoProperties;
import com.wellpag.cobrancapix.model.ContaMercadoPago;
import com.wellpag.cobrancapix.model.StatusContaMercadoPago;
import com.wellpag.cobrancapix.oauth.OAuthStateService;
import com.wellpag.cobrancapix.repository.ContaMercadoPagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;

/**
 * Orquestra o fluxo OAuth Connect do Mercado Pago (issue #35):
 * <p>
 * 1. {@link #construirUrlAutorizacao(String)} — monta a URL de autorizacao do
 *    Mercado Pago Connect (chamada pelo controller em
 *    GET /professor/cobranca-pix/connect, ja autenticado como PROFESSOR).
 * 2. {@link #conectar(String, String)} — processa o callback publico
 *    (GET /cobranca-pix/oauth/callback): valida o {@code state}, troca o
 *    {@code code} por tokens no Mercado Pago, e grava/atualiza
 *    {@code ContaMercadoPago} como {@code CONNECTED}. Qualquer rejeicao
 *    (state invalido/expirado, code ausente, falha na troca com o Mercado
 *    Pago) lanca {@link IllegalArgumentException} sem persistir nada — o
 *    controller e quem decide redirecionar para a URL de erro configurada.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CobrancaPixOAuthService {

    private static final String AUTHORIZATION_BASE_URL = "https://auth.mercadopago.com/authorization";

    private final OAuthStateService oAuthStateService;
    private final MercadoPagoClient mercadoPagoClient;
    private final MercadoPagoProperties mercadoPagoProperties;
    private final ContaMercadoPagoRepository contaMercadoPagoRepository;

    public String construirUrlAutorizacao(String professorId) {
        String state = oAuthStateService.gerar(professorId);

        return UriComponentsBuilder.fromUriString(AUTHORIZATION_BASE_URL)
            .queryParam("client_id", mercadoPagoProperties.getClientId())
            .queryParam("response_type", "code")
            .queryParam("platform_id", "mp")
            .queryParam("redirect_uri", mercadoPagoProperties.getRedirectUri())
            .queryParam("state", state)
            .build()
            .toUriString();
    }

    public void conectar(String code, String state) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("code ausente no callback OAuth");
        }

        String professorId = oAuthStateService.validarEExtrairProfessorId(state);

        MercadoPagoTokenResponse token = mercadoPagoClient.trocarCodePorToken(code);
        if (token == null || !StringUtils.hasText(token.accessToken())) {
            throw new IllegalArgumentException("resposta invalida do Mercado Pago na troca de code por token");
        }

        Instant agora = Instant.now();
        ContaMercadoPago conta = contaMercadoPagoRepository.findByProfessorId(professorId)
            .orElseGet(() -> ContaMercadoPago.builder()
                .professorId(professorId)
                .createdAt(agora)
                .build());

        conta.setMpUserId(token.userId());
        conta.setAccessToken(token.accessToken());
        conta.setRefreshToken(token.refreshToken());
        conta.setTokenExpiresAt(token.expiresIn() != null ? agora.plusSeconds(token.expiresIn()) : null);
        conta.setPublicKey(token.publicKey());
        conta.setStatus(StatusContaMercadoPago.CONNECTED);
        conta.setUpdatedAt(agora);

        contaMercadoPagoRepository.save(conta);
        log.info("Conta Mercado Pago conectada/atualizada para professorId={}", professorId);
    }
}
