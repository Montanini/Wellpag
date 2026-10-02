package com.wellpag.cobrancapix.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Corpo da resposta de {@code POST /oauth/token} do Mercado Pago (troca de
 * authorization code por tokens). So os campos que {@code CobrancaPixOAuthService}
 * realmente usa para montar {@code ContaMercadoPago} sao mapeados —
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)} porque a resposta real
 * do Mercado Pago tem mais campos (ex.: {@code scope}, {@code live_mode}) que
 * nao interessam aqui.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MercadoPagoTokenResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") Long expiresIn,
    @JsonProperty("user_id") String userId,
    @JsonProperty("public_key") String publicKey
) {
}
