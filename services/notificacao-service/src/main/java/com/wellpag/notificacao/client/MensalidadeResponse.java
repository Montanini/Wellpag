package com.wellpag.notificacao.client;

/**
 * Subconjunto do response body devolvido por financeiro-service em
 * PATCH /professor/mensalidades/{id}/confirmar — apenas os campos que
 * FinanceiroServiceClient/NotificacaoService realmente inspecionam (nenhum,
 * hoje; mantido tipado para deserializacao explicita em vez de JsonNode, e
 * para eventual uso futuro). Vive em client/ pelo mesmo motivo de
 * ConfirmarPagamentoRequest.
 */
public record MensalidadeResponse(
    String id,
    String alunoId,
    String mesReferencia,
    Double valor,
    Integer diaVencimento,
    String status,
    String dataPagamento,
    String observacao
) {}
