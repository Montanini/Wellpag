package com.wellpag.aluno.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

/**
 * Cliente HTTP sincrono para agenda-service — usado por
 * GET /aluno/portal/horarios para buscar os horários do aluno autenticado.
 *
 * Mesmo padrão já validado em
 * notificacao-service/client/FinanceiroServiceClient: reenvia o header
 * Authorization recebido na requisição original do aluno sem gerar token
 * novo (aluno-service não emite tokens — quem chama já está autenticado),
 * trata erro de conectividade com ResourceAccessException -&gt;
 * IllegalStateException (-&gt; 422 via GlobalExceptionHandler) e erro de
 * resposta com RestClientResponseException -&gt; IllegalArgumentException
 * (-&gt; 400).
 */
@Slf4j
@Component
public class AgendaServiceClient {

    private final RestClient restClient;

    public AgendaServiceClient(@Value("${wellpag.agenda-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .build();
    }

    /**
     * @param authorization valor bruto do header Authorization (ex.: "Bearer eyJ...")
     *                       vindo da requisição original do aluno, reenviado sem alteração.
     */
    public List<HorarioResponse> horarios(String authorization) {
        try {
            return restClient.get()
                .uri("/portal/horarios")
                .header("Authorization", authorization)
                .retrieve()
                .body(new ParameterizedTypeReference<List<HorarioResponse>>() {});
        } catch (RestClientResponseException e) {
            log.warn("agenda-service recusou busca de horarios do portal: status={} body={}",
                e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalArgumentException("Não foi possível obter os horários");
        } catch (ResourceAccessException e) {
            log.error("agenda-service inalcancavel ao buscar horarios do portal: {}", e.getMessage());
            throw new IllegalStateException("Serviço de horários indisponível no momento, tente novamente em instantes");
        }
    }
}
