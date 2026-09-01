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
 * Cliente HTTP sincrono para financeiro-service — usado por
 * GET /aluno/portal/mensalidades, /aluno/portal/mensalidades/{mes} e
 * /aluno/portal/relatorio (que reusa a mesma fonte de /portal/mensalidades e
 * agrega localmente em AlunoPortalService.relatorio()).
 *
 * Mesmo padrão já validado em
 * notificacao-service/client/FinanceiroServiceClient: reenvia o header
 * Authorization recebido na requisição original do aluno sem gerar token
 * novo, trata erro de conectividade com ResourceAccessException -&gt;
 * IllegalStateException (-&gt; 422) e erro de resposta com
 * RestClientResponseException -&gt; IllegalArgumentException (-&gt; 400).
 */
@Slf4j
@Component
public class FinanceiroServiceClient {

    private final RestClient restClient;

    public FinanceiroServiceClient(@Value("${wellpag.financeiro-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .build();
    }

    public List<MensalidadeResponse> mensalidades(String authorization) {
        return buscar("/portal/mensalidades", authorization);
    }

    public List<MensalidadeResponse> mensalidadesPorMes(String mes, String authorization) {
        return buscar("/portal/mensalidades/{mes}", authorization, mes);
    }

    private List<MensalidadeResponse> buscar(String uri, String authorization, Object... uriVars) {
        try {
            return restClient.get()
                .uri(uri, uriVars)
                .header("Authorization", authorization)
                .retrieve()
                .body(new ParameterizedTypeReference<List<MensalidadeResponse>>() {});
        } catch (RestClientResponseException e) {
            log.warn("financeiro-service recusou busca de mensalidades do portal uri={}: status={} body={}",
                uri, e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalArgumentException("Não foi possível obter as mensalidades");
        } catch (ResourceAccessException e) {
            log.error("financeiro-service inalcancavel ao buscar mensalidades do portal uri={}: {}", uri, e.getMessage());
            throw new IllegalStateException("Serviço de mensalidades indisponível no momento, tente novamente em instantes");
        }
    }
}
