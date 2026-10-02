package com.wellpag.cobrancapix.client;

import com.wellpag.cobrancapix.config.MercadoPagoProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Cliente HTTP fino para a API real do Mercado Pago — sem logica de negocio,
 * so monta a requisicao e mapeia a resposta (mesmo espirito de
 * {@code EvolutionApiClient} em notificacao-service: RestClient construido
 * uma vez no construtor com config via {@code @Value}, metodos que so fazem
 * HTTP + parse).
 * <p>
 * Ao contrario do padrao de {@code RestClientResponseException}/
 * {@code ResourceAccessException} -> {@code IllegalArgumentException}/
 * {@code IllegalStateException} usado por clientes inter-servico (ex.:
 * {@code FinanceiroServiceClient} em aluno-service/notificacao-service — ver
 * CLAUDE.md raiz, "Real inter-service calls"), este cliente fala com uma API
 * de terceiro real (Mercado Pago), nao com outro servico Wellpag — entao esse
 * padrao de traducao de erro nao se aplica aqui. Excecoes de HTTP/rede sao
 * logadas (para nao virarem um 500 silencioso sem pista nenhuma nos logs) e
 * propagadas — o controller/service acima decide o que fazer (aqui:
 * CobrancaPixOAuthService trata qualquer falha na troca de code por token
 * como "callback rejeitado", redirecionando para a URL de erro configurada,
 * sem persistir nada).
 */
@Slf4j
@Component
public class MercadoPagoClient {

    private final RestClient restClient;
    private final MercadoPagoProperties properties;

    public MercadoPagoClient(
            @Value("${wellpag.mercadopago.api-base-url}") String apiBaseUrl,
            MercadoPagoProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
            .baseUrl(apiBaseUrl)
            .build();
    }

    /**
     * Troca um authorization {@code code} por {@code access_token}/
     * {@code refresh_token} via {@code POST /oauth/token}. Retentativas com
     * backoff exponencial em falha de rede ou 5xx do Mercado Pago (ver
     * Javadoc da classe / spec da issue #33, secao "Implementation Decisions"
     * para MercadoPagoClient) — um 4xx (code invalido/expirado, por exemplo)
     * nao e retentado, ja que repetir a mesma requisicao invalida so bateria
     * no mesmo erro.
     */
    @Retryable(
        retryFor = {ResourceAccessException.class, HttpServerErrorException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public MercadoPagoTokenResponse trocarCodePorToken(String code) {
        Map<String, String> body = Map.of(
            "grant_type", "authorization_code",
            "client_id", properties.getClientId(),
            "client_secret", properties.getClientSecret(),
            "code", code,
            "redirect_uri", properties.getRedirectUri()
        );

        try {
            return restClient.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(MercadoPagoTokenResponse.class);
        } catch (Exception e) {
            log.warn("Falha ao trocar authorization code por token no Mercado Pago: {}", e.getMessage());
            throw e;
        }
    }
}
