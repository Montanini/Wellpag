package com.wellpag.notificacao.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;

/**
 * Cliente HTTP sincrono para financeiro-service — primeira integracao REST
 * sincrona real entre dois microsservicos nesta migracao.
 *
 * Ate agora (waves anteriores), quando um servico extraido precisava de dados
 * de outro dominio ainda nao extraido, a solucao foi uma "ponte" read-only
 * direta no MongoDB (ver model/Aluno.java, model/Mensalidade.java, etc. neste
 * mesmo servico). Isso funcionava porque o dono real daqueles dados ainda era
 * o monolito. Mas Mensalidade ja tem um dono real e extraido: financeiro-service
 * (porta 8095, ja mergeado em main). Escrever direto na collection
 * "mensalidades" a partir daqui seria contornar esse dono e duplicar a regra
 * de negocio de confirmacao de pagamento (e a validacao de posse
 * professor->mensalidade) que financeiro-service ja implementa. Por isso,
 * NotificacaoService.vincular() chama o endpoint real
 * PATCH /professor/mensalidades/{id}/confirmar em vez de escrever no banco.
 *
 * Autenticacao: o endpoint do financeiro-service exige JWT valido com role
 * PROFESSOR. Como quem chama vincular() ja e o professor autenticado desta
 * requisicao, o header Authorization recebido no controller e simplesmente
 * repassado (forward) para esta chamada — nenhum token novo e gerado aqui.
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

    /**
     * Confirma o pagamento de uma mensalidade no financeiro-service.
     *
     * @param authorization valor bruto do header Authorization (ex.: "Bearer eyJ...")
     *                       vindo da requisicao original do professor, reenviado sem alteracao.
     * @throws IllegalArgumentException se financeiro-service responder erro de cliente
     *         (ex.: 400 — mensalidade nao encontrada para esse professor; a validacao de
     *         posse e feita la, nao aqui).
     * @throws IllegalStateException se financeiro-service estiver inalcancavel (fora do ar,
     *         timeout, DNS) — sem isso, a excecao de conectividade propagava sem tratamento
     *         e o Spring Security devolvia um 403 vazio e enganoso no dispatch de erro,
     *         escondendo que o problema real era o financeiro-service estar indisponivel.
     */
    public MensalidadeResponse confirmarPagamento(String mensalidadeId, String authorization,
                                                   LocalDate dataPagamento, String observacao) {
        try {
            return restClient.patch()
                .uri("/professor/mensalidades/{mensalidadeId}/confirmar", mensalidadeId)
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ConfirmarPagamentoRequest(dataPagamento, observacao))
                .retrieve()
                .body(MensalidadeResponse.class);
        } catch (RestClientResponseException e) {
            log.warn("financeiro-service recusou confirmacao de pagamento mensalidadeId={}: status={} body={}",
                mensalidadeId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalArgumentException("Mensalidade não encontrada");
        } catch (ResourceAccessException e) {
            log.error("financeiro-service inalcancavel ao confirmar pagamento mensalidadeId={}: {}",
                mensalidadeId, e.getMessage());
            throw new IllegalStateException("Serviço de mensalidades indisponível no momento, tente novamente em instantes");
        }
    }
}
